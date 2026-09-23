package com.nfcplatform.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientUpdateRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email,
        String phone
) {
}
