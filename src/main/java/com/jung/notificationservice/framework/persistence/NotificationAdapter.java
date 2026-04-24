package com.jung.notificationservice.framework.persistence;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationOutputPort {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return notificationJpaRepository.findByIdempotencyKey(idempotencyKey);
    }
}
