package com.jung.notificationservice.framework.listener;

import com.jung.notificationservice.application.outputport.NotificationMessageOutputPort;
import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.UpsertNotificationUseCase;
import com.jung.notificationservice.common.util.IdempotencyKeyGenerator;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.event.NotificationDomainEvent;
import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDomainEventListener {

    private final NotificationOutputPort repositoryPort;
    private final NotificationMessageOutputPort messagePort;
    private final UpsertNotificationUseCase upsertNotificationUseCase;

    // ── 도메인 이벤트 경로 ──
    // BEFORE_COMMIT: 비즈니스 트랜잭션과 함께 DB 저장 (원자성 보장)
    // 중복 체크 후 저장하여 대부분의 중복을 예외 없이 처리
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void saveNotification(NotificationDomainEvent event) {
        String key = IdempotencyKeyGenerator.generate(
                event.getEventId(), event.getRecipientId(), event.getNotificationType(), event.getChannel()
        );
        upsertNotificationUseCase.upsert(
                Notification.create(
                        key, event.getRecipientId(), event.getEventId(),
                        event.getNotificationType(), event.getReferenceData(),
                        event.getChannel(), event.getScheduledAt()
                )
        );
    }

    // AFTER_COMMIT: Consumer 역할
    // 발송 실패해도 비즈니스 트랜잭션과 무관
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchDomainNotification(NotificationDomainEvent event) {
        repositoryPort.findByIdempotencyKey(
                IdempotencyKeyGenerator.generate(
                        event.getEventId(), event.getRecipientId(), event.getNotificationType(), event.getChannel()
                )
        ).ifPresent(n -> messagePort.publish(
                NotificationRegisteredEvent.create(
                        n.getIdempotencyKey(), n.getRecipientId(), n.getEventId(),
                        n.getNotificationType(), n.getReferenceData(),
                        n.getChannel(), n.getScheduledAt()
                )
        ));
    }
}
