package com.nfcplatform.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProfileUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug may only contain lowercase letters, numbers and hyphens")
        String slug,

        String fullName,
        String jobTitle,

        String companyName,
        String industry,
        String registrationNumber,
        String googleMapsUrl,
        String logo,

        String bio,
        String profileImage,
        String coverImage,
        String phone,
        String whatsapp,
        String email,
        String website,
        String address,
        String city,
        String country
) {
}
