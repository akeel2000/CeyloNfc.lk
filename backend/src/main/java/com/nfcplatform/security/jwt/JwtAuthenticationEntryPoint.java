package com.nfcplatform.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcplatform.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this, Spring Security's default entry point returns an empty 403 for any
 * unauthenticated request, which the frontend's apiClient can't distinguish from a real
 * authorization failure - it only retries via /auth/refresh on 401. That silently logged
 * users out on their next request after the 15-minute access token expired, even with a
 * valid 30-day refresh token, instead of transparently refreshing.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error("UNAUTHORIZED", "Authentication required"));
    }
}
