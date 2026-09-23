package com.nfcplatform.lead.dto;

import jakarta.validation.constraints.NotBlank;

public record LeadUpdateRequest(
        @NotBlank String status,
        String notes
) {
}
