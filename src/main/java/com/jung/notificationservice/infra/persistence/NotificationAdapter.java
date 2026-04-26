package com.jung.notificationservice.infra.persistence;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationOutputPort {

    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationJpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return notificationJpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional
    public int markAsProcessing(Long id, LocalDateTime now) {
        return notificationJpaRepository.markAsProcessing(id, now);
    }

    @Override
    public List<String> findPendingKeys(LocalDateTime now) {
        return notificationJpaRepository.findPendingKeys(now);
    }

    @Override
    public int recoverStuckProcessing(LocalDateTime threshold, LocalDateTime now) {
        return notificationJpaRepository.recoverStuckProcessing(threshold, now);
    }

    @Override
    public List<Notification> findByRecipientId(Long recipientId, Boolean isRead) {
        if (isRead == null) {
            return notificationJpaRepository.findByRecipientId(recipientId);
        }
        return notificationJpaRepository.findByRecipientIdAndIsRead(recipientId, isRead);
    }

    @Override
    public int markAsRead(Long id, LocalDateTime now) {
        return notificationJpaRepository.markAsRead(id, now);
    }

    @Override
    @Transactional
    public int upsert(Notification notification) {
        return notificationJpaRepository.upsert(notification);
    }
}
