package com.jung.notificationservice.infra.persistence;

import com.jung.notificationservice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.status = 'PROCESSING', n.processingStartedAt = :now, n.updatedAt = :now " +
            "WHERE n.id = :id " +
            "AND n.status IN ('PENDING', 'RETRYING')")
    int markAsProcessing(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("SELECT n.idempotencyKey " +
            "FROM Notification n " +
            "WHERE n.status IN ('PENDING', 'RETRYING') " +
            "AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
            "AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now) " +
            "ORDER BY n.createdAt ASC")
    List<String> findPendingKeys(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.status = 'PENDING', n.processingStartedAt = null, n.updatedAt = :now " +
            "WHERE n.status = 'PROCESSING' " +
            "AND n.processingStartedAt < :threshold")
    int recoverStuckProcessing(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    List<Notification> findByRecipientId(Long recipientId);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, boolean isRead);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n " +
            "SET n.isRead = true, n.updatedAt = :now " +
            "WHERE n.id = :id " +
            "AND n.isRead = false")
    int markAsRead(@Param("id") Long id, @Param("now") LocalDateTime now);
}
