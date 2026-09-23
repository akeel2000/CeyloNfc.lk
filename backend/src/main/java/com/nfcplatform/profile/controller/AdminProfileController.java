package com.nfcplatform.profile.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.profile.dto.BusinessHourDto;
import com.nfcplatform.profile.dto.ProfileResponse;
import com.nfcplatform.profile.dto.ProfileUpdateRequest;
import com.nfcplatform.profile.dto.SocialLinkDto;
import com.nfcplatform.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).PROFILE_MANAGE)")
public class AdminProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> get(@RequestParam String clientUuid) {
        return ApiResponse.ok(profileService.getOrCreateProfileForClient(clientUuid));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@RequestParam String clientUuid,
                                                 @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(profileService.updateProfileForClient(clientUuid, request), "Profile saved");
    }

    @PostMapping("/publish")
    public ApiResponse<ProfileResponse> publish(@RequestParam String clientUuid, @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ApiResponse.ok(profileService.setPublishedForClient(clientUuid, published),
                published ? "Profile published" : "Profile unpublished");
    }

    @PutMapping("/social-links")
    public ApiResponse<List<SocialLinkDto>> updateSocialLinks(@RequestParam String clientUuid,
                                                                 @Valid @RequestBody List<SocialLinkDto> links) {
        return ApiResponse.ok(profileService.replaceSocialLinksForClient(clientUuid, links), "Social links saved");
    }

    @PutMapping("/business-hours")
    public ApiResponse<List<BusinessHourDto>> updateBusinessHours(@RequestParam String clientUuid,
                                                                      @Valid @RequestBody List<BusinessHourDto> hours) {
        return ApiResponse.ok(profileService.replaceBusinessHoursForClient(clientUuid, hours), "Opening hours saved");
    }
}
