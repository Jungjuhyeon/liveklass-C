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

- [x] `NotificationStatus` enum 작성 (`PENDING`, `PROCESSING`, `COMPLETED`, `RETRYING`, `FAILED`)
- [x] `NotificationChannel` enum 작성 (`EMAIL`, `IN_APP`)
- [x] `BaseEntity` 작성 (`common/domain`, `@MappedSuperclass`, JPA Auditing)
- [x] `Notification` 엔티티 작성
  - [x] `id`, `recipientId`, `notificationType`, `channel`
  - [x] `referenceData` (JSON 문자열)
  - [x] `status`, `idempotencyKey` (UNIQUE)
  - [x] `retryCount`, `maxRetryCount` (기본값 3)
  - [x] `lastErrorMessage`
  - [x] `isRead` (기본값 false)
  - [x] `scheduledAt`, `processingStartedAt`, `processedAt`, `nextRetryAt`
  - [x] `createdAt`, `updatedAt` (BaseEntity)
- [x] `@EnableJpaAuditing` 메인 클래스에 추가
- [x] `application.yml` `ddl-auto: update` 로 테이블 자동 생성 확인

**기술 포인트**
- `idempotency_key` UNIQUE 제약 → 중복 요청 DB 레벨 차단
- `processingStartedAt` → 복구 워커에서 타임아웃 감지에 사용
- `scheduledAt` null = 즉시 발송 대상, 값 있음 = 해당 시각 이후 폴링 포함

---

## Phase 2 — 알림 저장 API

> `POST /notifications` 요청을 받아 DB에 저장하고 202를 반환한다

- [ ] `NotificationController` 작성
  - [ ] `POST /notifications` 엔드포인트
  - [ ] 요청 DTO (`recipientId`, `notificationType`, `referenceData`, `channel`, `scheduledAt`)
  - [ ] 응답 DTO (`notificationId`, `status`)
- [ ] `NotificationService.register()` 작성
  - [ ] `idempotency_key` 생성 — `SHA256(eventId:recipientId:type:channel)`
  - [ ] `DataIntegrityViolationException` catch → 기존 레코드 반환 (멱등성)
- [ ] 저장 후 `202 Accepted` 반환 확인

**기술 포인트**
- 저장만 하고 발송은 하지 않음 (발송은 Phase 3에서)
- 중복 키 충돌 시 예외를 던지지 않고 기존 레코드를 그대로 반환
---

## Phase 3 — 비동기 발송 처리

> 트랜잭션 커밋 직후 별도 스레드에서 발송을 처리한다

- [ ] `NotificationMessagePort` 인터페이스 정의
- [ ] `SpringNotificationMessageAdapter` 작성
  - [ ] `@Async`로 `NotificationProcessor.processOne()` 호출
- [ ] `NotificationProcessor.processOne(Long id)` 작성
  - [ ] CAS UPDATE — `status IN ('PENDING','RETRYING')` → `PROCESSING` 전환
  - [ ] affected_rows = 0 이면 skip (다른 경로가 이미 처리 중)
- [ ] `MockEmailSender` 작성 — 로그 출력으로 대체
- [ ] `MockInAppSender` 작성 — 로그 출력으로 대체
- [ ] `TransactionSynchronizationAdapter` 등록
  - [ ] AFTER_COMMIT 시점에 `NotificationMessagePort.send()` 호출
- [ ] `@EnableAsync` + `AsyncConfig` 스레드풀 설정

**기술 포인트**
- AFTER_COMMIT 이후 실행 → 커밋 전 레코드를 읽는 문제 없음
- `@Async`는 별도 스레드풀 → API 요청 스레드 블로킹 없음
- `NotificationMessagePort` 인터페이스로 추상화 → Kafka 전환 시 구현체만 교체

```
현재: SpringNotificationMessageAdapter → @Async → Processor
Kafka: KafkaNotificationMessageAdapter → kafkaTemplate.send() → KafkaConsumer → Processor
```

---

## Phase 4 — 재시도 & 실패 처리

> 발송 실패 시 Exponential Backoff으로 재시도하고 최종 실패를 기록한다

- [ ] 발송 성공 시 → `COMPLETED` + `processedAt` 기록
- [ ] 발송 실패 시 분기 처리
  - [ ] `retryCount < maxRetryCount` → `RETRYING` + `nextRetryAt` 계산
  - [ ] `retryCount >= maxRetryCount` → `FAILED` + `lastErrorMessage` 기록
- [ ] Exponential Backoff 계산 구현
  - [ ] `nextRetryAt = now + 5^retryCount 분`

| retryCount | 대기 시간 |
|---|---|
| 1 | 5분 |
| 2 | 25분 |
| 3 | FAILED 전이 |

**기술 포인트**
- 예외를 무시하지 않고 상태로 관리 → 운영 추적 가능
- `lastErrorMessage`에 예외 메시지 기록 → 실패 원인 확인 가능
- `FAILED` 상태는 폴링 쿼리 WHERE 조건에서 제외 → 자동 재시도 없음

---

## Phase 5 — 폴링 워커 & 복구 워커

> 안전망 역할의 폴링 워커와 PROCESSING 타임아웃 복구 워커를 구현한다

- [ ] `NotificationPollingWorker` 작성
  - [ ] `@Scheduled(fixedDelay = 10_000)` — 10초마다 실행
  - [ ] `PENDING` / `RETRYING` 레코드 조회 (LIMIT 100)
  - [ ] `nextRetryAt <= now` 조건 포함
  - [ ] `scheduledAt IS NULL OR scheduledAt <= now` 조건 포함
  - [ ] 각 레코드에 대해 `NotificationProcessor.processOne()` 호출
- [ ] `NotificationRecoveryWorker` 작성
  - [ ] `@Scheduled(fixedDelay = 300_000)` — 5분마다 실행
  - [ ] `PROCESSING` 상태에서 `processingStartedAt < now - 10분` 인 레코드 조회
  - [ ] `PENDING` 으로 복구 (`retryCount` 변경 없음)

**기술 포인트**
- `fixedDelay` 사용 — 이전 실행 완료 후 N초 대기 (`fixedRate`는 중첩 실행 위험)
- 폴링 워커와 @Async가 동시에 같은 레코드를 집어도 CAS UPDATE로 중복 처리 방지
- 서버 재시작 후 DB에 남은 PENDING 레코드를 폴링 워커가 자동 재처리

---

## Phase 6 — 조회 API

> 알림 상태 조회, 목록 조회, 읽음 처리 API를 구현한다

- [ ] `GET /notifications/{id}` — 단건 상태 조회
- [ ] `GET /notifications?recipientId=&isRead=` — 수신자 기준 목록 조회
  - [ ] `isRead` 파라미터로 읽음/안읽음 필터
- [ ] `PATCH /notifications/{id}/read` — 읽음 처리
  - [ ] `UPDATE WHERE is_read = false` 조건으로 멱등성 보장
  - [ ] affected_rows = 0 이면 이미 읽음 처리된 것으로 정상 응답

**기술 포인트**
- 읽음 처리는 `UPDATE WHERE is_read = false` 조건 하나로 멱등성 해결
- 여러 기기에서 동시 요청이 와도 한 번만 처리됨
