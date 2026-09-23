package com.nfcplatform.lead.dto;

import jakarta.validation.constraints.NotNull;

public record LeadConvertRequest(
        @NotNull String clientType
) {
}
