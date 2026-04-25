package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.SaveNotificationUseCase;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPersistenceService implements SaveNotificationUseCase {

    private final NotificationOutputPort repositoryPort;

    @Override
    public void saveIfAbsent(Notification notification) {
        if (repositoryPort.findByIdempotencyKey(notification.getIdempotencyKey()).isPresent()) {
            log.debug("중복 알림 무시 — key={}", notification.getIdempotencyKey());
            return;
        }
        repositoryPort.save(notification);
    }
}
