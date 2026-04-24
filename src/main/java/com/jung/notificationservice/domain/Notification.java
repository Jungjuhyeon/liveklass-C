package com.jung.notificationservice.domain;

import com.jung.notificationservice.common.domain.BaseEntity;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_next_retry_at", columnList = "next_retry_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 100)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(columnDefinition = "TEXT")
    private String referenceData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetryCount;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(nullable = false)
    private boolean isRead;

    private LocalDateTime scheduledAt;

    private LocalDateTime processingStartedAt;

    private LocalDateTime processedAt;

    private LocalDateTime nextRetryAt;

    private Notification(Long recipientId, String notificationType, NotificationChannel channel,
                         String referenceData, String idempotencyKey, LocalDateTime scheduledAt) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.referenceData = referenceData;
        this.idempotencyKey = idempotencyKey;
        this.scheduledAt = scheduledAt;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.isRead = false;
    }

    public static Notification create(Long recipientId, String notificationType,
                                      NotificationChannel channel, String referenceData,
                                      String idempotencyKey, LocalDateTime scheduledAt) {
        return new Notification(recipientId, notificationType, channel, referenceData, idempotencyKey, scheduledAt);
    }
}
