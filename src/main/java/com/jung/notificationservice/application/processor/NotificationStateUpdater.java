package com.jung.notificationservice.application.processor;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStateUpdater {

    private final NotificationOutputPort notificationOutputPort;

    @Transactional
    public void markResult(Long notificationId, boolean success, String error) {
        Notification notification = notificationOutputPort.findById(notificationId).orElseThrow();
        if (success) {
            notification.markCompleted();
            log.info("[Processor] 발송 완료 - notificationId={}", notificationId);
        } else {
            notification.markRetryingOrFailed(error);
            log.info("[Processor] 상태 전이 - notificationId={}, status={}, retryCount={}",
                    notificationId, notification.getStatus(), notification.getRetryCount());
        }
    }
}
