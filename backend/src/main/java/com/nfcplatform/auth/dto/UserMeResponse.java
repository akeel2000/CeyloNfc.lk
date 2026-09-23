package com.nfcplatform.auth.dto;

import java.util.Set;

public record UserMeResponse(
        String uuid,
        String email,
        String phone,
        Set<String> roles,
        Set<String> permissions,
        boolean mustChangePassword
) {
}
