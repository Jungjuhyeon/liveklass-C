package com.jung.notificationservice.framework.web.response;

import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

    private final Long notificationId;
    private final NotificationStatus status;

    public static NotificationResponse mapToDTO(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .status(notification.getStatus())
                .build();
    }
}
