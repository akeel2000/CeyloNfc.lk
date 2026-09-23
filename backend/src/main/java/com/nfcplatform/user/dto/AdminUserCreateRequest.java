package com.nfcplatform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUserCreateRequest(
        @NotBlank @Email String email,
        String phone,
        @NotNull String role
) {
}
