package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.domain.Notification;

public interface SaveNotificationUseCase {

    void saveIfAbsent(Notification notification);
}
