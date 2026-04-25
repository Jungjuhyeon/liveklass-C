package com.jung.notificationservice.framework.listener;

import com.jung.notificationservice.application.usecase.SaveNotificationUseCase;
import com.jung.notificationservice.application.outputport.NotificationMessageOutputPort;
import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.common.util.IdempotencyKeyGenerator;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import com.jung.notificationservice.domain.event.NotificationDomainEvent;
import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDomainEventListenerTest {

    @Mock
    private NotificationOutputPort repositoryPort;

    @Mock
    private NotificationMessageOutputPort messagePort;

    @Mock
    private SaveNotificationUseCase saveNotificationUseCase;

    @InjectMocks
    private NotificationDomainEventListener listener;

    @Test
    @DisplayName("BEFORE_COMMIT — 도메인 이벤트 수신 시 persistenceService를 통해 저장한다")
    void saveNotification_delegatesToPersistenceService() {
        NotificationDomainEvent event = createEvent();

        listener.saveNotification(event);

        verify(saveNotificationUseCase).saveIfAbsent(any(Notification.class));
    }

    @Test
    @DisplayName("AFTER_COMMIT — 저장된 알림을 메시지 포트로 발행한다")
    void dispatchDomainNotification_publishesEvent() {
        NotificationDomainEvent event = createEvent();
        String key = IdempotencyKeyGenerator.generate(
                "evt-001", 1L, "ENROLLMENT_COMPLETE", NotificationChannel.EMAIL);
        Notification notification = Notification.create(
                key, 1L, "evt-001", "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        given(repositoryPort.findByIdempotencyKey(key)).willReturn(Optional.of(notification));

        listener.dispatchDomainNotification(event);

        verify(messagePort).publish(any(NotificationRegisteredEvent.class));
    }

    @Test
    @DisplayName("AFTER_COMMIT — 알림이 없으면 발행하지 않는다")
    void dispatchDomainNotification_notFound_doesNotPublish() {
        NotificationDomainEvent event = createEvent();
        String key = IdempotencyKeyGenerator.generate(
                "evt-001", 1L, "ENROLLMENT_COMPLETE", NotificationChannel.EMAIL);
        given(repositoryPort.findByIdempotencyKey(key)).willReturn(Optional.empty());

        listener.dispatchDomainNotification(event);

        verify(messagePort, org.mockito.Mockito.never()).publish(any());
    }

    private NotificationDomainEvent createEvent() {
        return NotificationDomainEvent.create(
                1L, "evt-001", "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
    }
}
