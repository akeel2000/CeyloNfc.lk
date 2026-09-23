package com.nfcplatform.user.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.permission.dto.PermissionOverridesUpdateRequest;
import com.nfcplatform.permission.dto.PermissionResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.dto.AdminUserCreateRequest;
import com.nfcplatform.user.dto.AdminUserCreateResponse;
import com.nfcplatform.user.dto.AdminUserResponse;
import com.nfcplatform.user.dto.AdminUserStatusUpdateRequest;
import com.nfcplatform.user.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages platform staff (ADMIN/SUPER_ADMIN) accounts and per-admin permission overrides.
 * SUPER_ADMIN-only, unlike most /admin/** controllers which also accept a granted permission -
 * granting yourself or another admin more access is inherently a Super Admin action, and
 * letting a permission-holding admin manage this would be a privilege-escalation path.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(adminUserService.listAdminUsers(search, pageable));
    }

    @GetMapping("/{uuid}")
    public ApiResponse<AdminUserResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(adminUserService.getAdminUser(uuid));
    }

    @PostMapping
    public ApiResponse<AdminUserCreateResponse> create(@Valid @RequestBody AdminUserCreateRequest request,
                                                          @AuthenticationPrincipal UserPrincipal actor,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.createAdminUser(request, actor, httpRequest), "Admin user created");
    }

    @PatchMapping("/{uuid}/status")
    public ApiResponse<AdminUserResponse> updateStatus(@PathVariable String uuid,
                                                          @Valid @RequestBody AdminUserStatusUpdateRequest request,
                                                          @AuthenticationPrincipal UserPrincipal actor,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.updateStatus(uuid, request, actor, httpRequest), "User status updated");
    }

    @GetMapping("/{uuid}/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable String uuid) {
        return ApiResponse.ok(adminUserService.getPermissions(uuid));
    }

    @PutMapping("/{uuid}/permissions")
    public ApiResponse<List<PermissionResponse>> updatePermissions(@PathVariable String uuid,
                                                                      @Valid @RequestBody PermissionOverridesUpdateRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal actor,
                                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(adminUserService.updatePermissions(uuid, request, actor, httpRequest), "Permissions updated");
    }
}
