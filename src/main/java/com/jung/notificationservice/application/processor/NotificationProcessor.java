package com.jung.notificationservice.application.processor;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.outputport.NotificationSendOutputPort;
import com.jung.notificationservice.application.usecase.ProcessNotificationUseCase;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor implements ProcessNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;
    private final NotificationSendOutputPort notificationSendOutputPort;
    private final NotificationStateUpdater stateUpdater;

    @Override
    public void processOne(String idempotencyKey) {
        Notification notification = notificationOutputPort.findByIdempotencyKey(idempotencyKey).orElseThrow();
        Long notificationId = notification.getId();

        int updated = notificationOutputPort.markAsProcessing(notificationId, LocalDateTime.now());
        if (updated == 0) {
            log.debug("[Processor] 이미 처리 중인 알림 skip - idempotencyKey={}", idempotencyKey);
            return;
        }

        boolean success = false;
        String error = null;
        try {
            notificationSendOutputPort.send(notification);
            success = true;
        } catch (Exception e) {
            error = e.getMessage();
            log.warn("[Processor] 발송 실패 - idempotencyKey={}, error={}", idempotencyKey, e.getMessage());
        }

        stateUpdater.markResult(notificationId, success, error);
    }
}
