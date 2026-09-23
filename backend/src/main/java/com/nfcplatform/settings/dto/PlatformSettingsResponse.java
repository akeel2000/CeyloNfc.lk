package com.nfcplatform.settings.dto;

import com.nfcplatform.settings.entity.PlatformSettings;

public record PlatformSettingsResponse(
        String siteName,
        String supportEmail,
        String tagline
) {
    public static PlatformSettingsResponse from(PlatformSettings settings) {
        return new PlatformSettingsResponse(settings.getSiteName(), settings.getSupportEmail(), settings.getTagline());
    }
}
