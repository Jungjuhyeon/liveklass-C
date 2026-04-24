package com.jung.notificationservice.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(500, "COMMON_500", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(400, "COMMON_400", "잘못된 요청입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_404", "알림을 찾을 수 없습니다."),
    NOTIFICATION_DUPLICATE(409, "NOTIFICATION_409", "이미 접수된 알림 요청입니다.");

    private final Integer httpStatus;
    private final String code;
    private final String message;
}
