package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.domain.Notification;

public interface RetryNotificationUseCase {

    Notification retry(Long id);
}
