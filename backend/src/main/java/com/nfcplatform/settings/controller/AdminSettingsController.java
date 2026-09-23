package com.nfcplatform.settings.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.settings.dto.PlatformSettingsResponse;
import com.nfcplatform.settings.dto.PlatformSettingsUpdateRequest;
import com.nfcplatform.settings.service.PlatformSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).SETTINGS_MANAGE)")
public class AdminSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    public ApiResponse<PlatformSettingsResponse> get() {
        return ApiResponse.ok(platformSettingsService.get());
    }

    @PutMapping
    public ApiResponse<PlatformSettingsResponse> update(@Valid @RequestBody PlatformSettingsUpdateRequest request,
                                                          @AuthenticationPrincipal UserPrincipal actor,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(platformSettingsService.update(request, actor, httpRequest), "Settings saved");
    }
}
