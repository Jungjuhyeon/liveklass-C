package com.jung.notificationservice.framework.web;

import com.jung.notificationservice.application.usecase.InquiryNotificationUseCase;
import com.jung.notificationservice.application.usecase.ReadNotificationUseCase;
import com.jung.notificationservice.application.usecase.RetryNotificationUseCase;
import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private SubmitNotificationUseCase submitNotificationUseCase;
    @Mock
    private InquiryNotificationUseCase inquiryNotificationUseCase;
    @Mock
    private ReadNotificationUseCase readNotificationUseCase;
    @Mock
    private RetryNotificationUseCase retryNotificationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(
                        submitNotificationUseCase, inquiryNotificationUseCase,
                        readNotificationUseCase, retryNotificationUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("알림 발송 요청 시 202 Accepted를 반환한다")
    void submit_returns202() throws Exception {
        doNothing().when(submitNotificationUseCase).submit(any());

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("알림 단건 조회 시 200 OK를 반환한다")
    void getNotification_returns200() throws Exception {
        Notification notification = Notification.create("key-001", 1L, "evt-001",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        given(inquiryNotificationUseCase.findById(1L)).willReturn(notification);

        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("존재하지 않는 알림 조회 시 404를 반환한다")
    void getNotification_notFound_returns404() throws Exception {
        given(inquiryNotificationUseCase.findById(999L))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("수신자별 알림 목록 조회 시 200 OK를 반환한다")
    void getNotifications_returns200() throws Exception {
        Notification notification = Notification.create("key-001", 1L, "evt-001",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        given(inquiryNotificationUseCase.findByRecipientId(1L, null))
                .willReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").param("recipientId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("읽음 처리 시 200 OK를 반환한다")
    void markAsRead_returns200() throws Exception {
        doNothing().when(readNotificationUseCase).markAsRead(1L);

        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("수동 재시도 시 200 OK와 PENDING 상태를 반환한다")
    void retry_returns200() throws Exception {
        Notification notification = Notification.create("key-001", 1L, "evt-001",
                "ENROLLMENT_COMPLETE", "{}", NotificationChannel.EMAIL, null);
        given(retryNotificationUseCase.retry(1L)).willReturn(notification);

        mockMvc.perform(patch("/api/notifications/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("FAILED가 아닌 알림 재시도 시 400을 반환한다")
    void retry_notRetryable_returns400() throws Exception {
        given(retryNotificationUseCase.retry(1L))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_RETRYABLE));

        mockMvc.perform(patch("/api/notifications/1/retry"))
                .andExpect(status().isBadRequest());
    }

    private String buildRequestJson() {
        return """
                {
                    "eventId": "evt-001",
                    "recipientId": 1,
                    "notificationType": "ENROLLMENT_COMPLETE",
                    "referenceData": "{\\"lectureId\\":\\"123\\"}",
                    "channel": "EMAIL"
                }
                """;
    }
}
