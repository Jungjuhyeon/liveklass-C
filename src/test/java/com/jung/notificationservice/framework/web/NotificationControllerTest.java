package com.jung.notificationservice.framework.web;

import com.jung.notificationservice.application.usecase.SubmitNotificationUseCase;
import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.exception.GlobalExceptionHandler;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.Notification;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private SubmitNotificationUseCase submitNotificationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(submitNotificationUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("알림 저장 요청 시 202 Accepted를 반환한다")
    void submit_returns202() throws Exception {
        Notification notification = Notification.create(
                1L, "ENROLLMENT_COMPLETE", NotificationChannel.EMAIL,
                "{\"lectureId\":\"123\"}", "key-001", LocalDateTime.now().plusHours(1)
        );
        given(submitNotificationUseCase.submit(any())).willReturn(notification);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("중복 요청 시 409를 반환한다")
    void submit_returns409OnDuplicate() throws Exception {
        given(submitNotificationUseCase.submit(any()))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_DUPLICATE));

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("NOTIFICATION_409"));
    }

    private String buildRequestJson() {
        return """
                {
                    "recipientId": 1,
                    "notificationType": "ENROLLMENT_COMPLETE",
                    "payload": "{\\"lectureId\\":\\"123\\"}",
                    "channel": "EMAIL"
                }
                """;
    }
}
