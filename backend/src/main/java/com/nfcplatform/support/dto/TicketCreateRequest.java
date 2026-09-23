package com.nfcplatform.support.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketCreateRequest(
        @NotBlank String subject,
        @NotBlank String message,
        String priority,
        String attachmentUrl
) {
}
