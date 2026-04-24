package com.jung.notificationservice.common.util;

import com.jung.notificationservice.common.exception.BusinessException;
import com.jung.notificationservice.common.response.ErrorCode;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {}

    public static String generate(String eventId, Long recipientId, String notificationType, NotificationChannel channel) {
        String raw = eventId + ":" + recipientId + ":" + notificationType + ":" + channel.name();
        return sha256(raw);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
