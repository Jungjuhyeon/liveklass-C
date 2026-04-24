package com.jung.notificationservice.application.outputport;

import com.jung.notificationservice.domain.Notification;

import java.util.Optional;

public interface NotificationOutputPort {

    Notification save(Notification notification);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
