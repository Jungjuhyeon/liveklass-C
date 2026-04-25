package com.jung.notificationservice.infra.persistence;

import com.jung.notificationservice.domain.NotificationTemplate;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByNotificationTypeAndChannel(String notificationType, NotificationChannel channel);
}
