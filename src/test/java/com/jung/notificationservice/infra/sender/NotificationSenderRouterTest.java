package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationSenderRouterTest {

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender inAppSender;

    @Test
    @DisplayName("EMAIL 채널 알림은 emailSender로 라우팅된다")
    void send_emailChannel_routesToEmailSender() {
        given(emailSender.supportedChannel()).willReturn(NotificationChannel.EMAIL);
        NotificationSenderRouter router = new NotificationSenderRouter(List.of(emailSender, inAppSender));

        Notification notification = Notification.create(1L, "ENROLLMENT_COMPLETE",
                NotificationChannel.EMAIL, "{}", "key-001", LocalDateTime.now());

        router.send(notification);

        verify(emailSender).send(notification);
    }

    @Test
    @DisplayName("IN_APP 채널 알림은 inAppSender로 라우팅된다")
    void send_inAppChannel_routesToInAppSender() {
        given(emailSender.supportedChannel()).willReturn(NotificationChannel.EMAIL);
        given(inAppSender.supportedChannel()).willReturn(NotificationChannel.IN_APP);
        NotificationSenderRouter router = new NotificationSenderRouter(List.of(emailSender, inAppSender));

        Notification notification = Notification.create(1L, "ENROLLMENT_COMPLETE",
                NotificationChannel.IN_APP, "{}", "key-002", LocalDateTime.now());

        router.send(notification);

        verify(inAppSender).send(notification);
    }

    @Test
    @DisplayName("지원하지 않는 채널이면 BusinessException을 던진다")
    void send_unsupportedChannel_throwsBusinessException() {
        given(emailSender.supportedChannel()).willReturn(NotificationChannel.EMAIL);
        given(inAppSender.supportedChannel()).willReturn(NotificationChannel.EMAIL);
        NotificationSenderRouter router = new NotificationSenderRouter(List.of(emailSender, inAppSender));

        Notification notification = Notification.create(1L, "ENROLLMENT_COMPLETE",
                NotificationChannel.IN_APP, "{}", "key-003", LocalDateTime.now());

        assertThatThrownBy(() -> router.send(notification))
                .isInstanceOf(BusinessException.class);
    }
}
