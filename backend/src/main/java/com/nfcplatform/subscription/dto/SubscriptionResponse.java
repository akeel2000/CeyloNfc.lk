package com.nfcplatform.subscription.dto;

import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.subscription.entity.Subscription;

import java.time.Instant;

public record SubscriptionResponse(
        String uuid,
        String clientUuid,
        String clientDisplayName,
        String status,
        Instant startDate,
        Instant endDate,
        Instant renewalDate,
        String notes,
        PackagePlanResponse plan,
        int cardsUsed
) {
    public static SubscriptionResponse from(Subscription sub, String clientUuid, String clientDisplayName,
                                             PackagePlanResponse plan, int cardsUsed) {
        return new SubscriptionResponse(sub.getUuid(), clientUuid, clientDisplayName, sub.getStatus().name(),
                sub.getStartDate(), sub.getEndDate(), sub.getRenewalDate(), sub.getNotes(), plan, cardsUsed);
    }
}
