package com.jung.notificationservice.framework.web;

import com.jung.notificationservice.application.usecase.InquiryNotificationUseCase;
import com.jung.notificationservice.application.usecase.ReadNotificationUseCase;
import com.jung.notificationservice.application.usecase.RetryNotificationUseCase;
import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.response.ApiResponse;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import com.jung.notificationservice.framework.web.response.NotificationDetailResponse;
import com.jung.notificationservice.framework.web.response.NotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SubmitNotificationUseCase submitNotificationUseCase;
    private final InquiryNotificationUseCase inquiryNotificationUseCase;
    private final ReadNotificationUseCase readNotificationUseCase;
    private final RetryNotificationUseCase retryNotificationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody NotificationRequest request) {
        submitNotificationUseCase.submit(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> getNotification(@PathVariable Long id) {
        Notification notification = inquiryNotificationUseCase.findById(id);
        return ResponseEntity.ok(ApiResponse.success(NotificationDetailResponse.mapToDTO(notification)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDetailResponse>>> getNotifications(
            @RequestParam Long recipientId,
            @RequestParam(required = false) Boolean isRead) {
        List<NotificationDetailResponse> result = inquiryNotificationUseCase.findByRecipientId(recipientId, isRead)
                .stream()
                .map(NotificationDetailResponse::mapToDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        readNotificationUseCase.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<NotificationResponse>> retry(@PathVariable Long id) {
        Notification notification = retryNotificationUseCase.retry(id);
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.mapToDTO(notification)));
    }
}
