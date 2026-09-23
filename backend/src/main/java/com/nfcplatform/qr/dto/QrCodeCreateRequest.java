package com.nfcplatform.qr.dto;

import jakarta.validation.constraints.NotBlank;

public record QrCodeCreateRequest(
        @NotBlank String name,
        @NotBlank String destinationType,
        String externalUrl,
        String googleReviewLocationUuid
) {
}
