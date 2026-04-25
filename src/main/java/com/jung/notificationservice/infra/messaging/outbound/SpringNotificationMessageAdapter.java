package com.jung.notificationservice.infra.messaging.outbound;

import com.jung.notificationservice.application.outputport.NotificationMessageOutputPort;
import com.jung.notificationservice.application.usecase.ProcessNotificationUseCase;
import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationMessageAdapter implements NotificationMessageOutputPort {

    private final ProcessNotificationUseCase processNotificationUseCase;

    @Override
    @Async("notificationExecutor")
    public void publish(NotificationRegisteredEvent event) {
        processNotificationUseCase.processOne(event.getIdempotencyKey());
    }

}
