package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import com.jung.notificationservice.framework.persistence.NotificationJpaRepository;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SubmitNotificationInputPortTest {

    @Autowired
    private SubmitNotificationUseCase submitNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("알림 저장 시 PENDING 상태로 저장된다")
    void submit_savesWithPendingStatus() {
        NotificationRequest request = buildRequest();

        Notification result = submitNotificationUseCase.submit(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(result.getRecipientId()).isEqualTo(1L);
        assertThat(result.getNotificationType()).isEqualTo("ENROLLMENT_COMPLETE");
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.isRead()).isFalse();
        assertThat(result.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("동일한 요청이 중복으로 들어오면 기존 알림을 반환한다")
    void submit_returnExistingOnDuplicate() {
        NotificationRequest request = buildRequest();

        Notification first = submitNotificationUseCase.submit(request);
        Notification second = submitNotificationUseCase.submit(request);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(notificationJpaRepository.count()).isEqualTo(1);
    }

    private NotificationRequest buildRequest() {
        return NotificationRequest.builder()
                .recipientId(1L)
                .notificationType("ENROLLMENT_COMPLETE")
                .payload("{\"lectureId\":\"123\"}")
                .channel(NotificationChannel.EMAIL)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
