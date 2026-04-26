package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.domain.Notification;

public interface UpsertNotificationUseCase {

    void upsert(Notification notification);
}
