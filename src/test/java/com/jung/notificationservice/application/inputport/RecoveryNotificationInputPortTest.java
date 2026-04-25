package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.RecoverNotificationsUseCase;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RecoveryNotificationInputPortTest {

    @Autowired
    private RecoverNotificationsUseCase recoverNotificationsUseCase;

    @Autowired
    private NotificationOutputPort notificationOutputPort;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("PROCESSING 상태로 10분 초과된 알림을 PENDING으로 복구한다")
    void recoverStuck_recoversTimedOutProcessingNotifications() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        // PROCESSING 상태로 전환 (15분 전 시작 → 타임아웃 대상)
        notificationOutputPort.markAsProcessing(saved.getId(), LocalDateTime.now().minusMinutes(15));

        recoverNotificationsUseCase.recoverStuck();

        Notification found = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("PROCESSING 상태로 10분 이내인 알림은 복구하지 않는다")
    void recoverStuck_doesNotRecoverRecentProcessing() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        // PROCESSING 상태로 전환 (5분 전 시작 → 타임아웃 아님)
        notificationOutputPort.markAsProcessing(saved.getId(), LocalDateTime.now().minusMinutes(5));

        recoverNotificationsUseCase.recoverStuck();

        Notification found = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
    }
}
