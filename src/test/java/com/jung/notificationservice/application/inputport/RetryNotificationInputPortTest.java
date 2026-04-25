package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.RetryNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RetryNotificationInputPortTest {

    @Autowired
    private RetryNotificationUseCase retryNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("FAILED 알림을 수동 재시도하면 PENDING으로 전환되고 retryCount가 0으로 초기화된다")
    void retry_failedNotification_resetsToPending() {
        Notification notification = Notification.create("key-001", 1L, "evt-001",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        for (int i = 0; i < 4; i++) {
            notification.markRetryingOrFailed("error");
        }
        Notification saved = notificationJpaRepository.save(notification);

        Notification result = retryNotificationUseCase.retry(saved.getId());

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(result.getRetryCount()).isZero();
        assertThat(result.getLastErrorMessage()).isNull();
    }

    @Test
    @DisplayName("FAILED가 아닌 알림을 재시도하면 BusinessException을 던진다")
    void retry_nonFailedNotification_throwsBusinessException() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null)); // PENDING 상태

        assertThatThrownBy(() -> retryNotificationUseCase.retry(saved.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 알림 재시도 시 BusinessException을 던진다")
    void retry_notFound_throwsBusinessException() {
        assertThatThrownBy(() -> retryNotificationUseCase.retry(999L))
                .isInstanceOf(BusinessException.class);
    }
}
