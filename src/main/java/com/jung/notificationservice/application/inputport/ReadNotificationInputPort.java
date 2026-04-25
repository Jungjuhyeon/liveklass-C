package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.ReadNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadNotificationInputPort implements ReadNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;

    @Override
    @Transactional
    public void markAsRead(Long id) {
        notificationOutputPort.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        int updated = notificationOutputPort.markAsRead(id, LocalDateTime.now());
        if (updated == 0) {
            log.debug("[Read] 이미 읽음 처리된 알림 - notificationId={}", id);
        }
    }
}
