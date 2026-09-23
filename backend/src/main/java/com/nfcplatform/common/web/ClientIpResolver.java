package com.nfcplatform.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Single source of truth for resolving the caller's IP behind a reverse proxy (trusts
 * X-Forwarded-For's first hop). Was duplicated identically across AuthService,
 * ClientService, NfcCardService and both public redirect controllers before being
 * extracted here - kept as one static method rather than a bean since it is pure and
 * stateless.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
