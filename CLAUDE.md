# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 & 테스트 명령어

```bash
./gradlew build              # 빌드 + 전체 테스트
./gradlew test               # 전체 테스트
./gradlew test --tests "com.jung.notificationservice.domain.NotificationTest"  # 단일 테스트 클래스
./gradlew test --tests "*.NotificationTest.메서드명"  # 단일 테스트 메서드
./gradlew bootRun            # 애플리케이션 실행 (MySQL 필요: localhost:3306/notification)
```

## 기술 스택

- Spring Boot 4.0.5 / Java 21
- Spring Data JPA + MySQL 8.0 (Docker)
- Gradle Groovy DSL

## 아키텍처

DDD + 헥사고날 아키텍처. 의존성 방향: `framework/infra → application → domain`

### 핵심 처리 흐름

알림은 즉시 발송하지 않고 DB에 PENDING으로 저장 후 비동기 처리한다. `notifications` 테이블이 Outbox + 메시지 큐 역할을 겸한다.

1. **API/도메인 이벤트** → `NotificationDomainEventListener`가 BEFORE_COMMIT에서 `UpsertNotificationUseCase`를 통해 DB 저장(원자성 보장), AFTER_COMMIT에서 `NotificationMessageOutputPort.publish()` 호출
2. **비동기 발송** → `SpringNotificationMessageAdapter`(@Async)가 `NotificationProcessor.processOne()` 실행
3. **안전망** → 폴링 워커(10초)가 누락된 PENDING/RETRYING 처리, 복구 워커(5분)가 PROCESSING 타임아웃(10분) 복구

### 중복 방지 2계층

- **요청 레벨**: UPSERT (`ON DUPLICATE KEY UPDATE`) + `idempotency_key` UNIQUE 제약
- **처리 레벨**: CAS UPDATE (`UPDATE WHERE status IN (PENDING, RETRYING)`)

### Kafka 전환 포인트

`NotificationMessageOutputPort` 구현체만 교체하면 됨. 현재는 `SpringNotificationMessageAdapter`(@Async 직접 호출), Kafka 도입 시 `KafkaNotificationMessageAdapter`로 교체.

## 패키지 구조

```
com.jung.notificationservice
├── common              # BaseEntity, IdempotencyKeyGenerator, ApiResponse, ErrorCode, GlobalExceptionHandler
├── domain              # Notification 엔티티, NotificationStatus/Channel enum, 도메인 이벤트
├── application
│   ├── usecase         # Input Port 인터페이스
│   ├── inputport       # UseCase 구현체
│   ├── outputport      # Output Port 인터페이스 (NotificationOutputPort, NotificationMessageOutputPort, NotificationSendOutputPort)
│   └── processor       # NotificationProcessor (CAS + 발송), NotificationStateUpdater
├── framework
│   ├── web             # Controller, Request/Response DTO
│   └── listener        # NotificationDomainEventListener (BEFORE_COMMIT 저장 + AFTER_COMMIT 발송)
└── infra
    ├── config          # AsyncConfig
    ├── messaging       # SpringNotificationMessageAdapter
    ├── persistence     # JpaRepository, NotificationAdapter
    ├── scheduler       # PollingWorker, RecoveryWorker
    └── sender          # NotificationSenderRouter, EmailSender, InAppSender (로그 출력 Mock)
```

## 테스트 전략

| 대상 | 방식 |
|---|---|
| 도메인 엔티티 | 순수 단위 테스트 — JPA, Spring Context 의존 금지 |
| Service / Processor | `@SpringBootTest` 통합 테스트 — 실제 MySQL 연결, `@AfterEach`로 데이터 정리 |
| Controller | `MockMvcBuilders.standaloneSetup` + `@ExtendWith(MockitoExtension.class)` — Spring Context 없이 Mock 사용 |
| 단순 컴포넌트 (Router, Listener 등) | `@ExtendWith(MockitoExtension.class)` 단위 테스트 |

## 문서

- [spec.md](docs/spec.md) — 요구사항 및 기능 명세
- [plan.md](docs/plan.md) — 기술 설계 및 단계별 구현 계획
- [task.md](docs/task.md) — 구현 체크리스트

## Git 커밋 컨벤션

커밋 메시지 형식: `<type> : <subject>`

타입: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
