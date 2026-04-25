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
                "key-001", 1L, null, "ENROLLMENT_COMPLETE",
                "{\"lectureId\":\"123\"}", NotificationChannel.EMAIL, scheduledAt
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

    @Test
    @DisplayName("markCompleted 호출 시 COMPLETED 상태로 전이되고 processedAt이 설정된다")
    void markCompleted() {
        Notification notification = createNotification();

        notification.markCompleted();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
        assertThat(notification.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("발송 실패 시 retryCount가 증가하고 RETRYING 상태로 전이된다")
    void markRetryingOrFailed_withinMaxRetry() {
        Notification notification = createNotification();

        notification.markRetryingOrFailed("connection timeout");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getNextRetryAt()).isNotNull();
        assertThat(notification.getLastErrorMessage()).isEqualTo("connection timeout");
    }

    @Test
    @DisplayName("Exponential Backoff으로 nextRetryAt이 계산된다 (2^retryCount 분)")
    void markRetryingOrFailed_exponentialBackoff() {
        Notification notification = createNotification();
        LocalDateTime before = LocalDateTime.now();

        notification.markRetryingOrFailed("error"); // retryCount=1 → 2^1=2분
        assertThat(notification.getNextRetryAt()).isAfterOrEqualTo(before.plusMinutes(2));

        LocalDateTime before2 = LocalDateTime.now();
        notification.markRetryingOrFailed("error"); // retryCount=2 → 2^2=4분
        assertThat(notification.getNextRetryAt()).isAfterOrEqualTo(before2.plusMinutes(4));
    }

    @Test
    @DisplayName("최대 재시도 횟수 초과 시 FAILED 상태로 전이되고 nextRetryAt은 null이다")
    void markRetryingOrFailed_exceedsMaxRetry() {
        Notification notification = createNotification(); // maxRetryCount=3

        for (int i = 0; i < 4; i++) {
            notification.markRetryingOrFailed("error " + i);
        }

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getRetryCount()).isEqualTo(4);
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getLastErrorMessage()).isEqualTo("error 3");
    }

    @Test
    @DisplayName("resetForRetry 호출 시 PENDING 상태로 초기화된다")
    void resetForRetry() {
        Notification notification = createNotification();
        for (int i = 0; i < 4; i++) {
            notification.markRetryingOrFailed("error");
        }

        notification.resetForRetry();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isZero();
        assertThat(notification.getLastErrorMessage()).isNull();
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getProcessingStartedAt()).isNull();
    }

    private Notification createNotification() {
        return Notification.create(
                "key-001", 1L, null, "ENROLLMENT_COMPLETE",
                "{\"lectureId\":\"123\"}", NotificationChannel.EMAIL, null
        );
    }
}
