# PRD — 알림 발송 시스템

## 1. 배경 및 목적

수강 신청 완료, 결제 확정, 강의 시작 D-1, 취소 처리 등 다양한 비즈니스 이벤트가 발생할 때 사용자에게 이메일 또는 인앱 알림을 발송해야 한다.  
알림 발송은 비즈니스 트랜잭션과 분리되어 처리되어야 하며, 장애 상황에서도 알림이 유실되지 않아야 한다.

---

## 2. 목표

- 알림 처리 실패가 수강신청, 결제 등 핵심 비즈니스 트랜잭션에 영향을 주지 않아야 한다.
- 네트워크 장애, 외부 서버 오류 같은 일시적 장애에도 재시도를 통해 결국 발송이 완료되어야 한다.
- 동일한 이벤트에 대해 알림이 중복 발송되어서는 안 된다.
- 실제 메시지 브로커 없이 구현하되 Kafka 등 운영 환경으로 언제든 전환 가능한 구조를 갖추어야 한다.

---

## 3. 사용자 및 이해관계자

| 역할 | 설명 |
|---|---|
| 수강생 | 알림을 수신하는 최종 사용자 |
| 운영자 | 알림 발송 실패 건을 모니터링하고 수동으로 재시도하는 관리자 |
| 내부 서비스 | 수강신청, 결제 서비스 — 알림 발송을 트리거하는 도메인 서비스 |

---

## 4. API 명세

### 4.1 알림 발송 요청 등록

```
POST /api/notifications
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| eventId | String | Y | 비즈니스 이벤트 식별자 — 멱등성 키 생성에 사용 |
| recipientId | Long | Y | 수신자 ID |
| notificationType | String | Y | 알림 타입 (ENROLLMENT_COMPLETE, PAYMENT_CONFIRMED 등) |
| referenceData | String | N | 부가 참조 데이터 JSON 문자열 (템플릿 변수 치환에 사용) |
| channel | String | Y | 발송 채널 (EMAIL / IN_APP) |
| scheduledAt | DateTime | N | 예약 발송 시각 |

**Response** `202 Accepted`

```json
{
  "notificationId": 1,
  "status": "PENDING"
}
```

> 요청 즉시 발송하지 않는다. DB에 PENDING 상태로 저장 후 202를 반환하며, 발송은 API 요청 스레드와 분리된 별도 스레드에서 비동기로 처리된다.

---

### 4.2 알림 상태 조회

```
GET /api/notifications/{id}
```

**Response** `200 OK`

```json
{
  "notificationId": 1,
  "status": "COMPLETED",
  "channel": "EMAIL",
  "retryCount": 0,
  "lastErrorMessage": null,
  "createdAt": "2026-04-24T10:00:00",
  "processedAt": "2026-04-24T10:00:05"
}
```

---

### 4.3 사용자 알림 목록 조회

```
GET /api/notifications?recipientId={recipientId}&isRead={true|false}
```

**Query Params**

| 파라미터 | 필수 | 설명 |
|---|---|---|
| recipientId | Y | 수신자 ID |
| isRead | N | 읽음 여부 필터 (true / false) |

---

### 4.4 읽음 처리

```
PATCH /api/notifications/{id}/read
```

**Response** `200 OK`

> `UPDATE WHERE is_read = false` 조건으로 처리하여 멱등성 보장. 여러 기기에서 동시 요청해도 한 번만 처리된다.

---

### 4.5 수동 재시도

```
PATCH /api/notifications/{id}/retry
```

**Response** `200 OK`

```json
{
  "notificationId": 1,
  "status": "PENDING"
}
```

> FAILED 상태인 알림을 PENDING으로 전환하고 retryCount를 0으로 초기화한다. 운영자가 원인을 확인한 뒤 수동으로 재시도하는 용도이며, 초기화하지 않으면 재시도 즉시 다시 FAILED가 된다.

---

## 5. 알림 상태 정의

| 상태 | 설명 |
|---|---|
| PENDING | 최초 DB 저장 시 설정되는 발송 대기 상태 |
| PROCESSING | CAS UPDATE 성공 후 발송 처리 중인 상태 |
| COMPLETED | 발송 성공 |
| RETRYING | 발송 실패 후 재시도 대기 중 (retryCount < maxRetryCount) |
| FAILED | 최대 재시도 횟수 초과, 자동 재시도 없음. 수동 재시도 API로 PENDING 전환 및 retryCount 초기화 가능 |

### 상태 전이 다이어그램

```
PENDING ──(CAS UPDATE 성공)──► PROCESSING ──(발송 성공)──► COMPLETED
                                    │
                        ┌───────────┴───────────┐
                (retryCount < max)       (retryCount >= max)
                        │                       │
                     RETRYING               FAILED
                        │
              (next_retry_at 도래)
                        │
                   PROCESSING

PROCESSING ──(10분 초과)──► PENDING  ※ retryCount 유지

FAILED ──(수동 재시도 API)──► PENDING  ※ retryCount = 0 초기화
```

---

## 6. 재시도 정책

Exponential Backoff 방식을 사용하며 최대 3회 재시도한다.

| 시도 | retryCount | 다음 재시도까지 대기 |
|---|---|---|
| 1차 실패 | 1 | 2분 후 (2^1) |
| 2차 실패 | 2 | 4분 후 (2^2) |
| 3차 실패 | 3 | 8분 후 (2^3) |
| 4차 실패 | 4 | FAILED 전이 |

- 지연 시간 계산: `2^retryCount 분`
- 실패 사유는 `lastErrorMessage` 컬럼에 예외 메시지로 기록한다.

---

## 7. 중복 발송 방지

두 레이어로 구성한다.

### 7.1 요청 레벨 (idempotency_key)

- `idempotency_key = SHA256(eventId:recipientId:notificationType:channel)` 로 생성
- DB UNIQUE 제약을 걸어 동시 중복 요청을 DB 레벨에서 차단
- `INSERT ... ON DUPLICATE KEY UPDATE` (UPSERT) 전략으로 중복 요청 시 예외 없이 무시
- DB UNIQUE 제약이 원자적으로 중복을 방지하여 race condition에도 안전

### 7.2 처리 레벨 (CAS UPDATE)

- `UPDATE WHERE status IN (PENDING, RETRYING)` 조건으로 PROCESSING 전환
- affected_rows가 0이면 이미 다른 인스턴스가 처리 중이므로 skip
- 이벤트 경로(@Async)와 폴링 경로가 동시에 같은 레코드를 집으려 할 때 중복 방지

---

## 8. 시스템 구조

### 8.1 처리 흐름

`notifications` 테이블이 Outbox + 메시지 큐 역할을 동시에 담당한다.

```
[진입점 A — 수동 API]
POST /api/notifications
    └─ @Transactional: DB 저장 (PENDING) + publishEvent()  ─► 202 반환
    └─ AFTER_COMMIT → messagePort.publish() → @Async → Processor.processOne()

[진입점 B — 도메인 이벤트]
비즈니스 트랜잭션
    └─ BEFORE_COMMIT: 알림 DB 저장 (PENDING) — 비즈니스와 같은 커넥션, 원자성 보장
    └─ AFTER_COMMIT → messagePort.publish() → @Async → Processor.processOne()

[폴링 워커] (10초마다, 안전망 역할)
    └─► PENDING/RETRYING 레코드 조회 ─► NotificationProcessor.processOne()

[복구 워커] (5분마다)
    └─► PROCESSING 10분 초과 레코드 ─► PENDING 복구
```

> 정상 경로에서는 AFTER_COMMIT @Async가 즉시 처리하므로 발송 지연이 없다.  
> 폴링 워커는 @Async 실패 또는 서버 재시작 후 남은 PENDING 레코드를 처리하는 안전망이다.

### 8.2 Kafka 전환 구조

`NotificationMessageOutputPort` 인터페이스가 유일한 교체 포인트다.

| 환경 | 구현체 | 동작 |
|---|---|---|
| 현재 (로컬) | SpringNotificationMessageAdapter | @Async로 Processor 직접 호출 |
| Kafka 도입 시 | KafkaNotificationMessageAdapter | kafkaTemplate.send()로 토픽 발행 |

Kafka 도입 시 도메인, 애플리케이션, 퍼시스턴스, 스케줄러, 웹 레이어는 전혀 변경하지 않는다.

---

## 9. 운영 시나리오 대응

| 시나리오 | 대응 방법 |
|---|---|
| PROCESSING 타임아웃 | 복구 워커가 5분마다 `processing_started_at` 10분 초과 레코드를 PENDING으로 복구. retryCount는 건드리지 않음 (발송 시도가 아니었으므로) |
| 서버 재시작 후 유실 | DB가 영속 저장소이므로 자동 해결. 재시작 후 폴링 워커가 PENDING/RETRYING 레코드를 자동 재처리 |
| 다중 인스턴스 중복 처리 | CAS UPDATE로 방지. 먼저 UPDATE에 성공한 인스턴스만 처리, 나머지는 affected_rows=0으로 skip |

---

## 10. 제약사항

| 항목 | 내용 |
|---|---|
| 이메일 발송 | 실제 발송 불필요. Mock 또는 로그 출력으로 대체 |
| 메시지 브로커 | 실제 설치 불필요. NotificationMessageOutputPort 인터페이스로 추상화하여 Kafka 전환 시 구현체만 교체 |

---

## 11. 알림 템플릿

`notification_templates` 테이블에 알림 타입 + 채널 조합별 메시지 템플릿을 관리한다.

- `titleTemplate`, `bodyTemplate` 필드에 `{변수명}` 플레이스홀더를 사용
- 발송 시 `referenceData` JSON을 파싱하여 변수 치환 후 발송
- 템플릿이 없는 경우 경고 로그만 남기고 발송을 건너뜀
- `(notification_type, channel)` UNIQUE 제약
