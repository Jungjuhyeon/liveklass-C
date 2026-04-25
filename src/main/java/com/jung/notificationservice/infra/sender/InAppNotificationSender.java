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
public class InAppNotificationSender implements NotificationSender {

    private final NotificationTemplateOutputPort templateOutputPort;

    @Override
    public void send(Notification notification) {
        templateOutputPort.findByTypeAndChannel(notification.getNotificationType(), NotificationChannel.IN_APP)
                .ifPresentOrElse(
                        template -> {
                            Map<String, String> variables = ReferenceDataParser.parse(notification.getReferenceData());
                            String title = template.resolveTitle(variables);
                            String body = template.resolveBody(variables);
                            log.info("[IN_APP] 알림 발송 - notificationId={}, recipientId={}, title={}",
                                    notification.getId(), notification.getRecipientId(), title);
                            // 실제 인앱 발송 로직
                        },
                        () -> log.warn("[IN_APP] 템플릿 없음 - type={}, notificationId={}",
                                notification.getNotificationType(), notification.getId())
                );
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.IN_APP;
    }
}
