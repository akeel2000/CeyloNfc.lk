package com.nfcplatform.user.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.entity.RefreshToken;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.permission.dto.PermissionOverridesUpdateRequest;
import com.nfcplatform.permission.dto.PermissionResponse;
import com.nfcplatform.permission.entity.Permission;
import com.nfcplatform.permission.repository.AdminPermissionOverrideRepository;
import com.nfcplatform.permission.repository.PermissionRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.dto.AdminUserCreateRequest;
import com.nfcplatform.user.dto.AdminUserCreateResponse;
import com.nfcplatform.user.dto.AdminUserStatusUpdateRequest;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for platform-staff account management - self-suspend protection, session
 * revocation on suspend/disable, and the SUPER_ADMIN-has-everything / permission-override
 * rules described in docs/ROLE_PERMISSION_MATRIX.md. Pure Mockito, no Spring context/DB.
 */
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AdminPermissionOverrideRepository overrideRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AdminUserService adminUserService;

    private static final long ACTOR_USER_ID = 1L;
    private static final String ACTOR_UUID = "actor-uuid";
    private static final long TARGET_USER_ID = 2L;
    private static final String TARGET_UUID = "target-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        adminUserService = new AdminUserService(userRepository, roleRepository, permissionRepository,
                overrideRepository, refreshTokenRepository, passwordEncoder, auditService, emailService,
                appProperties);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- createAdminUser ------------------------------------------------------------------

    @Test
    void createAdminUserRejectsADuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@test.local")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createAdminUser(
                new AdminUserCreateRequest("taken@test.local", null, "ADMIN"), principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createAdminUserRejectsAnUnrecognizedRoleString() {
        assertThatThrownBy(() -> adminUserService.createAdminUser(
                new AdminUserCreateRequest("new@test.local", null, "NOT_A_ROLE"), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAdminUserRejectsARoleThatIsNotStaff() {
        assertThatThrownBy(() -> adminUserService.createAdminUser(
                new AdminUserCreateRequest("new@test.local", null, "CLIENT"), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAdminUserForcesAPasswordChangeAndEmailsTheAccount() {
        Role adminRole = new Role();
        adminRole.setCode(RoleCode.ADMIN);
        when(userRepository.existsByEmailIgnoreCase("new@test.local")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        AdminUserCreateResponse response = adminUserService.createAdminUser(
                new AdminUserCreateRequest("new@test.local", null, "ADMIN"), principal(), httpServletRequest);

        assertThat(response.temporaryPassword()).isNotBlank();
        verify(emailService).sendAccountCreatedEmail(eq("new@test.local"), eq("https://ceylonfc.com/login"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().isMustChangePassword()).isTrue();
    }

    // --- updateStatus -----------------------------------------------------------------------

    @Test
    void adminCannotSuspendTheirOwnAccount() {
        User self = staffUser(ACTOR_USER_ID, ACTOR_UUID, UserStatus.ACTIVE);
        UserPrincipal actor = principal();
        when(userRepository.findAdminUserByUuid(ACTOR_UUID)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> adminUserService.updateStatus(ACTOR_UUID,
                new AdminUserStatusUpdateRequest("DISABLED"), actor, httpServletRequest))
                .isInstanceOf(ValidationException.class);

        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
    }

    @Test
    void adminCanReactivateTheirOwnAccount() {
        User self = staffUser(ACTOR_USER_ID, ACTOR_UUID, UserStatus.LOCKED);
        UserPrincipal actor = principal();
        when(userRepository.findAdminUserByUuid(ACTOR_UUID)).thenReturn(Optional.of(self));

        assertThat(adminUserService.updateStatus(ACTOR_UUID, new AdminUserStatusUpdateRequest("ACTIVE"),
                actor, httpServletRequest).status()).isEqualTo("ACTIVE");
    }

    @Test
    void suspendingAnotherAdminRevokesTheirActiveSessions() {
        User target = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        RefreshToken activeSession = new RefreshToken();
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(target));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(TARGET_USER_ID))
                .thenReturn(List.of(activeSession));

        adminUserService.updateStatus(TARGET_UUID, new AdminUserStatusUpdateRequest("DISABLED"),
                principal(), httpServletRequest);

        assertThat(activeSession.getRevokedAt()).isNotNull();
    }

    @Test
    void reactivatingAnAdminDoesNotTouchSessions() {
        User target = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.DISABLED);
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(target));

        adminUserService.updateStatus(TARGET_UUID, new AdminUserStatusUpdateRequest("ACTIVE"),
                principal(), httpServletRequest);

        verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedAtIsNull(any());
    }

    // --- getPermissions / updatePermissions --------------------------------------------------

    @Test
    void superAdminShowsEveryPermissionGrantedRegardlessOfOverrides() {
        User superAdmin = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        superAdmin.setRoles(Set.of(role(RoleCode.SUPER_ADMIN)));
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(superAdmin));
        when(overrideRepository.findGrantedPermissionCodesByUserId(TARGET_USER_ID)).thenReturn(Set.of());
        when(permissionRepository.findAll()).thenReturn(List.of(permission("CLIENT_VIEW"), permission("AUDIT_VIEW")));

        List<PermissionResponse> permissions = adminUserService.getPermissions(TARGET_UUID);

        assertThat(permissions).allMatch(PermissionResponse::granted);
    }

    @Test
    void regularAdminOnlyShowsOverriddenPermissionsAsGranted() {
        User admin = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        admin.setRoles(Set.of(role(RoleCode.ADMIN)));
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(admin));
        when(overrideRepository.findGrantedPermissionCodesByUserId(TARGET_USER_ID)).thenReturn(Set.of("CLIENT_VIEW"));
        when(permissionRepository.findAll()).thenReturn(List.of(permission("CLIENT_VIEW"), permission("AUDIT_VIEW")));

        List<PermissionResponse> permissions = adminUserService.getPermissions(TARGET_UUID);

        assertThat(permissions).filteredOn(PermissionResponse::granted).extracting(PermissionResponse::code)
                .containsExactly("CLIENT_VIEW");
    }

    @Test
    void permissionOverridesCannotBeSetOnASuperAdmin() {
        User superAdmin = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        superAdmin.setRoles(Set.of(role(RoleCode.SUPER_ADMIN)));
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> adminUserService.updatePermissions(TARGET_UUID,
                new PermissionOverridesUpdateRequest(Set.of("CLIENT_VIEW")), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updatingPermissionsRejectsAnUnknownPermissionCode() {
        User admin = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        admin.setRoles(Set.of(role(RoleCode.ADMIN)));
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(admin));
        when(permissionRepository.findAll()).thenReturn(List.of(permission("CLIENT_VIEW")));

        assertThatThrownBy(() -> adminUserService.updatePermissions(TARGET_UUID,
                new PermissionOverridesUpdateRequest(Set.of("NOT_A_REAL_PERMISSION")), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updatingPermissionsReplacesOverridesWithExactlyTheGrantedSet() {
        User admin = staffUser(TARGET_USER_ID, TARGET_UUID, UserStatus.ACTIVE);
        admin.setRoles(Set.of(role(RoleCode.ADMIN)));
        when(userRepository.findAdminUserByUuid(TARGET_UUID)).thenReturn(Optional.of(admin));
        when(permissionRepository.findAll())
                .thenReturn(List.of(permission("CLIENT_VIEW"), permission("AUDIT_VIEW"), permission("LEAD_MANAGE")));
        when(overrideRepository.findGrantedPermissionCodesByUserId(TARGET_USER_ID)).thenReturn(Set.of("CLIENT_VIEW"));

        adminUserService.updatePermissions(TARGET_UUID,
                new PermissionOverridesUpdateRequest(Set.of("CLIENT_VIEW")), principal(), httpServletRequest);

        verify(overrideRepository).deleteAllByUserId(TARGET_USER_ID);
        verify(overrideRepository, times(1)).save(any());
    }

    private User staffUser(long id, String uuid, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUuid(uuid);
        user.setEmail(uuid + "@test.local");
        user.setPasswordHash("hash");
        user.setStatus(status);
        Set<Role> roles = new HashSet<>();
        roles.add(role(RoleCode.ADMIN));
        user.setRoles(roles);
        return user;
    }

    private Role role(RoleCode code) {
        Role role = new Role();
        role.setCode(code);
        role.setName(code.name());
        return role;
    }

    private Permission permission(String code) {
        Permission permission = new Permission();
        permission.setId((long) code.hashCode());
        permission.setCode(code);
        permission.setDescription(code);
        return permission;
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(ACTOR_USER_ID);
        user.setUuid(ACTOR_UUID);
        user.setEmail("actor@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role(RoleCode.SUPER_ADMIN));
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
