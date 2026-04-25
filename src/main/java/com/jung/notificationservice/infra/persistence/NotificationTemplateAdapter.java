package com.jung.notificationservice.infra.persistence;

import com.jung.notificationservice.application.outputport.NotificationTemplateOutputPort;
import com.jung.notificationservice.domain.NotificationTemplate;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationTemplateAdapter implements NotificationTemplateOutputPort {

    private final NotificationTemplateJpaRepository templateJpaRepository;

    @Override
    public Optional<NotificationTemplate> findByTypeAndChannel(String notificationType, NotificationChannel channel) {
        return templateJpaRepository.findByNotificationTypeAndChannel(notificationType, channel);
    }
}
