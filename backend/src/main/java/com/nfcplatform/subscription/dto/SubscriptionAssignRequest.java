package com.nfcplatform.subscription.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record SubscriptionAssignRequest(
        @NotBlank String packagePlanUuid,
        String status,
        Instant endDate,
        Instant renewalDate,
        String notes
) {
}
