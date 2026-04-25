package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.framework.web.request.NotificationRequest;

public interface SubmitNotificationUseCase {

    void submit(NotificationRequest request);
}
