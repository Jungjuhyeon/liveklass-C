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
        @Index(name = "idx_status_scheduled_retry", columnList = "status, scheduled_at, next_retry_at"),
        @Index(name = "idx_status_processing_started", columnList = "status, processing_started_at"),
        @Index(name = "idx_recipient_is_read", columnList = "recipient_id, is_read")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long recipientId;

    private String eventId; //AggregateId

    @Column(nullable = false, length = 100)
    private String notificationType; //이벤트 타입(aggregateType)

    @Column(columnDefinition = "TEXT")
    private String referenceData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel; //발송채널

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

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

    private Notification(String idempotencyKey, Long recipientId, String eventId, String notificationType,
                         String referenceData, NotificationChannel channel, LocalDateTime scheduledAt) {
        this.idempotencyKey = idempotencyKey;
        this.recipientId = recipientId;
        this.eventId = eventId;
        this.notificationType = notificationType;
        this.referenceData = referenceData;
        this.channel = channel;
        this.scheduledAt = scheduledAt;

        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.maxRetryCount = 3;
        this.isRead = false;
    }

    public static Notification create(String idempotencyKey, Long recipientId,
                                      String eventId, String notificationType,
                                      String referenceData, NotificationChannel channel, LocalDateTime scheduledAt){
        return new Notification(idempotencyKey, recipientId, eventId, notificationType, referenceData, channel,scheduledAt);
    }

    public void resetForRetry() {
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.lastErrorMessage = null;
        this.nextRetryAt = null;
        this.processingStartedAt = null;
    }

    public void markCompleted() {
        this.status = NotificationStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markRetryingOrFailed(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        this.retryCount++;
        if (this.retryCount > this.maxRetryCount) {
            this.status = NotificationStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            this.status = NotificationStatus.RETRYING;
            this.nextRetryAt = LocalDateTime.now().plusMinutes((long) Math.pow(2, retryCount));
        }
    }
}
