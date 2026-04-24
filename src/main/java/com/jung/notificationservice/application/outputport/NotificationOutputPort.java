package com.jung.notificationservice.application.outputport;

import com.jung.notificationservice.domain.Notification;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationOutputPort {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    int markAsProcessing(Long id, LocalDateTime now);
}
