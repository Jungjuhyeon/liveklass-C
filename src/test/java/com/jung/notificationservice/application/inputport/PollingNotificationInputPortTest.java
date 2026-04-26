package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationSendOutputPort;
import com.jung.notificationservice.application.usecase.PollNotificationsUseCase;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.enumeration.NotificationStatus;
import com.jung.notificationservice.infra.persistence.NotificationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;

@SpringBootTest
class PollingNotificationInputPortTest {

    @Autowired
    private PollNotificationsUseCase pollNotificationsUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @MockitoBean
    private NotificationSendOutputPort notificationSendOutputPort;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("PENDING 상태의 알림을 폴링하여 처리한다")
    void pollPending_processesPendingNotifications() {
        notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        willDoNothing().given(notificationSendOutputPort).send(any());

        pollNotificationsUseCase.pollPending();

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-001").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
    }

    @Test
    @DisplayName("scheduledAt이 미래인 알림은 폴링 대상에서 제외된다")
    void pollPending_skipsScheduledInFuture() {
        notificationJpaRepository.save(
                Notification.create("key-002", 1L, "evt-002", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, LocalDateTime.now().plusHours(1)));

        pollNotificationsUseCase.pollPending();

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-002").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("PENDING 알림이 없으면 아무 처리도 하지 않는다")
    void pollPending_noPending_doesNothing() {
        pollNotificationsUseCase.pollPending();
        // 예외 없이 정상 종료
    }
}
