package com.nfcplatform.profile.dto;

import java.util.List;

/**
 * Unified shape for both profile kinds - only the fields relevant to the client's
 * type (INDIVIDUAL vs BUSINESS) are populated. Kept flat rather than two separate DTOs
 * so the frontend can render a single "My Profile" screen driven by `type`.
 */
public record ProfileResponse(
        String type,
        String uuid,
        String slug,
        boolean published,
        String publicUrl,

        // Individual-only
        String fullName,
        String jobTitle,

        // Company-only
        String companyName,
        String industry,
        String registrationNumber,
        String googleMapsUrl,
        String logo,

        // Shared
        String bio,
        String profileImage,
        String coverImage,
        String phone,
        String whatsapp,
        String email,
        String website,
        String address,
        String city,
        String country,
        String templateUuid,

        List<SocialLinkDto> socialLinks,

        // Company-only - always empty for INDIVIDUAL
        List<BusinessHourDto> businessHours
) {
}
