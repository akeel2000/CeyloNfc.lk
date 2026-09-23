package com.nfcplatform.user.dto;

/**
 * Returned only once, immediately after creation - mirrors ClientCreateResponse.
 * temporaryPassword is never recoverable from the API again afterwards.
 */
public record AdminUserCreateResponse(
        AdminUserResponse user,
        String temporaryPassword
) {
}
