package com.nfcplatform.packageplan.dto;

import com.nfcplatform.packageplan.entity.PackagePlan;

import java.math.BigDecimal;

public record PackagePlanResponse(
        String uuid,
        String name,
        String description,
        BigDecimal price,
        String billingPeriod,
        Integer cardLimit,
        Integer profileLimit,
        Integer reviewLocationLimit,
        Integer menuLimit,
        boolean premiumTemplates,
        boolean active,
        int sortOrder
) {
    public static PackagePlanResponse from(PackagePlan plan) {
        return new PackagePlanResponse(plan.getUuid(), plan.getName(), plan.getDescription(), plan.getPrice(),
                plan.getBillingPeriod().name(), plan.getCardLimit(), plan.getProfileLimit(),
                plan.getReviewLocationLimit(), plan.getMenuLimit(), plan.isPremiumTemplates(), plan.isActive(),
                plan.getSortOrder());
    }
}
