package com.nfcplatform.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record MenuCategoryRequest(
        @NotBlank String name,
        Integer sortOrder,
        Boolean active
) {
}
