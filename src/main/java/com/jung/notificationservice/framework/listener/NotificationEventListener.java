package com.jung.notificationservice.framework.listener;

import com.jung.notificationservice.application.outputport.NotificationMessageOutputPort;
import com.jung.notificationservice.domain.event.NotificationRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationMessageOutputPort messagePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchNotificationRegistered(NotificationRegisteredEvent event) {
        messagePort.publish(event.notificationId());
    }
}
