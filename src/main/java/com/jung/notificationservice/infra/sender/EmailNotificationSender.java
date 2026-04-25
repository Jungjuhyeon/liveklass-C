package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.application.outputport.NotificationTemplateOutputPort;
import com.jung.notificationservice.common.util.ReferenceDataParser;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.NotificationTemplate;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final NotificationTemplateOutputPort templateOutputPort;

    @Override
    public void send(Notification notification) {
        templateOutputPort.findByTypeAndChannel(notification.getNotificationType(), NotificationChannel.EMAIL)
                .ifPresentOrElse(
                        template -> {
                            Map<String, String> variables = ReferenceDataParser.parse(notification.getReferenceData());
                            String title = template.resolveTitle(variables);
                            String body = template.resolveBody(variables);
                            log.info("[EMAIL] 알림 발송 - notificationId={}, recipientId={}, title={}",
                                    notification.getId(), notification.getRecipientId(), title);
                            // 실제 이메일 발송 로직
                        },
                        () -> log.warn("[EMAIL] 템플릿 없음 - type={}, notificationId={}",
                                notification.getNotificationType(), notification.getId())
                );
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }
}
