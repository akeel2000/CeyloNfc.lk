package com.nfcplatform.auth.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.dto.ChangePasswordRequest;
import com.nfcplatform.auth.dto.ForgotPasswordRequest;
import com.nfcplatform.auth.dto.LoginRequest;
import com.nfcplatform.auth.dto.ResetPasswordRequest;
import com.nfcplatform.auth.dto.UserMeResponse;
import com.nfcplatform.auth.entity.PasswordResetToken;
import com.nfcplatform.auth.entity.RefreshToken;
import com.nfcplatform.auth.repository.PasswordResetTokenRepository;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.common.exception.AccountLockedException;
import com.nfcplatform.common.exception.InvalidCredentialsException;
import com.nfcplatform.common.exception.UnauthorizedException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.CookieUtil;
import com.nfcplatform.security.PrincipalFactory;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.security.jwt.JwtService;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the authentication core - account lockout, refresh-token rotation with
 * reuse detection, and the password reset / change flows. See docs/SECURITY.md for the
 * documented lockout and reuse-detection design these tests verify against. Pure Mockito, no
 * Spring context/DB.
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private PrincipalFactory principalFactory;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;

    private AuthService authService;

    private static final long USER_ID = 7L;
    private static final String EMAIL = "user@test.local";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        authService = new AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository,
                passwordEncoder, jwtService, cookieUtil, appProperties, auditService, emailService, principalFactory);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateOpaqueToken()).thenReturn("opaque-token");
        when(jwtService.newSessionId()).thenReturn("session-id");
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
    }

    // --- login ------------------------------------------------------------------------------

    @Test
    void loginRejectsAnUnknownEmailWithTheSameMessageAsAWrongPassword() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "whatever"),
                httpServletRequest, httpServletResponse))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginRejectsALockedAccountBeforeEvenCheckingThePassword() {
        User user = activeUser();
        user.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "whatever"),
                httpServletRequest, httpServletResponse))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginRejectsANonActiveAccountWithTheGenericMessageRatherThanLeakingItsStatus() {
        User user = activeUser();
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "whatever"),
                httpServletRequest, httpServletResponse))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginIncrementsFailedAttemptsOnAWrongPasswordWithoutLockingBelowTheThreshold() {
        User user = activeUser();
        user.setFailedLoginAttempts(1);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong"),
                httpServletRequest, httpServletResponse))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void loginLocksTheAccountOnceTheFailedAttemptThresholdIsReached() {
        User user = activeUser();
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong"),
                httpServletRequest, httpServletResponse))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void successfulLoginResetsFailedAttemptsAndClearsAnyLock() {
        User user = activeUser();
        user.setFailedLoginAttempts(3);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(principalFactory.build(user)).thenReturn(new UserPrincipal(user));

        authService.login(new LoginRequest(EMAIL, "correct"), httpServletRequest, httpServletResponse);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    // --- refresh: rotation and reuse detection -----------------------------------------------

    @Test
    void refreshRejectsAMissingCookieWithoutTouchingTheDatabase() {
        when(cookieUtil.readCookie(httpServletRequest, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(httpServletRequest, httpServletResponse))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void refreshingAnAlreadyRevokedTokenRevokesEverySessionForThatUserNotJustTheOne() {
        RefreshToken revoked = refreshToken();
        revoked.setRevokedAt(Instant.now().minusSeconds(60));
        RefreshToken anotherActiveSession = new RefreshToken();
        when(cookieUtil.readCookie(httpServletRequest, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn("stolen-token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(anotherActiveSession));

        assertThatThrownBy(() -> authService.refresh(httpServletRequest, httpServletResponse))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("already been used");

        assertThat(anotherActiveSession.getRevokedAt()).isNotNull();
    }

    @Test
    void refreshingAnExpiredButNeverRevokedTokenDoesNotTriggerFullSessionRevocation() {
        RefreshToken expired = refreshToken();
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(cookieUtil.readCookie(httpServletRequest, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn("expired-token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh(httpServletRequest, httpServletResponse))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
    }

    @Test
    void refreshRejectsATokenBelongingToANoLongerActiveUser() {
        RefreshToken token = refreshToken();
        User inactiveUser = activeUser();
        inactiveUser.setStatus(UserStatus.DISABLED);
        when(cookieUtil.readCookie(httpServletRequest, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn("token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.refresh(httpServletRequest, httpServletResponse))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void successfulRefreshRotatesTheTokenRatherThanReusingIt() {
        RefreshToken token = refreshToken();
        User user = activeUser();
        when(cookieUtil.readCookie(httpServletRequest, CookieUtil.REFRESH_TOKEN_COOKIE)).thenReturn("token");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(principalFactory.build(user)).thenReturn(new UserPrincipal(user));

        authService.refresh(httpServletRequest, httpServletResponse);

        assertThat(token.getRevokedAt()).isNotNull();
        assertThat(token.getReplacedByTokenHash()).isNotNull();
    }

    // --- forgotPassword: anti-enumeration -----------------------------------------------------

    @Test
    void forgotPasswordSilentlyNoOpsForAnUnknownEmailRatherThanRevealingItDoesntExist() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.empty());

        assertThatCode(() -> authService.forgotPassword(new ForgotPasswordRequest(EMAIL), httpServletRequest))
                .doesNotThrowAnyException();

        verify(emailService, never()).sendPasswordResetEmail(any(), any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void forgotPasswordCreatesATokenAndEmailsItForAKnownAccount() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateOpaqueToken()).thenReturn("reset-token");

        authService.forgotPassword(new ForgotPasswordRequest(EMAIL), httpServletRequest);

        verify(emailService).sendPasswordResetEmail(EMAIL, "https://ceylonfc.com/reset-password?token=reset-token");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    // --- resetPassword / changePassword: both revoke every existing session ------------------

    @Test
    void resetPasswordRejectsAnExpiredOrAlreadyUsedToken() {
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setUsedAt(Instant.now());
        usedToken.setExpiresAt(Instant.now().plusSeconds(600));
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("token", "NewPass1!")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void resetPasswordRevokesEveryActiveSessionSoAStolenSessionCantSurviveAPasswordReset() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(USER_ID);
        resetToken.setExpiresAt(Instant.now().plusSeconds(600));
        User user = activeUser();
        RefreshToken activeSession = new RefreshToken();
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(activeSession));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        authService.resetPassword(new ResetPasswordRequest("token", "NewPass1!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(activeSession.getRevokedAt()).isNotNull();
    }

    @Test
    void changePasswordRejectsTheWrongCurrentPassword() {
        User user = activeUser();
        UserPrincipal principal = new UserPrincipal(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(principal, new ChangePasswordRequest("wrong", "NewPass1!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePasswordRevokesEveryActiveSession() {
        User user = activeUser();
        UserPrincipal principal = new UserPrincipal(user);
        RefreshToken activeSession = new RefreshToken();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(USER_ID)).thenReturn(List.of(activeSession));

        authService.changePassword(principal, new ChangePasswordRequest("correct", "NewPass1!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(activeSession.getRevokedAt()).isNotNull();
    }

    private User activeUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setUuid("user-uuid");
        user.setEmail(EMAIL);
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setCode(RoleCode.CLIENT);
        role.setName(RoleCode.CLIENT.name());
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private RefreshToken refreshToken() {
        RefreshToken token = new RefreshToken();
        token.setUserId(USER_ID);
        token.setExpiresAt(Instant.now().plusSeconds(600));
        return token;
    }
}
