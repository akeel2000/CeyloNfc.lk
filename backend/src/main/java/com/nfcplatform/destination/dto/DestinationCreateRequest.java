package com.nfcplatform.destination.dto;

import jakarta.validation.constraints.NotBlank;

public record DestinationCreateRequest(
        @NotBlank String clientUuid,
        @NotBlank String name,
        @NotBlank String type,
        String externalUrl,
        String googleReviewLocationUuid
) {
}
