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
- [x] `DataIntegrityViolationException` catch → 기존 레코드 반환
- [x] `NotificationJpaRepository` 작성
- [x] `NotificationAdapter` 작성 (`NotificationOutputPort` 구현체)
- [x] `NotificationController` 작성 — `POST /notifications`
- [x] 저장 후 `202 Accepted` 반환 확인

---

## Phase 3 — 비동기 발송 처리

- [x] `NotificationMessageOutputPort` 인터페이스 정의
- [ ] `MockEmailSender` 작성 (로그 출력)
- [ ] `MockInAppSender` 작성 (로그 출력)
- [ ] `NotificationProcessor.processOne()` 작성 (CAS UPDATE)
- [ ] `SpringNotificationMessageAdapter` 작성 (`@Async`)
- [ ] `TransactionSynchronizationAdapter` 등록 (AFTER_COMMIT)
- [ ] `AsyncConfig` 스레드풀 설정 + `@EnableAsync`

---

## Phase 4 — 재시도 & 실패 처리

- [ ] 발송 성공 시 `COMPLETED` + `processedAt` 기록
- [ ] 발송 실패 시 `RETRYING` 전이 + `nextRetryAt` 계산
- [ ] 최대 재시도 초과 시 `FAILED` + `lastErrorMessage` 기록
- [ ] Exponential Backoff 계산 구현 (`5^retryCount 분`)

---

## Phase 5 — 폴링 워커 & 복구 워커

- [ ] `NotificationPollingWorker` 작성 (`@Scheduled fixedDelay = 10s`)
- [ ] `NotificationRecoveryWorker` 작성 (`@Scheduled fixedDelay = 5min`)

---

## Phase 6 — 조회 API

- [ ] `GET /notifications/{id}` — 단건 상태 조회
- [ ] `GET /notifications?recipientId=&isRead=` — 목록 조회
- [ ] `PATCH /notifications/{id}/read` — 읽음 처리
