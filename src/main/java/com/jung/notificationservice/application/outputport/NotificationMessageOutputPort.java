package com.jung.notificationservice.application.outputport;

import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;

public interface NotificationMessageOutputPort {

    void publish(NotificationRegisteredEvent event);

}
