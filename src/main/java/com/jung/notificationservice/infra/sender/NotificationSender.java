package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;

public interface NotificationSender {

    void send(Notification notification);

    NotificationChannel supportedChannel();
}
