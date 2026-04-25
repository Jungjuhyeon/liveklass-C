package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.ReadNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReadNotificationInputPortTest {

    @Autowired
    private ReadNotificationUseCase readNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("읽음 처리 시 isRead가 true로 변경된다")
    void markAsRead_updatesIsReadToTrue() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));

        readNotificationUseCase.markAsRead(saved.getId());

        Notification found = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isRead()).isTrue();
    }

    @Test
    @DisplayName("이미 읽은 알림을 다시 읽음 처리해도 예외가 발생하지 않는다")
    void markAsRead_alreadyRead_doesNotThrow() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));

        readNotificationUseCase.markAsRead(saved.getId());
        readNotificationUseCase.markAsRead(saved.getId()); // 두 번째 호출

        Notification found = notificationJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 BusinessException을 던진다")
    void markAsRead_notFound_throwsBusinessException() {
        assertThatThrownBy(() -> readNotificationUseCase.markAsRead(999L))
                .isInstanceOf(BusinessException.class);
    }
}
