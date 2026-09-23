package com.nfcplatform.profile.controller;

import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.profile.dto.PublicProfileResponse;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.profile.service.VCardGenerator;
import com.nfcplatform.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicProfileController {

    private static final int VIEW_RATE_LIMIT_CAPACITY = 20;
    private static final Duration VIEW_RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final ProfileService profileService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/profile/{slug}")
    public ApiResponse<PublicProfileResponse> individual(@PathVariable String slug) {
        return ApiResponse.ok(profileService.getPublicIndividualProfile(slug));
    }

    @GetMapping("/company/{slug}")
    public ApiResponse<PublicProfileResponse> company(@PathVariable String slug) {
        return ApiResponse.ok(profileService.getPublicCompanyProfile(slug));
    }

    @GetMapping("/profile/{slug}/vcard")
    public ResponseEntity<String> individualVCard(@PathVariable String slug) {
        return vcardResponse(profileService.getPublicIndividualProfile(slug), slug);
    }

    @GetMapping("/company/{slug}/vcard")
    public ResponseEntity<String> companyVCard(@PathVariable String slug) {
        return vcardResponse(profileService.getPublicCompanyProfile(slug), slug);
    }

    @PostMapping("/profile/{slug}/view")
    public ApiResponse<Void> recordIndividualView(@PathVariable String slug, HttpServletRequest request) {
        checkRateLimit(request);
        profileService.recordIndividualProfileView(slug, request.getHeader("User-Agent"), request.getHeader("Referer"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/company/{slug}/view")
    public ApiResponse<Void> recordCompanyView(@PathVariable String slug, HttpServletRequest request) {
        checkRateLimit(request);
        profileService.recordCompanyProfileView(slug, request.getHeader("User-Agent"), request.getHeader("Referer"));
        return ApiResponse.ok(null);
    }

    private void checkRateLimit(HttpServletRequest request) {
        String rateKey = "profile-view:" + com.nfcplatform.common.web.ClientIpResolver.resolve(request);
        if (!rateLimiterService.tryConsume(rateKey, VIEW_RATE_LIMIT_CAPACITY, VIEW_RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }
    }

    private ResponseEntity<String> vcardResponse(PublicProfileResponse profile, String slug) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vcard"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + slug + ".vcf\"")
                .body(VCardGenerator.generate(profile));
    }
}
