package com.nfcplatform.support.dto;

import java.time.Instant;

public record SupportMessageDto(
        String uuid,
        String senderEmail,
        boolean fromSupportStaff,
        String body,
        String attachmentUrl,
        Instant createdAt
) {
}
