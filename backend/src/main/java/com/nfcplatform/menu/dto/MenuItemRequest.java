package com.nfcplatform.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank String name,
        String description,
        String image,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        Boolean available,
        Boolean featured,
        Integer sortOrder
) {
}
