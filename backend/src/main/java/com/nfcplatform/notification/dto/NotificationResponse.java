package com.nfcplatform.notification.dto;

import com.nfcplatform.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        String uuid,
        String type,
        String title,
        String body,
        String link,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getUuid(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getLink(), notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}
