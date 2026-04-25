# 개발 계획 — 알림 발송 시스템

## 기술 스택

| 분류 | 기술 |
|---|---|
| Framework | Spring Boot 4.0.5 |
| Language | Java 21 |
| Build | Gradle Groovy DSL |
| DB | MySQL 8.0 (Docker) |
| ORM | Spring Data JPA + Hibernate |
| 비동기 | Spring @Async + TransactionSynchronizationAdapter |
| 스케줄러 | Spring @Scheduled |
| 중복 방지 | DB UNIQUE 제약 (idempotency_key) + CAS UPDATE |
| 재시도 | Exponential Backoff 직접 구현 |
| 테스트 | JUnit 5 |

---

## Phase 1 — 도메인 모델 & DB 설계

> 핵심 엔티티와 상태를 정의하고 DB 스키마를 확정한다

- `idempotency_key` UNIQUE 제약 → 중복 요청 DB 레벨 차단
- `processingStartedAt` → 복구 워커에서 타임아웃 감지에 사용
- `scheduledAt` null = 즉시 발송 대상, 값 있음 = 해당 시각 이후 폴링 포함

---

## Phase 2 — 알림 저장 API

> `POST /api/notifications` 요청을 받아 DB에 저장하고 202를 반환한다

- 저장만 하고 발송은 하지 않음 (발송은 Phase 3에서)
- 저장 전 `findByIdempotencyKey`로 중복 체크하여 이미 존재하면 skip (멱등성)
- `idempotency_key` = `SHA256(eventId:recipientId:notificationType:channel)`
- `eventId` — 비즈니스 이벤트 식별자 (멱등성 키 생성용), `referenceData` — 부가 참조 데이터 JSON (템플릿 변수 치환용)

---

## Phase 3 — 비동기 발송 처리

> 트랜잭션 커밋 직후 별도 스레드에서 발송을 처리한다

진입점 두 가지가 동일한 AFTER_COMMIT 구조로 합류한다.

```
[진입점 A — 수동 API]
POST /api/notifications
    └─ @Transactional: DB 저장 (PENDING) + publishEvent()
    └─ AFTER_COMMIT → messagePort.publish() → @Async → Processor.processOne()
    └─ API 스레드: 202 반환 후 종료

[진입점 B — 도메인 이벤트]
비즈니스 트랜잭션
    └─ BEFORE_COMMIT: 알림 DB 저장 (PENDING) — 비즈니스와 같은 커넥션, 원자성 보장
    └─ AFTER_COMMIT → messagePort.publish() → @Async → Processor.processOne()
```

- AFTER_COMMIT 이후 실행 → 커밋 전 레코드를 읽는 문제 없음
- `@Async`는 SpringNotificationMessageAdapter 안에만 위치, 리스너에는 없음
- `NotificationMessageOutputPort` 인터페이스로 추상화 → Kafka 전환 시 구현체만 교체

```
현재: SpringNotificationMessageAdapter → @Async → Processor.processOne()
Kafka: KafkaNotificationMessageAdapter → kafkaTemplate.send() → @KafkaListener → Processor.processOne()
```

---

## Phase 4 — 재시도 & 실패 처리

> 발송 실패 시 Exponential Backoff으로 재시도하고 최종 실패를 기록한다

- 예외를 무시하지 않고 상태로 관리 → 운영 추적 가능
- `lastErrorMessage`에 예외 메시지 기록 → 실패 원인 확인 가능
- `FAILED` 상태는 폴링 쿼리 WHERE 조건에서 제외 → 자동 재시도 없음

| retryCount | 대기 시간 |
|---|---|
| 1 | 2분 (2^1) |
| 2 | 4분 (2^2) |
| 3 | 8분 (2^3) |
| 4 | FAILED 전이 |

---

## Phase 5 — 폴링 워커 & 복구 워커

> 안전망 역할의 폴링 워커와 PROCESSING 타임아웃 복구 워커를 구현한다

- `fixedDelay` 사용 — 이전 실행 완료 후 N초 대기 (`fixedRate`는 중첩 실행 위험)
- 폴링 워커와 @Async가 동시에 같은 레코드를 집어도 CAS UPDATE로 중복 처리 방지
- 서버 재시작 후 DB에 남은 PENDING 레코드를 폴링 워커가 자동 재처리

---

## Phase 6 — 조회 & 수동 재시도 API

> 알림 상태 조회, 목록 조회, 읽음 처리, 수동 재시도 API를 구현한다

- 읽음 처리는 `UPDATE WHERE is_read = false` 조건 하나로 멱등성 해결
- 여러 기기에서 동시 요청이 와도 한 번만 처리됨
- 수동 재시도: FAILED 상태를 PENDING으로 전환하고 retryCount를 0으로 초기화
  - 운영자가 원인을 확인하고 재시도하는 맥락이므로 초기화가 맞음
  - 초기화하지 않으면 재시도 즉시 다시 FAILED가 됨

---

## Phase 7 — 알림 템플릿

> 알림 타입 + 채널 조합별 메시지 템플릿을 관리하고 발송 시 변수 치환한다

- `NotificationTemplate` 엔티티: `(notificationType, channel)` UNIQUE 제약
- `titleTemplate`, `bodyTemplate`에 `{변수명}` 플레이스홀더 사용
- `ReferenceDataParser`로 `referenceData` JSON → `Map<String, String>` 변환
- `EmailNotificationSender`, `InAppNotificationSender`에서 템플릿 조회 → 변수 치환 → 로그 출력
- 템플릿이 없으면 경고 로그만 남기고 발송 건너뜀
