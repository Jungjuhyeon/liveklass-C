package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.util.IdempotencyKeyGenerator;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
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
    @DisplayName("알림 발송 요청 시 PENDING 상태로 DB에 저장된다")
    void submit_savesWithPendingStatus() {
        NotificationRequest request = buildRequest();

        submitNotificationUseCase.submit(request);

        String key = IdempotencyKeyGenerator.generate(
                request.getEventId(), request.getRecipientId(),
                request.getNotificationType(), request.getChannel()
        );
        Notification saved = notificationJpaRepository.findByIdempotencyKey(key).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getRecipientId()).isEqualTo(1L);
        assertThat(saved.getNotificationType()).isEqualTo("ENROLLMENT_COMPLETE");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("동일한 요청이 중복으로 들어오면 알림이 하나만 저장된다")
    void submit_ignoreDuplicateRequest() {
        NotificationRequest request = buildRequest();

        submitNotificationUseCase.submit(request);
        submitNotificationUseCase.submit(request);

        assertThat(notificationJpaRepository.count()).isEqualTo(1);
    }

    private NotificationRequest buildRequest() {
        return NotificationRequest.builder()
                .eventId("evt-001")
                .recipientId(1L)
                .notificationType("ENROLLMENT_COMPLETE")
                .referenceData("{\"lectureId\":\"123\"}")
                .channel(NotificationChannel.EMAIL)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();
    }
}
