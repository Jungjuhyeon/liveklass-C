package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {
        log.info("[EMAIL] 알림 발송 - notificationId={}, recipientId={}, type={}",
                notification.getId(),
                notification.getRecipientId(),
                notification.getNotificationType());
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }
}
