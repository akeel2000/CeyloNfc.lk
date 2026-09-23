package com.nfcplatform.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        @NotBlank String clientUuid,
        @NotEmpty @Valid List<OrderLineRequest> items,
        String notes
) {
    public record OrderLineRequest(
            @NotBlank String productUuid,
            int quantity
    ) {
    }
}
