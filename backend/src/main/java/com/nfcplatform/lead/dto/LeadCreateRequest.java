package com.nfcplatform.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LeadCreateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String company,
        String message,
        String source
) {
}
