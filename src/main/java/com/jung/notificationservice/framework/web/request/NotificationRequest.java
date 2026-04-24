package com.jung.notificationservice.framework.web.request;

import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private Long recipientId;
    private String notificationType;
    private String payload;
    private NotificationChannel channel;
    private LocalDateTime scheduledAt;
}
