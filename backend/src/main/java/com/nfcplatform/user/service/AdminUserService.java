package com.nfcplatform.user.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.auth.repository.RefreshTokenRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.mail.EmailService;
import com.nfcplatform.common.util.PasswordGenerator;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.permission.dto.PermissionOverridesUpdateRequest;
import com.nfcplatform.permission.dto.PermissionResponse;
import com.nfcplatform.permission.entity.AdminPermissionOverride;
import com.nfcplatform.permission.entity.Permission;
import com.nfcplatform.permission.repository.AdminPermissionOverrideRepository;
import com.nfcplatform.permission.repository.PermissionRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.role.repository.RoleRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.dto.AdminUserCreateRequest;
import com.nfcplatform.user.dto.AdminUserCreateResponse;
import com.nfcplatform.user.dto.AdminUserResponse;
import com.nfcplatform.user.dto.AdminUserStatusUpdateRequest;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.entity.UserStatus;
import com.nfcplatform.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Manages platform staff accounts (ADMIN/SUPER_ADMIN) and per-admin permission overrides.
 * Deliberately separate from ClientService, which manages CLIENT-role users - staff and
 * customer accounts have different lifecycles and this keeps SUPER_ADMIN-only endpoints
 * (see AdminUserController) from ever sharing a code path with client self-service.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AdminPermissionOverrideRepository overrideRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listAdminUsers(String search, Pageable pageable) {
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<User> page = userRepository.searchAdminUsers(searchPattern, pageable);
        return PageResponse.of(page, AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getAdminUser(String uuid) {
        return AdminUserResponse.from(findStaffOrThrow(uuid));
    }

    @Transactional
    public AdminUserCreateResponse createAdminUser(AdminUserCreateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        RoleCode roleCode = parseStaffRole(request.role());

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user account already exists with this email");
        }

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException(roleCode + " role missing - check Flyway seed migration"));

        String temporaryPassword = PasswordGenerator.generate();

        User user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setRoles(Set.of(role));
        user = userRepository.save(user);

        emailService.sendAccountCreatedEmail(user.getEmail(), appProperties.getFrontendUrl() + "/login");

        auditService.record(actor.getId(), "USER_CREATE", "User", user.getUuid(),
                clientIp(httpRequest), java.util.Map.of("email", user.getEmail(), "role", roleCode.name()));

        return new AdminUserCreateResponse(AdminUserResponse.from(user), temporaryPassword);
    }

    @Transactional
    public AdminUserResponse updateStatus(String uuid, AdminUserStatusUpdateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        User user = findStaffOrThrow(uuid);
        UserStatus newStatus = parseStatus(request.status());

        if (user.getUuid().equals(actor.getUuid()) && newStatus != UserStatus.ACTIVE) {
            throw new ValidationException("You cannot suspend or disable your own account");
        }

        user.setStatus(newStatus);
        if (newStatus != UserStatus.ACTIVE) {
            revokeAllSessionsForUser(user.getId());
        }
        user = userRepository.save(user);

        auditService.record(actor.getId(), "USER_STATUS_CHANGE", "User", user.getUuid(),
                clientIp(httpRequest), java.util.Map.of("status", newStatus.name()));

        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions(String uuid) {
        User user = findStaffOrThrow(uuid);
        boolean isSuperAdmin = user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.SUPER_ADMIN);
        Set<String> granted = overrideRepository.findGrantedPermissionCodesByUserId(user.getId());

        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(permission -> new PermissionResponse(
                        permission.getCode(),
                        permission.getDescription(),
                        isSuperAdmin || granted.contains(permission.getCode())))
                .toList();
    }

    @Transactional
    public List<PermissionResponse> updatePermissions(String uuid, PermissionOverridesUpdateRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        User user = findStaffOrThrow(uuid);
        boolean isSuperAdmin = user.getRoles().stream().anyMatch(role -> role.getCode() == RoleCode.SUPER_ADMIN);
        if (isSuperAdmin) {
            throw new ValidationException("Super Admin already has every permission - overrides don't apply");
        }

        List<Permission> allPermissions = permissionRepository.findAll();
        Set<String> validCodes = allPermissions.stream().map(Permission::getCode).collect(java.util.stream.Collectors.toSet());
        for (String code : request.grantedPermissionCodes()) {
            if (!validCodes.contains(code)) {
                throw new ValidationException("Unknown permission code: " + code);
            }
        }

        overrideRepository.deleteAllByUserId(user.getId());
        for (Permission permission : allPermissions) {
            if (request.grantedPermissionCodes().contains(permission.getCode())) {
                AdminPermissionOverride override = new AdminPermissionOverride();
                override.setUserId(user.getId());
                override.setPermissionId(permission.getId());
                override.setGranted(true);
                overrideRepository.save(override);
            }
        }

        auditService.record(actor.getId(), "USER_PERMISSIONS_UPDATE", "User", user.getUuid(),
                clientIp(httpRequest), java.util.Map.of("grantedPermissions", request.grantedPermissionCodes()));

        return getPermissions(uuid);
    }

    private void revokeAllSessionsForUser(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(rt -> rt.setRevokedAt(Instant.now()));
    }

    private User findStaffOrThrow(String uuid) {
        return userRepository.findAdminUserByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user was not found"));
    }

    private RoleCode parseStaffRole(String value) {
        RoleCode code;
        try {
            code = RoleCode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role: " + value);
        }
        if (code != RoleCode.ADMIN && code != RoleCode.SUPER_ADMIN) {
            throw new ValidationException("Role must be ADMIN or SUPER_ADMIN");
        }
        return code;
    }

    private UserStatus parseStatus(String value) {
        try {
            return UserStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid user status: " + value);
        }
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
