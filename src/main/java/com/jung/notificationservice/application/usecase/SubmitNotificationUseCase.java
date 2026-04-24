package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.framework.web.request.NotificationRequest;

public interface SubmitNotificationUseCase {

    Notification submit(NotificationRequest request);
}
