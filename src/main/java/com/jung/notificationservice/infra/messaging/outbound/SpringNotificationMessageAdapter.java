package com.jung.notificationservice.infra.messaging.outbound;

import com.jung.notificationservice.application.processor.NotificationProcessor;
import com.jung.notificationservice.application.outputport.NotificationMessageOutputPort;
import com.jung.notificationservice.infra.sender.NotificationSenderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringNotificationMessageAdapter implements NotificationMessageOutputPort {

    private final NotificationProcessor notificationProcessor;
    private final NotificationSenderRouter senderRouter;

    @Override
    @Async("notificationExecutor")
    public void publish(Long notificationId) {
        notificationProcessor.processOne(notificationId);
    }
}
