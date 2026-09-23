package com.nfcplatform.support.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageCreateRequest(
        @NotBlank String body,
        String attachmentUrl
) {
}
