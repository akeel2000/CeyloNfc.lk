package com.nfcplatform.packageplan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PackagePlanRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @NotBlank String billingPeriod,
        Integer cardLimit,
        Integer profileLimit,
        Integer reviewLocationLimit,
        Integer menuLimit,
        Boolean premiumTemplates,
        Boolean active,
        Integer sortOrder
) {
}
