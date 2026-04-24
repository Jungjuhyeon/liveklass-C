## 기술 스택

- Spring Boot 4.0.5 / Java 21
- Spring Data JPA + MySQL 8.0
- Gradle Groovy DSL

## 아키텍처

DDD + 헥사고날 아키텍처 패턴을 따른다.

## 패키지 구조

```
com.jung.notificationservice
├── common
│   ├── domain          # BaseEntity (공통 베이스)
│   └── util            # IdempotencyKeyGenerator 등 공통 유틸
├── domain
│   ├── Notification.java
│   └── enumeration     # NotificationStatus, NotificationChannel
├── application
│   ├── usecase         # 유스케이스 인터페이스 (Input Port 정의)
│   ├── inputport       # 유스케이스 구현체
│   └── outputport      # 외부 의존성 인터페이스 (Output Port 정의)
└── framework
    ├── web             # Controller
    │   ├── request     # 요청 DTO
    │   └── response    # 응답 DTO
    └── persistence     # JpaRepository, Adapter (Output Port 구현체)
```

## Git 커밋 컨벤션

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서의 수정 |
| `style` | 코드 수정 없이 스타일만 변경 |
| `refactor` | 코드 리팩토링 |
| `test` | Test 관련 코드의 추가, 수정 |
| `chore` | 코드 수정 없이 설정 변경 |

커밋 메시지 형식: `<type> : <subject>`
