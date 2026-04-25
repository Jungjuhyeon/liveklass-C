package com.jung.notificationservice.application.usecase;

public interface ProcessNotificationUseCase {

    void processOne(String idempotencyKey);
}
