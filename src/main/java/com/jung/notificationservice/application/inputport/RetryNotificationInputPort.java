package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.RetryNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryNotificationInputPort implements RetryNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;

    @Override
    @Transactional
    public Notification retry(Long id) {
        Notification notification = notificationOutputPort.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_RETRYABLE);
        }

        notification.resetForRetry();
        log.info("[Retry] 수동 재시도 등록 - notificationId={}", id);
        return notification;
    }
}
