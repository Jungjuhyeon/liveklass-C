package com.jung.notificationservice.application.usecase;

import com.jung.notificationservice.domain.Notification;

import java.util.List;

public interface InquiryNotificationUseCase {

    Notification findById(Long id);

    List<Notification> findByRecipientId(Long recipientId, Boolean isRead);
}
