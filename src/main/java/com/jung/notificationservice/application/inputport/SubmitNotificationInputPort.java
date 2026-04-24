package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.common.util.IdempotencyKeyGenerator;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmitNotificationInputPort implements SubmitNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;

    @Override
    @Transactional
    public Notification submit(NotificationRequest request) {
        String idempotencyKey = IdempotencyKeyGenerator.generate(
                request.getPayload(), request.getRecipientId(), request.getNotificationType(), request.getChannel()
        );

        try {
            return notificationOutputPort.save(
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
            return notificationOutputPort.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_DUPLICATE));
        }
    }
}
