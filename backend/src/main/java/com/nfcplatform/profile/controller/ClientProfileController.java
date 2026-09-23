package com.nfcplatform.profile.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.profile.dto.BusinessHourDto;
import com.nfcplatform.profile.dto.ProfileResponse;
import com.nfcplatform.profile.dto.ProfileUpdateRequest;
import com.nfcplatform.profile.dto.SocialLinkDto;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientProfileController {

    private final ProfileService profileService;
    private final TemplateService templateService;

    @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(profileService.getOrCreateOwnProfile(actor));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal UserPrincipal actor,
                                                 @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(profileService.updateOwnProfile(actor, request), "Profile saved");
    }

    @PostMapping("/publish")
    public ApiResponse<ProfileResponse> publish(@AuthenticationPrincipal UserPrincipal actor,
                                                  @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ApiResponse.ok(profileService.setPublished(actor, published),
                published ? "Profile published" : "Profile unpublished");
    }

    @PutMapping("/social-links")
    public ApiResponse<List<SocialLinkDto>> updateSocialLinks(@AuthenticationPrincipal UserPrincipal actor,
                                                                 @Valid @RequestBody List<SocialLinkDto> links) {
        return ApiResponse.ok(profileService.replaceSocialLinks(actor, links), "Social links saved");
    }

    @PutMapping("/business-hours")
    public ApiResponse<List<BusinessHourDto>> updateBusinessHours(@AuthenticationPrincipal UserPrincipal actor,
                                                                      @Valid @RequestBody List<BusinessHourDto> hours) {
        return ApiResponse.ok(profileService.replaceBusinessHours(actor, hours), "Opening hours saved");
    }

    @PostMapping("/template/{templateUuid}")
    public ApiResponse<ProfileResponse> applyTemplate(@AuthenticationPrincipal UserPrincipal actor,
                                                         @PathVariable String templateUuid) {
        templateService.applyToOwnProfile(actor, templateUuid);
        return ApiResponse.ok(profileService.getOrCreateOwnProfile(actor), "Template applied");
    }
}
