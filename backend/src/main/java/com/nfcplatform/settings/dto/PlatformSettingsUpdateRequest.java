package com.nfcplatform.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformSettingsUpdateRequest(
        @NotBlank @Size(max = 100) String siteName,
        @NotBlank @Email String supportEmail,
        @Size(max = 255) String tagline
) {
}
