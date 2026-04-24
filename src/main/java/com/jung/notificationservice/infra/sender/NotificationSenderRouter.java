package com.jung.notificationservice.infra.sender;

import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationSenderRouter {

    private final List<NotificationSender> senders;

    public void send(Notification notification) {
        senders.stream()
                .filter(s -> s.supportedChannel() == notification.getChannel())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR))
                .send(notification);
    }
}
