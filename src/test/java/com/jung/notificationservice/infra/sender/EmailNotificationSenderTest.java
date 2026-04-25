package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.application.outputport.NotificationTemplateOutputPort;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.NotificationTemplate;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private NotificationTemplateOutputPort templateOutputPort;

    @InjectMocks
    private EmailNotificationSender emailNotificationSender;

    @Test
    @DisplayName("템플릿이 있으면 변수 치환 후 발송된다")
    void send_withTemplate_rendersAndSends() {
        Notification notification = Notification.create(
                "key-001", 1L, "evt-001", "ENROLLMENT_COMPLETE",
                "{\"lectureName\":\"스프링 부트\",\"userName\":\"홍길동\"}",
                NotificationChannel.EMAIL, null
        );
        NotificationTemplate template = NotificationTemplate.create(
                "ENROLLMENT_COMPLETE", NotificationChannel.EMAIL,
                "{lectureName} 수강 신청 완료",
                "안녕하세요 {userName}님, {lectureName} 강의가 신청되었습니다."
        );
        given(templateOutputPort.findByTypeAndChannel("ENROLLMENT_COMPLETE", NotificationChannel.EMAIL))
                .willReturn(Optional.of(template));

        emailNotificationSender.send(notification);

        verify(templateOutputPort).findByTypeAndChannel("ENROLLMENT_COMPLETE", NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("템플릿이 없으면 경고 로그만 남기고 발송하지 않는다")
    void send_withoutTemplate_logsWarning() {
        Notification notification = Notification.create(
                "key-002", 1L, "evt-002", "UNKNOWN_TYPE",
                null, NotificationChannel.EMAIL, null
        );
        given(templateOutputPort.findByTypeAndChannel("UNKNOWN_TYPE", NotificationChannel.EMAIL))
                .willReturn(Optional.empty());

        emailNotificationSender.send(notification);

        verify(templateOutputPort).findByTypeAndChannel("UNKNOWN_TYPE", NotificationChannel.EMAIL);
    }
}
