package com.nfcplatform.profile.dto;

import java.util.List;

/** Public-facing projection - never includes uuid, client id, or unpublished data. */
public record PublicProfileResponse(
        String type,
        String slug,
        String fullName,
        String jobTitle,
        String companyName,
        String industry,
        String bio,
        String profileImage,
        String coverImage,
        String logo,
        String phone,
        String whatsapp,
        String email,
        String website,
        String address,
        String city,
        String country,
        String googleMapsUrl,
        String templatePrimaryColor,
        String templateLayout,
        List<SocialLinkDto> socialLinks,

        // Company-only - null for INDIVIDUAL
        List<BusinessHourDto> businessHours,
        Boolean openNow,
        String openStatusLabel
) {
}
