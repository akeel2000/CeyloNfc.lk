package com.nfcplatform.client.dto;

import jakarta.validation.constraints.NotNull;

public record ClientStatusUpdateRequest(
        @NotNull String status
) {
}
