package com.jung.notificationservice.framework.web;

import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.response.ApiResponse;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.framework.web.request.NotificationRequest;
import com.jung.notificationservice.framework.web.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SubmitNotificationUseCase submitNotificationUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> submit(@RequestBody NotificationRequest request) {
        Notification notification = submitNotificationUseCase.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(NotificationResponse.mapToDTO(notification)));
    }
}
