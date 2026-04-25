package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.InquiryNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InquiryNotificationInputPortTest {

    @Autowired
    private InquiryNotificationUseCase inquiryNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("알림 ID로 단건 조회한다")
    void findById_returnsNotification() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));

        Notification found = inquiryNotificationUseCase.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getNotificationType()).isEqualTo("ENROLLMENT_COMPLETE");
    }

    @Test
    @DisplayName("존재하지 않는 알림 조회 시 BusinessException을 던진다")
    void findById_notFound_throwsBusinessException() {
        assertThatThrownBy(() -> inquiryNotificationUseCase.findById(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("수신자 ID로 알림 목록을 조회한다")
    void findByRecipientId_returnsList() {
        notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        notificationJpaRepository.save(
                Notification.create("key-002", 1L, "evt-002", "PAYMENT_CONFIRMED",
                        "{}", NotificationChannel.IN_APP, null));
        notificationJpaRepository.save(
                Notification.create("key-003", 2L, "evt-003", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));

        List<Notification> result = inquiryNotificationUseCase.findByRecipientId(1L, null);

        assertThat(result).hasSize(2);
    }
}
