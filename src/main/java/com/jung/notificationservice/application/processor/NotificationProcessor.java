package com.jung.notificationservice.application.processor;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationOutputPort notificationOutputPort;

    @Transactional
    public Optional<Notification> processOne(Long notificationId) {
        int updated = notificationOutputPort.markAsProcessing(notificationId, LocalDateTime.now());
        if (updated == 0) {
            log.info("[Processor] 이미 처리 중인 알림 skip - notificationId={}", notificationId);
            return Optional.empty();
        }
        return notificationOutputPort.findById(notificationId);
    }
}
