package com.nfcplatform.settings.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.settings.dto.PlatformSettingsResponse;
import com.nfcplatform.settings.service.PlatformSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only, unauthenticated - the public site header/footer render these live (see brand.ts). */
@RestController
@RequestMapping("/api/v1/public/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    public ApiResponse<PlatformSettingsResponse> get() {
        return ApiResponse.ok(platformSettingsService.get());
    }
}
