package com.jung.notificationservice.domain;

import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    @DisplayName("알림 생성 시 초기값이 올바르게 설정된다")
    void create() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);

        Notification notification = Notification.create(
                1L, "ENROLLMENT_COMPLETE", NotificationChannel.EMAIL,
                "{\"lectureId\":\"123\"}", "key-001", scheduledAt
        );

        assertThat(notification.getRecipientId()).isEqualTo(1L);
        assertThat(notification.getNotificationType()).isEqualTo("ENROLLMENT_COMPLETE");
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getReferenceData()).isEqualTo("{\"lectureId\":\"123\"}");
        assertThat(notification.getIdempotencyKey()).isEqualTo("key-001");
        assertThat(notification.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isZero();
        assertThat(notification.getMaxRetryCount()).isEqualTo(3);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getProcessingStartedAt()).isNull();
        assertThat(notification.getProcessedAt()).isNull();
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getLastErrorMessage()).isNull();
    }
}
