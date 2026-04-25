package com.jung.notificationservice.application.outputport;

import com.jung.notificationservice.domain.NotificationTemplate;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;

import java.util.Optional;

public interface NotificationTemplateOutputPort {

    Optional<NotificationTemplate> findByTypeAndChannel(String notificationType, NotificationChannel channel);
}
