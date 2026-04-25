package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.PollNotificationsUseCase;
import com.jung.notificationservice.application.usecase.ProcessNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class PollingNotificationInputPort implements PollNotificationsUseCase {

    private final NotificationOutputPort notificationOutputPort;
    private final ProcessNotificationUseCase processNotificationUseCase;

    @Override
    public void pollPending() {
        List<String> keys = notificationOutputPort.findPendingKeys(LocalDateTime.now());
        if (keys.isEmpty()) {
            return;
        }
        log.debug("[Polling] 처리 대상 알림 - count={}", keys.size());
        keys.forEach(processNotificationUseCase::processOne);
    }
}
