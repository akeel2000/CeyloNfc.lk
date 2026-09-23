package com.nfcplatform.profile.dto;

import com.nfcplatform.profile.entity.SocialLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLinkDto(
        @NotNull String platform,
        @NotBlank String url,
        int displayOrder,
        boolean enabled
) {
    public static SocialLinkDto from(SocialLink link) {
        return new SocialLinkDto(link.getPlatform().name(), link.getUrl(), link.getDisplayOrder(), link.isEnabled());
    }
}
