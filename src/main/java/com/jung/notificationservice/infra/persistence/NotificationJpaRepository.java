package com.jung.notificationservice.infra.persistence;

import com.jung.notificationservice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'PROCESSING', n.processingStartedAt = :now, n.updatedAt = :now WHERE n.id = :id AND n.status IN ('PENDING', 'RETRYING')")
    int markAsProcessing(@Param("id") Long id, @Param("now") LocalDateTime now);
}
