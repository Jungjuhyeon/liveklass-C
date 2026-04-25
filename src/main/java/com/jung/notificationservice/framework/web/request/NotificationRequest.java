package com.jung.notificationservice.framework.web.request;

import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "수신자 ID는 필수입니다.")
    private Long recipientId;
    @NotBlank(message = "알림 타입(notificationType)은 필수입니다.")
    private String notificationType;
    @NotBlank(message = "이벤트 ID(eventId)는 필수입니다.")
    private String eventId;
    private String referenceData;
    @NotNull(message = "발송 채널(channel)은 필수입니다.")
    private NotificationChannel channel;
    private LocalDateTime scheduledAt;
}
