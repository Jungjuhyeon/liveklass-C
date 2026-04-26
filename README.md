# 알림 발송 시스템

## ✅ 프로젝트 개요

수강 신청 완료, 결제 확정, 강의 시작 D-1, 취소 처리 등 다양한 비즈니스 이벤트 발생 시 사용자에게 이메일 또는 인앱 알림을 발송하는 시스템입니다.

알림 처리 실패가 비즈니스 트랜잭션에 영향을 주지 않으며, 재시도 정책과 중복 방지를 통해 안정적으로 알림을 발송합니다.

## ✅ 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| ORM | Spring Data JPA + Hibernate |
| DB | MySQL 8.0 (Docker) |
| Build | Gradle Groovy DSL |
| 비동기 | Spring @Async + @TransactionalEventListener |
| 스케줄러 | Spring @Scheduled |
| 테스트 | JUnit 5 + AssertJ + Mockito |

## ✅ 실행 방법

### 사전 요구사항

- Java 21
- Docker

### 1. MySQL 실행

```bash
docker compose up -d
```

MySQL이 `localhost:3306`에 실행됩니다. (DB: `notification`, 계정: `admin` / `pwd1234`)

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080`에 실행됩니다. JPA `ddl-auto: update`로 테이블이 자동 생성됩니다.

## ✅ API 목록 및 예시

### 1. 알림 발송 요청 등록

```
POST http://localhost:8080/api/notifications
```

**Request**

```json
{
  "eventId": "evt-001",
  "recipientId": 1,
  "notificationType": "ENROLLMENT_COMPLETE",
  "referenceData": "{\"lectureName\":\"스프링 부트\",\"userName\":\"홍길동\"}",
  "channel": "EMAIL",
  "scheduledAt": null
}
```

**Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "200",
  "message": "OK",
  "data": null
}
```

> 요청 즉시 발송하지 않고 DB에 PENDING 상태로 저장한 뒤, AFTER_COMMIT 이후 별도 스레드에서 비동기 발송합니다.

### 2. 알림 단건 상태 조회

```
GET http://localhost:8080/api/notifications/{id}
```

**Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "200",
  "message": "OK",
  "data": {
    "notificationId": 1,
    "status": "COMPLETED",
    "channel": "EMAIL",
    "retryCount": 0,
    "lastErrorMessage": null,
    "read": false,
    "createdAt": "2026-04-25T10:00:00",
    "processedAt": "2026-04-25T10:00:05"
  }
}
```

### 3. 사용자 알림 목록 조회

```
GET http://localhost:8080/api/notifications?recipientId=1&isRead=false
```

**Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "200",
  "message": "OK",
  "data": [
    {
      "notificationId": 1,
      "status": "COMPLETED",
      "channel": "EMAIL",
      "retryCount": 0,
      "lastErrorMessage": null,
      "read": false,
      "createdAt": "2026-04-25T10:00:00",
      "processedAt": "2026-04-25T10:00:05"
    }
  ]
}
```

### 4. 읽음 처리

```
PATCH http://localhost:8080/api/notifications/{id}/read
```

**Response** `200 OK`

> `UPDATE WHERE is_read = false` 조건으로 멱등성 보장. 여러 기기에서 동시 요청해도 한 번만 처리됩니다.

### 5. 수동 재시도

```
PATCH http://localhost:8080/api/notifications/{id}/retry
```

**Response** `200 OK`

```json
{
  "isSuccess": true,
  "code": "200",
  "message": "OK",
  "data": {
    "notificationId": 1,
    "status": "PENDING"
  }
}
```

> FAILED 상태인 알림만 재시도 가능합니다. PENDING으로 전환하고 retryCount를 0으로 초기화합니다.

## ✅ 데이터 모델 설명

### notifications 테이블

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT (PK) | 자동 생성 |
| idempotency_key | VARCHAR(64) UNIQUE | SHA256(eventId:recipientId:notificationType:channel) |
| recipient_id | BIGINT | 수신자 ID |
| event_id | VARCHAR(255) | 비즈니스 이벤트 식별자 |
| notification_type | VARCHAR(100) | 알림 타입 (ENROLLMENT_COMPLETE 등) |
| reference_data | TEXT | 부가 참조 데이터 JSON (템플릿 변수 치환용) |
| channel | VARCHAR(20) | 발송 채널 (EMAIL / IN_APP) |
| status | VARCHAR(20) | PENDING / PROCESSING / COMPLETED / RETRYING / FAILED |
| retry_count | INT | 현재 재시도 횟수 |
| max_retry_count | INT | 최대 재시도 횟수 (기본 3) |
| last_error_message | TEXT | 마지막 실패 사유 |
| is_read | BOOLEAN | 읽음 여부 |
| scheduled_at | DATETIME | 예약 발송 시각 (null이면 즉시 발송) |
| processing_started_at | DATETIME | 발송 처리 시작 시각 (타임아웃 감지용) |
| processed_at | DATETIME | 발송 완료 시각 |
| next_retry_at | DATETIME | 다음 재시도 시각 |
| created_at | DATETIME | 생성 시각 |
| updated_at | DATETIME | 수정 시각 |

**인덱스**: `idx_status_scheduled_retry(status, scheduled_at, next_retry_at)`, `idx_status_processing_started(status, processing_started_at)`, `idx_recipient_is_read(recipient_id, is_read)`

### notification_templates 테이블

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT (PK) | 자동 생성 |
| notification_type | VARCHAR(100) | 알림 타입 |
| channel | VARCHAR(20) | 발송 채널 |
| title_template | VARCHAR(255) | 제목 템플릿 (`{변수명}` 플레이스홀더) |
| body_template | TEXT | 본문 템플릿 |
| created_at | DATETIME | 생성 시각 |
| updated_at | DATETIME | 수정 시각 |

**UNIQUE 제약**: `(notification_type, channel)`

### 상태 전이 다이어그램

```
PENDING ──(CAS UPDATE 성공)──> PROCESSING ──(발송 성공)──> COMPLETED
                                    |
                        +-----------+-----------+
                  (retryCount < max)       (retryCount >= max)
                        |                       |
                     RETRYING               FAILED
                        |
              (next_retry_at 도래)
                        |
                   PROCESSING

PROCESSING ──(10분 초과)──> PENDING  ※ retryCount 유지

FAILED ──(수동 재시도 API)──> PENDING  ※ retryCount = 0 초기화
```

## ✅ 요구사항 해석 및 가정

### 1. 과제를 바라보는 세 가지 관점

이 과제를 처음 봤을 때 알림 시스템의 설계가 세 가지 관점으로 나뉜다고 생각했습니다.

**1️⃣ 모놀리식 아키텍처에서 알림 서비스를 붙이는 경우**

> 수강신청, 결제 같은 비즈니스 로직과 알림 서비스가 같은 서버에 존재합니다. 
이 경우 비즈니스 이벤트가 발생할 때마다 알림 발송 API를 호출하는 건 아닙니다. 
같은 서버 내에서 메서드를 직접 호출할 수도 있지만, 다양한 이벤트(수강 신청, 결제, 강의 시작 D-1, 취소 등)마다 알림 호출 코드를 하나하나 넣는 것은 유지보수성이 좋지 않습니다. 
그래서 `@TransactionalEventListener`를 통해 이벤트만 발행하면 알림 저장과 발송이 자동으로 처리되도록 구현했습니다.

**2️⃣ MSA 환경에서 API 호출 방식**

> 알림 서비스가 별도 서버로 분리된 경우, 수강신청 서비스나 결제 서비스가 알림 발송 요청 API(`POST /api/notifications`)를 호출하여 처리할 수 있습니다. 네트워크 통신에 따른 지연이 발생할 수 있지만, 비동기 통신으로 진행하면 충분히 대응 가능합니다.

**3️⃣ MSA 환경에서 이벤트 기반 아키텍처**

> Kafka 같은 메시지 브로커를 통해 비즈니스 이벤트를 발행하고, 알림 서버가 컨슈머로서 해당 토픽을 구독하여 알림 DB에 저장 후 발송을 처리하는 구조입니다.

이 세 가지 관점 중, "실제 메시지 브로커 설치 불필요. 단, 실제 운영 환경으로 전환 가능한 구조여야 함"이라는 제약사항을 고려하여 **1번 모놀리식 관점**을 기준으로 구현하되, `NotificationMessageOutputPort` 인터페이스 추상화를 통해 2번, 3번으로 전환 가능한 구조를 갖추었습니다.

### 2. 알림 처리 실패가 비즈니스 트랜잭션에 영향을 주어서는 안 됩니다

- **발송 실패**(이메일 서버 오류, 네트워크 장애 등)가 비즈니스에 영향을 주면 안 된다고 해석했습니다.
- 알림 저장은 BEFORE_COMMIT에서 비즈니스 트랜잭션과 함께 처리하여 원자성을 보장하고, 실제 발송은 AFTER_COMMIT 이후 @Async로 별도 스레드에서 처리합니다.
- 발송 실패 시 예외를 무시하지 않고 RETRYING/FAILED 상태로 관리하며, lastErrorMessage에 실패 사유를 기록합니다.

### 3. 실제 메시지 브로커 없이 구현하되, 운영 환경으로 전환 가능한 구조

- `NotificationMessageOutputPort` 인터페이스로 추상화하여 현재는 `SpringNotificationMessageAdapter`(@Async 직접 호출)를 사용합니다.
- Kafka 도입 시 `KafkaNotificationMessageAdapter` 구현체만 교체하면 되며, 도메인, 애플리케이션, 퍼시스턴스, 웹 레이어는 변경 없이 전환 가능합니다.

### 4. 동일한 이벤트에 대해 알림이 중복 발송되면 안 됩니다

- **요청 레벨**: `INSERT ... ON DUPLICATE KEY UPDATE` (UPSERT) 전략으로 중복 요청 시 예외 없이 무시합니다. `idempotency_key` UNIQUE 제약이 DB 레벨에서 원자적으로 중복을 방지합니다.
- **처리 레벨**: CAS UPDATE(`UPDATE WHERE status IN (PENDING, RETRYING)`)로 동시 처리 방지. affected_rows가 0이면 skip합니다.

## ✅ 설계 결정과 이유

### 1. BEFORE_COMMIT + AFTER_COMMIT 이벤트 리스너 구조

```
비즈니스 트랜잭션 (수강 신청, 결제 등)
  └─ ApplicationEventPublisher.publishEvent()
      ├─ BEFORE_COMMIT: 알림 DB 저장 (비즈니스와 같은 트랜잭션, 원자성 보장)
      └─ AFTER_COMMIT: @Async로 비동기 발송 (발송 실패가 비즈니스에 영향 없음)
```

- **BEFORE_COMMIT으로 저장하는 이유**: 비즈니스 트랜잭션과 알림 저장이 같은 트랜잭션에서 처리되어, 비즈니스가 롤백되면 알림도 함께 롤백됩니다.
- **AFTER_COMMIT으로 발송하는 이유**: 발송은 커밋 이후 별도 스레드에서 처리되므로, 이메일 서버 오류 등 발송 실패가 비즈니스 트랜잭션에 영향을 주지 않습니다.
- **이벤트 리스너를 사용하는 이유**: 모놀리식 환경에서 다양한 비즈니스 이벤트(수강 신청, 결제, 취소 등)마다 알림 호출 코드를 직접 넣는 대신, 이벤트만 발행하면 알림이 자동으로 처리됩니다. 유지보수성이 좋고, Kafka 도입 시에도 발행 로직에 끼워넣기만 하면 됩니다.

### 2. notifications 테이블이 Outbox + 메시지 큐 역할

별도의 outbox 테이블이나 메시지 브로커 없이, notifications 테이블 자체가 영속 저장소이자 작업 큐 역할을 합니다.
- 서버 재시작 후에도 DB에 남은 PENDING 레코드를 폴링 워커가 자동 재처리합니다.
- 구조가 단순하면서도 메시지 유실이 없습니다.

### 3. 비동기 처리 이중 경로 (이벤트 + 폴링)

```
정상 경로: AFTER_COMMIT → @Async → Processor.processOne() (즉시 처리)
안전망:    PollingWorker (10초마다) → PENDING/RETRYING 재처리
```

- 정상 경로에서는 발송 지연이 없고, @Async 실패나 서버 재시작 시 폴링 워커가 보완합니다.

### 4. CAS UPDATE로 다중 인스턴스 중복 처리 방지

```sql
UPDATE notifications SET status = 'PROCESSING'
WHERE id = :id AND status IN ('PENDING', 'RETRYING')
```

- 먼저 UPDATE에 성공한 인스턴스만 발송을 처리하고, 나머지는 affected_rows=0으로 skip합니다.
- 별도의 분산 락 없이 DB 레벨에서 동시성을 제어합니다.

### 5. Processor에 트랜잭션을 두지 않는 이유

```
processOne() ← 트랜잭션 없음
  ├─ markAsProcessing()  ← 자체 트랜잭션, 즉시 커밋
  ├─ send()              ← 트랜잭션 밖, 느릴 수 있음
  └─ markResult()        ← 자체 트랜잭션, 즉시 커밋
```

- markAsProcessing이 즉시 커밋되어야 다른 인스턴스가 CAS 결과를 즉시 확인할 수 있습니다.
- send()가 느린 경우 DB 커넥션을 불필요하게 점유하지 않습니다.

### 6. 재시도 정책 — Exponential Backoff

| retryCount | 대기 시간 |
|---|---|
| 1 | 2분 (2^1) |
| 2 | 4분 (2^2) |
| 3 | 8분 (2^3) |
| 4 | FAILED 전이 |

- 일시적 장애에 대해 점진적으로 간격을 넓혀 재시도합니다.
- FAILED 상태는 자동 재시도 대상에서 제외되며, 운영자가 원인 확인 후 수동 재시도(retryCount 초기화) 할 수 있습니다.

## ✅ 선택 구현 항목

### 1. 발송 스케줄링

`scheduledAt` 필드를 통해 특정 시각에 발송을 예약할 수 있습니다. `scheduledAt`이 null이면 즉시 발송 대상이고, 값이 있으면 해당 시각 이후에 폴링 워커가 조회하여 처리합니다.

```sql
WHERE status IN ('PENDING', 'RETRYING')
AND (scheduled_at IS NULL OR scheduled_at <= :now)
AND (next_retry_at IS NULL OR next_retry_at <= :now)
```

### 2. 알림 템플릿 관리

`notification_templates` 테이블에 알림 타입 + 채널 조합별 메시지 템플릿을 관리합니다.

- `titleTemplate`, `bodyTemplate`에 `{변수명}` 플레이스홀더를 사용
- 발송 시 `referenceData` JSON을 `ReferenceDataParser`로 파싱하여 변수 치환 후 발송
- 템플릿이 없는 경우 기본 템플릿(알림 타입을 제목으로 사용)으로 발송하여 알림 유실을 방지

```
// 템플릿 예시
titleTemplate: "{lectureName} 수강 신청 완료"
bodyTemplate: "안녕하세요 {userName}님, {lectureName} 강의 수강 신청이 완료되었습니다."

// referenceData
{"lectureName": "스프링 부트", "userName": "홍길동"}

// 치환 결과
title: "스프링 부트 수강 신청 완료"
body: "안녕하세요 홍길동님, 스프링 부트 강의 수강 신청이 완료되었습니다."
```

### 3. 읽음 처리 — 여러 기기 동시 요청

`UPDATE WHERE is_read = false` 조건 하나로 멱등성을 보장합니다.

```sql
UPDATE notifications SET is_read = true
WHERE id = :id AND is_read = false
```

- 여러 기기에서 동시에 읽음 처리 요청이 와도 첫 번째 요청만 affected_rows=1이고, 나머지는 affected_rows=0으로 한 번만 처리됩니다.
- 별도의 락 없이 SQL 조건만으로 동시성을 해결합니다.

### 4. 최종 실패 알림 보관 및 수동 재시도

FAILED 상태의 알림은 삭제되지 않고 DB에 보관되며, `lastErrorMessage`에 실패 사유가 기록되어 운영자가 원인을 확인할 수 있습니다.

- `PATCH /api/notifications/{id}/retry` API로 수동 재시도가 가능합니다.
- 재시도 시 **retryCount를 0으로 초기화**합니다. 초기화하지 않으면 이미 최대 재시도 횟수에 도달한 상태이므로 재시도 즉시 다시 FAILED가 됩니다.
- 운영자가 원인을 확인하고 조치한 뒤 재시도하는 맥락이므로, 초기화하여 새로운 재시도 사이클을 시작하는 것이 적절합니다.

## ✅ 요구사항 대응 정리

| 요구사항 | 구현 방법 |
|---|---|
| API 스레드와 분리 | `AFTER_COMMIT` + `SpringNotificationMessageAdapter` `@Async` |
| 비즈니스 트랜잭션 영향 없음 | `AFTER_COMMIT` 이후 발송, 발송 실패가 비즈니스에 전파되지 않음 |
| 예외 단순 무시 아님 | `markRetryingOrFailed()`로 RETRYING/FAILED 상태 관리 + `lastErrorMessage` 기록 |
| 재시도 | Exponential Backoff (`2^retryCount`분, 최대 3회) |
| 중복 발송 방지 | `idempotency_key` UNIQUE 제약 + UPSERT (`ON DUPLICATE KEY UPDATE`) |
| 동시 중복 처리 방지 | CAS UPDATE (`UPDATE WHERE status IN ('PENDING', 'RETRYING')`) |
| PROCESSING 타임아웃 복구 | `NotificationRecoveryWorker` 5분마다 10분 초과 건 PENDING 복구 |
| 서버 재시작 유실 없음 | DB 영속 저장 + `NotificationPollingWorker` 10초마다 재처리 |
| 다중 인스턴스 중복 방지 | CAS UPDATE (`affected_rows=0`이면 skip) |
| 읽음 처리 동시 요청 | `UPDATE WHERE is_read = false` 멱등성 보장 |
| 수동 재시도 | FAILED → PENDING 전환, `retryCount` 0 초기화 |
| Kafka 전환 | `NotificationMessageOutputPort` 구현체만 교체 |
| 예약 발송 | `scheduledAt` 컬럼 + 폴링 쿼리 `WHERE scheduledAt <= :now` 조건 |
| 알림 템플릿 | `notification_templates` 테이블 + `{변수}` 치환, 없으면 기본 템플릿 발송 |

## ✅ 테스트 실행 방법

### 사전 요구사항

- MySQL 실행 필요 (`docker compose up -d`)

### 전체 테스트

```bash
./gradlew test
```

### 단일 테스트 클래스

```bash
./gradlew test --tests "com.jung.notificationservice.domain.NotificationTest"
```

### 테스트 전략

| 대상 | 방식 | 테스트 수 |
|---|---|---|
| 도메인 엔티티 (Notification, NotificationTemplate, ReferenceDataParser) | 순수 단위 테스트 | 13 |
| Service / Processor (Submit, Inquiry, Read, Retry, Recovery, Polling, Persistence, Processor) | @SpringBootTest 통합 테스트 (실제 MySQL) | 22 |
| Controller | MockMvcBuilders.standaloneSetup + Mockito | 7 |
| 단순 컴포넌트 (Router, Sender, Listener) | @ExtendWith(MockitoExtension.class) | 10 |
| **합계** | | **53** |

## ✅ 미구현 / 제약사항

- **이메일 발송**: 실제 발송하지 않고 로그 출력으로 대체합니다.
- **메시지 브로커**: 실제 Kafka 등을 사용하지 않으며, `NotificationMessageOutputPort` 인터페이스로 추상화하여 전환 가능한 구조만 갖추었습니다.
- **폴링 워커 단일 스레드**: 현재 폴링 워커가 조회된 알림을 순차 처리합니다. 대량 적체 시 병렬 처리로 개선 가능합니다.
- **@Async 큐 포화**: 스레드풀 queue=100 초과 시 TaskRejectedException이 발생할 수 있으나, 폴링 워커가 안전망으로 누락을 방지합니다.

## ✅ AI 활용 범위

- 본 과제에서는 **GPT와 Claude Code(claude.ai/code)**를 활용하여 요구사항 분석 및 개발을 진행하였습니다.
- 요구사항에 대해 스스로 고민한 내용을 바탕으로 GPT를 통해 설계 방향과 트레이드오프를 검토하였고, 이를 통해 요구사항을 구체화하였습니다.
- 구현 과정에서는 Claude Code를 활용하여 코드 리뷰, 테스트 코드 작성, 버그 탐지(Processor readOnly 트랜잭션 문제, 중복 저장 시 트랜잭션 오염 문제), 문서 작성 등을 보조적으로 수행하였습니다.
- 헥사고날 아키텍처 적용, 이벤트 리스너 구조 설계, 재시도 정책 수립 등 핵심 설계 결정은 직접 수행하였으며, AI는 구현 보조 및 검증 도구로 활용하였습니다.
