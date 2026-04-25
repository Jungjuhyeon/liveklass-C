package com.jung.notificationservice.application.outputport;

import com.jung.notificationservice.domain.Notification;

public interface NotificationSendOutputPort {

    void send(Notification notification);
}
