package com.jung.notificationservice.application.processor;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.outputport.NotificationSendOutputPort;
import com.jung.notificationservice.application.usecase.ProcessNotificationUseCase;
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
import static org.mockito.BDDMockito.willThrow;

@SpringBootTest
class NotificationProcessorTest {

    @Autowired
    private ProcessNotificationUseCase processNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private NotificationOutputPort notificationOutputPort;

    @MockitoBean
    private NotificationSendOutputPort notificationSendOutputPort;

    @AfterEach
    void tearDown() {
        notificationJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("발송 성공 시 COMPLETED 상태로 전이된다")
    void processOne_success_marksCompleted() {
        notificationJpaRepository.save(
                Notification.create("key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        willDoNothing().given(notificationSendOutputPort).send(any());

        processNotificationUseCase.processOne("key-001");

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-001").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
        assertThat(found.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("발송 실패 시 RETRYING 상태로 전이되고 에러 메시지가 기록된다 — 예외를 단순 무시하지 않음")
    void processOne_failure_marksRetryingWithErrorMessage() {
        notificationJpaRepository.save(
                Notification.create("key-002", 1L, "evt-002", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        willThrow(new RuntimeException("이메일 서버 연결 실패"))
                .given(notificationSendOutputPort).send(any());

        processNotificationUseCase.processOne("key-002");

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-002").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(found.getRetryCount()).isEqualTo(1);
        assertThat(found.getLastErrorMessage()).isEqualTo("이메일 서버 연결 실패");
        assertThat(found.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("최대 재시도 초과 시 FAILED 상태로 전이된다")
    void processOne_exceedsMaxRetry_marksFailed() {
        Notification notification = Notification.create("key-003", 1L, "evt-003",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        // retryCount=3, status=RETRYING → CAS UPDATE 대상
        for (int i = 0; i < 3; i++) {
            notification.markRetryingOrFailed("이전 실패");
        }
        notificationJpaRepository.save(notification);
        willThrow(new RuntimeException("이메일 서버 연결 실패"))
                .given(notificationSendOutputPort).send(any());

        processNotificationUseCase.processOne("key-003");

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-003").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(found.getRetryCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("이미 PROCESSING 상태인 알림은 중복 처리하지 않는다")
    void processOne_alreadyProcessing_skips() {
        Notification saved = notificationJpaRepository.save(
                Notification.create("key-004", 1L, "evt-004", "ENROLLMENT_COMPLETE",
                        "{}", NotificationChannel.EMAIL, null));
        notificationOutputPort.markAsProcessing(saved.getId(), LocalDateTime.now());

        processNotificationUseCase.processOne("key-004");

        Notification found = notificationJpaRepository.findByIdempotencyKey("key-004").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
    }
}
