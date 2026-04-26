# Task — 알림 발송 시스템

## Phase 1 — 도메인 모델 & DB 설계

- [x] `NotificationStatus` enum 작성
- [x] `NotificationChannel` enum 작성
- [x] `BaseEntity` 작성 (`common/domain`, JPA Auditing)
- [x] `Notification` 엔티티 작성 (정적 팩토리 메서드 패턴)
- [x] `@EnableJpaAuditing` 메인 클래스에 추가
- [x] `application.yml` `ddl-auto: update` 설정 확인
- [x] `NotificationTest` 단위 테스트 작성

---

## Phase 2 — 알림 저장 API

- [x] 요청 DTO 작성 (`recipientId`, `notificationType`, `referenceData`, `channel`, `scheduledAt`)
- [x] 응답 DTO 작성 (`notificationId`, `status`)
- [x] `SubmitNotificationInputPort.submit()` 작성 (inputport)
- [x] `idempotency_key` 생성 로직 작성 (`common/util/IdempotencyKeyGenerator`)
- [x] `UpsertNotificationUseCase` + `NotificationPersistenceService` 작성 (UPSERT 전략으로 중복 방지)
- [x] `NotificationJpaRepository` 작성
- [x] `NotificationAdapter` 작성 (`NotificationOutputPort` 구현체)
- [x] `NotificationController` 작성 — `POST /api/notifications`
- [x] 저장 후 `202 Accepted` 반환 확인

---

## Phase 3 — 비동기 발송 처리

- [x] `NotificationMessageOutputPort` 인터페이스 정의
- [x] `EmailNotificationSender` 작성 (템플릿 기반 로그 출력)
- [x] `InAppNotificationSender` 작성 (템플릿 기반 로그 출력)
- [x] `NotificationProcessor.processOne()` 작성 (CAS UPDATE)
- [x] `NotificationStateUpdater` 작성 (별도 트랜잭션에서 상태 반영)
- [x] `SpringNotificationMessageAdapter` 작성 (`@Async`)
- [x] `NotificationDomainEventListener` 작성 (BEFORE_COMMIT 저장 + AFTER_COMMIT 발송)
- [x] `AsyncConfig` 스레드풀 설정 + `@EnableAsync` + `@EnableScheduling`

---

## Phase 4 — 재시도 & 실패 처리

- [x] 발송 성공 시 `COMPLETED` + `processedAt` 기록
- [x] 발송 실패 시 `RETRYING` 전이 + `nextRetryAt` 계산
- [x] 최대 재시도 초과 시 `FAILED` + `lastErrorMessage` 기록
- [x] Exponential Backoff 계산 구현 (`2^retryCount 분`)

---

## Phase 5 — 폴링 워커 & 복구 워커

- [x] `NotificationPollingWorker` 작성 (`@Scheduled fixedDelay = 10s`)
- [x] `NotificationRecoveryWorker` 작성 (`@Scheduled fixedDelay = 5min`)

---

## Phase 6 — 조회 & 수동 재시도 API

- [x] `GET /api/notifications/{id}` — 단건 상태 조회
- [x] `GET /api/notifications?recipientId=&isRead=` — 목록 조회
- [x] `PATCH /api/notifications/{id}/read` — 읽음 처리
- [x] `PATCH /api/notifications/{id}/retry` — FAILED 알림 수동 재시도 (PENDING 전환 + retryCount 0 초기화)

---

## Phase 7 — 알림 템플릿

- [x] `NotificationTemplate` 엔티티 작성 (정적 팩토리 메서드, `resolve()` 변수 치환)
- [x] `NotificationTemplateOutputPort` 인터페이스 정의
- [x] `NotificationTemplateJpaRepository` 작성
- [x] `NotificationTemplateAdapter` 작성
- [x] `ReferenceDataParser` 작성 (JSON → Map 변환)
- [x] `EmailNotificationSender` / `InAppNotificationSender` 템플릿 연동
