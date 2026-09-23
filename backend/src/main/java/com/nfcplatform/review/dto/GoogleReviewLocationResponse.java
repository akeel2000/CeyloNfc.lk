package com.nfcplatform.review.dto;

import com.nfcplatform.review.entity.GoogleReviewLocation;

public record GoogleReviewLocationResponse(
        String uuid,
        String businessName,
        String locationName,
        String address,
        String googleMapsUrl,
        String googleReviewUrl,
        String googlePlaceId,
        boolean active
) {
    public static GoogleReviewLocationResponse from(GoogleReviewLocation location) {
        return new GoogleReviewLocationResponse(
                location.getUuid(),
                location.getBusinessName(),
                location.getLocationName(),
                location.getAddress(),
                location.getGoogleMapsUrl(),
                location.getGoogleReviewUrl(),
                location.getGooglePlaceId(),
                location.isActive()
        );
    }
}
