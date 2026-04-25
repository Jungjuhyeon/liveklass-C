package com.jung.notificationservice.framework.web.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonPropertyOrder({"notificationId", "status", "channel", "retryCount", "lastErrorMessage", "createdAt", "processedAt", "read"})
public class NotificationDetailResponse {

    private final Long notificationId;
    private final NotificationStatus status;
    private final NotificationChannel channel;
    private final int retryCount;
    private final String lastErrorMessage;
    private final boolean isRead;
    private final LocalDateTime createdAt;
    private final LocalDateTime processedAt;

    public static NotificationDetailResponse mapToDTO(Notification notification) {
        return NotificationDetailResponse.builder()
                .notificationId(notification.getId())
                .status(notification.getStatus())
                .channel(notification.getChannel())
                .retryCount(notification.getRetryCount())
                .lastErrorMessage(notification.getLastErrorMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .processedAt(notification.getProcessedAt())
                .build();
    }
}
