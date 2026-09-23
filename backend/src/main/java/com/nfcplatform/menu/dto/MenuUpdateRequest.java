package com.nfcplatform.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MenuUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug may only contain lowercase letters, numbers and hyphens")
        String slug,

        @NotBlank String name,
        String description,
        String logo,
        String currency
) {
}
