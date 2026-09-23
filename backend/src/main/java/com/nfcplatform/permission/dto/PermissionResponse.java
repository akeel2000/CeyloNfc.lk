package com.nfcplatform.permission.dto;

public record PermissionResponse(
        String code,
        String description,
        boolean granted
) {
}
