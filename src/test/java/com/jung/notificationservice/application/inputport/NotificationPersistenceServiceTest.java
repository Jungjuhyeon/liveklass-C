package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.UpsertNotificationUseCase;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationPersistenceServiceTest {

    @Autowired
    private UpsertNotificationUseCase upsertNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("알림이 존재하지 않으면 저장한다")
    void upsert_savesNewNotification() {
        Notification notification = Notification.create("key-001", 1L, "evt-001",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);

        upsertNotificationUseCase.upsert(notification);

        assertThat(notificationJpaRepository.findByIdempotencyKey("key-001")).isPresent();
    }

    @Test
    @DisplayName("동일한 idempotencyKey가 이미 존재하면 기존 데이터를 유지한다")
    void upsert_duplicateKey_keepsExisting() {
        notificationJpaRepository.save(
                Notification.create("key-002", 1L, "evt-002", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));

        Notification duplicate = Notification.create("key-002", 2L, "evt-003",
                "PAYMENT_CONFIRMED", "{}", NotificationChannel.IN_APP, null);
        upsertNotificationUseCase.upsert(duplicate);

        assertThat(notificationJpaRepository.count()).isEqualTo(1);
    }
}
