package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.common.util.IdempotencyKeyGenerator;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmitNotificationInputPort implements SubmitNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Notification submit(NotificationRequest request) {
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                request.getPayload(), request.getRecipientId(), request.getNotificationType(), request.getChannel()
        );

        Notification saved;
        try {
            saved = notificationOutputPort.save(
                    Notification.create(
                            request.getRecipientId(),
                            request.getNotificationType(),
                            request.getChannel(),
                            request.getPayload(),
                            idempotencyKey,
                            request.getScheduledAt()
                    )
            );
        } catch (DataIntegrityViolationException e) {
            saved = notificationOutputPort.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_DUPLICATE));
        }

        // 트랜잭션 안에서 이벤트 등록
        // → 커밋 후 AFTER_COMMIT 리스너 실행
        // → @Async 별도 스레드에서 발송
        // → 즉시 return (API 스레드 블로킹 없음)
        eventPublisher.publishEvent(new NotificationRegisteredEvent(saved.getId()));

        return saved;
    }
}
