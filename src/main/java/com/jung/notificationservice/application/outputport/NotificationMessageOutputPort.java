package com.jung.notificationservice.application.outputport;

public interface NotificationMessageOutputPort {

    void publish(Long notificationId);
}
