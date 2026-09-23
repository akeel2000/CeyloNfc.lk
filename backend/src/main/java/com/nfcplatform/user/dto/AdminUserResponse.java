package com.nfcplatform.user.dto;

import com.nfcplatform.user.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record AdminUserResponse(
        String uuid,
        String email,
        String phone,
        String status,
        Set<String> roles,
        boolean mustChangePassword,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getUuid(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus().name(),
                user.getRoles().stream().map(role -> role.getCode().name()).collect(Collectors.toUnmodifiableSet()),
                user.isMustChangePassword(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
