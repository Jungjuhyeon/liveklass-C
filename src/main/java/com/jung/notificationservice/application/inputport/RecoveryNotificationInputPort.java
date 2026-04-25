package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.RecoverNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryNotificationInputPort implements RecoverNotificationsUseCase {

    private static final int PROCESSING_TIMEOUT_MINUTES = 10;

    private final NotificationOutputPort notificationOutputPort;

    @Override
    @Transactional
    public void recoverStuck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        int count = notificationOutputPort.recoverStuckProcessing(threshold, now);
        if (count > 0) {
            log.info("[Recovery] PROCESSING 타임아웃 복구 - count={}", count);
        }
    }
}
