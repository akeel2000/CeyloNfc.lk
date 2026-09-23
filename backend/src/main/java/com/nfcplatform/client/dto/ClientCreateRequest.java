package com.nfcplatform.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClientCreateRequest(
        @NotNull String type,
        @NotBlank String displayName,
        @NotBlank @Email String email,
        String phone
) {
}
