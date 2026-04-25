package com.jung.notificationservice.application.inputport;

import com.jung.notificationservice.application.outputport.NotificationOutputPort;
import com.jung.notificationservice.application.usecase.InquiryNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryNotificationInputPort implements InquiryNotificationUseCase {

    private final NotificationOutputPort notificationOutputPort;

    @Override
    public Notification findById(Long id) {
        return notificationOutputPort.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Override
    public List<Notification> findByRecipientId(Long recipientId, Boolean isRead) {
        return notificationOutputPort.findByRecipientId(recipientId, isRead);
    }
}
