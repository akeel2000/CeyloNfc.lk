package com.nfcplatform.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TemplateRequest(
        @NotBlank String name,
        String description,
        String previewImage,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex color like #4338ca") String primaryColor,
        @NotBlank String layout,
        Boolean premium,
        Boolean active,
        Integer sortOrder
) {
}
