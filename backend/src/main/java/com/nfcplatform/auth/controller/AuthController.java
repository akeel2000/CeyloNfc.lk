package com.nfcplatform.auth.controller;

import com.nfcplatform.auth.dto.*;
import com.nfcplatform.auth.service.AuthService;
import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.common.web.ClientIpResolver;
import com.nfcplatform.security.RateLimiterService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * IP-based rate limiting here is deliberately separate from the per-account lockout in
 * AuthService (failed_login_attempts/locked_until) - lockout stops brute-forcing a single
 * known account, this stops one IP from spraying attempts across many accounts (credential
 * stuffing) or spamming password-reset emails.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/login")
    public ApiResponse<UserMeResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        requireWithinLimit("auth-login", httpRequest, 10);
        return ApiResponse.ok(authService.login(request, httpRequest, httpResponse), "Login successful");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ApiResponse.ok(null, "Logged out");
    }

    @PostMapping("/refresh")
    public ApiResponse<UserMeResponse> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ApiResponse.ok(authService.refresh(httpRequest, httpResponse), "Session refreshed");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                             HttpServletRequest httpRequest) {
        requireWithinLimit("auth-forgot-password", httpRequest, 5);
        authService.forgotPassword(request, httpRequest);
        return ApiResponse.ok(null, "If that email exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                            HttpServletRequest httpRequest) {
        requireWithinLimit("auth-reset-password", httpRequest, 5);
        authService.resetPassword(request);
        return ApiResponse.ok(null, "Password has been reset");
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return ApiResponse.ok(null, "Password changed");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserMeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.me(principal));
    }

    private void requireWithinLimit(String routeKey, HttpServletRequest request, int capacityPerMinute) {
        String rateKey = routeKey + ":" + ClientIpResolver.resolve(request);
        if (!rateLimiterService.tryConsume(rateKey, capacityPerMinute, RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }
    }
}
