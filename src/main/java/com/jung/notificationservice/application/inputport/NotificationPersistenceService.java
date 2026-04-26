package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.UpsertNotificationUseCase;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService implements UpsertNotificationUseCase {

    private final NotificationOutputPort repositoryPort;

    @Override
    public void upsert(Notification notification) {
        int result = repositoryPort.upsert(notification);
        if (result == 1) {
            log.debug("알림 저장 완료 — key={}", notification.getIdempotencyKey());
        } else {
            log.debug("중복 알림 무시 — key={}", notification.getIdempotencyKey());
        }
    }
}
