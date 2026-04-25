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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final NotificationTemplateOutputPort templateOutputPort;

    @Override
    public void send(Notification notification) {
        Map<String, String> variables = ReferenceDataParser.parse(notification.getReferenceData());
        String title;
        String body;

        Optional<NotificationTemplate> templateOpt = templateOutputPort.findByTypeAndChannel(
                notification.getNotificationType(), NotificationChannel.IN_APP);

        if (templateOpt.isPresent()) {
            NotificationTemplate template = templateOpt.get();
            title = template.resolveTitle(variables);
            body = template.resolveBody(variables);
        } else {
            title = notification.getNotificationType();
            body = "알림이 도착했습니다. (recipientId=" + notification.getRecipientId() + ")";
            log.warn("[IN_APP] 템플릿 없음, 기본 템플릿 사용 - type={}, notificationId={}",
                    notification.getNotificationType(), notification.getId());
        }

        log.info("[IN_APP] 알림 발송 - notificationId={}, recipientId={}, title={}",
                notification.getId(), notification.getRecipientId(), title);
        // 실제 인앱 발송 로직
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.IN_APP;
    }
}
