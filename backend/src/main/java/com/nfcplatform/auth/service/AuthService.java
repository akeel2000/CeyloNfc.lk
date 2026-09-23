package com.nfcplatform.auth.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.dto.*;
import com.nfcplatform.auth.entity.PasswordResetToken;
import com.nfcplatform.auth.entity.RefreshToken;
import com.nfcplatform.auth.repository.PasswordResetTokenRepository;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.common.exception.AccountLockedException;
import com.nfcplatform.common.exception.InvalidCredentialsException;
import com.nfcplatform.common.exception.UnauthorizedException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.common.util.HashUtil;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.security.CookieUtil;
import com.nfcplatform.security.PrincipalFactory;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.security.jwt.JwtService;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long PASSWORD_RESET_TOKEN_TTL_MINUTES = 60;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PrincipalFactory principalFactory;

    @Transactional
    public UserMeResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is temporarily locked due to repeated failed login attempts");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = principalFactory.build(user);
        issueTokens(principal, httpRequest, httpResponse);

        auditService.record(user.getId(), "AUTH_LOGIN", "User", user.getUuid(), clientIp(httpRequest), null);

        return toMeResponse(user, principal);
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String refreshTokenValue = cookieUtil.readCookie(httpRequest, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue != null) {
            refreshTokenRepository.findByTokenHash(HashUtil.sha256Hex(refreshTokenValue))
                    .ifPresent(rt -> rt.setRevokedAt(Instant.now()));
        }
        cookieUtil.clearCookie(httpResponse, CookieUtil.ACCESS_TOKEN_COOKIE);
        cookieUtil.clearCookie(httpResponse, CookieUtil.REFRESH_TOKEN_COOKIE);
    }

    @Transactional
    public UserMeResponse refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String refreshTokenValue = cookieUtil.readCookie(httpRequest, CookieUtil.REFRESH_TOKEN_COOKIE);
        if (refreshTokenValue == null) {
            throw new UnauthorizedException("No refresh token present");
        }

        RefreshToken existing = refreshTokenRepository.findByTokenHash(HashUtil.sha256Hex(refreshTokenValue))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.getRevokedAt() != null) {
            // Reuse of a revoked/rotated token: treat as compromise, kill the whole session chain.
            revokeAllForUser(existing.getUserId());
            throw new UnauthorizedException("Refresh token has already been used");
        }
        if (!existing.isActive()) {
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = userRepository.findById(existing.getUserId())
                .filter(u -> u.getDeletedAt() == null && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException("Account no longer active"));

        UserPrincipal principal = principalFactory.build(user);
        String newRefreshValue = issueTokens(principal, httpRequest, httpResponse);

        existing.setRevokedAt(Instant.now());
        existing.setReplacedByTokenHash(HashUtil.sha256Hex(newRefreshValue));

        return toMeResponse(user, principal);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email()).ifPresent(user -> {
            String rawToken = jwtService.generateOpaqueToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setTokenHash(HashUtil.sha256Hex(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
            resetToken.setIpAddress(clientIp(httpRequest));
            passwordResetTokenRepository.save(resetToken);

            String resetUrl = appProperties.getFrontendUrl() + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

            auditService.record(user.getId(), "AUTH_FORGOT_PASSWORD_REQUESTED", "User", user.getUuid(), clientIp(httpRequest), null);
        });
        // Always return success regardless of whether the email exists, to avoid account enumeration.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(HashUtil.sha256Hex(request.token()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        revokeAllForUser(user.getId());

        auditService.record(user.getId(), "AUTH_PASSWORD_RESET", "User", user.getUuid(), null, null);
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Session no longer valid"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        revokeAllForUser(user.getId());

        auditService.record(user.getId(), "AUTH_PASSWORD_CHANGED", "User", user.getUuid(), null, null);
    }

    public UserMeResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Session no longer valid"));
        return toMeResponse(user, principal);
    }

    private String issueTokens(UserPrincipal principal, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String sessionId = jwtService.newSessionId();
        String accessToken = jwtService.generateAccessToken(principal, sessionId);
        String refreshTokenValue = jwtService.generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(principal.getId());
        refreshToken.setTokenHash(HashUtil.sha256Hex(refreshTokenValue));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()));
        refreshToken.setIpAddress(clientIp(httpRequest));
        refreshToken.setUserAgent(httpRequest.getHeader("User-Agent"));
        refreshTokenRepository.save(refreshToken);

        cookieUtil.addCookie(httpResponse, CookieUtil.ACCESS_TOKEN_COOKIE, accessToken,
                jwtService.getAccessTokenExpirationSeconds(), true);
        cookieUtil.addCookie(httpResponse, CookieUtil.REFRESH_TOKEN_COOKIE, refreshTokenValue,
                jwtService.getRefreshTokenExpirationSeconds(), true);

        return refreshTokenValue;
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= appProperties.getSecurity().getMaxFailedLoginAttempts()) {
            user.setLockedUntil(Instant.now().plus(appProperties.getSecurity().getAccountLockDurationMinutes(), ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    private void revokeAllForUser(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(rt -> rt.setRevokedAt(Instant.now()));
    }

    private UserMeResponse toMeResponse(User user, UserPrincipal principal) {
        Set<String> roles = principal.getRoleCodes();
        Set<String> permissions = principal.isSuperAdmin()
                ? Set.of("ALL")
                : principal.getPermissionCodes().stream().collect(Collectors.toUnmodifiableSet());
        return new UserMeResponse(user.getUuid(), user.getEmail(), user.getPhone(), roles, permissions, user.isMustChangePassword());
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
