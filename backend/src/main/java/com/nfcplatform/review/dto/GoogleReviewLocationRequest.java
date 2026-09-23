package com.nfcplatform.review.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleReviewLocationRequest(
        @NotBlank String businessName,
        String locationName,
        String address,
        String googleMapsUrl,
        @NotBlank String googleReviewUrl,
        String googlePlaceId
) {
}
