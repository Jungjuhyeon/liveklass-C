package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.domain.event.NotificationDomainEvent;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmitNotificationInputPort implements SubmitNotificationUseCase {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void submit(NotificationRequest request) {
        eventPublisher.publishEvent(
                NotificationDomainEvent.create(
                        request.getRecipientId(),
                        request.getEventId(),
                        request.getNotificationType(),
                        request.getReferenceData(),
                        request.getChannel(),
                        request.getScheduledAt()
                )
        );
    }
}
