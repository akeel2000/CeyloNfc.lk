package com.nfcplatform.user.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
        @NotNull String status
) {
}
