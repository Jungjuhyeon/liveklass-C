package com.jung.notificationservice.domain.event;

import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationRegisteredEvent {
    private final String idempotencyKey;
    private final Long recipientId;
    private final String eventId;
    private final String notificationType;
    private final String referenceData;
    private final NotificationChannel channel;
    private final LocalDateTime scheduledAt;

    private NotificationRegisteredEvent(String idempotencyKey, Long recipientId, String eventId,
                                        String notificationType, String referenceData,
                                        NotificationChannel channel, LocalDateTime scheduledAt) {
        this.idempotencyKey = idempotencyKey;
        this.recipientId = recipientId;
        this.eventId = eventId;
        this.notificationType = notificationType;
        this.referenceData = referenceData;
        this.channel = channel;
        this.scheduledAt = scheduledAt;
    }

    public static NotificationRegisteredEvent create(String idempotencyKey, Long recipientId, String eventId,
                                                     String notificationType, String referenceData,
                                                     NotificationChannel channel, LocalDateTime scheduledAt) {
        return new NotificationRegisteredEvent(idempotencyKey, recipientId, eventId,
                notificationType, referenceData, channel, scheduledAt);
    }

}
