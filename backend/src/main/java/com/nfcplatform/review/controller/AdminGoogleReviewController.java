package com.nfcplatform.review.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.review.dto.GoogleReviewLocationRequest;
import com.nfcplatform.review.dto.GoogleReviewLocationResponse;
import com.nfcplatform.review.service.GoogleReviewLocationService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/google-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).GOOGLE_REVIEW_MANAGE)")
public class AdminGoogleReviewController {

    private final GoogleReviewLocationService reviewLocationService;

    @GetMapping
    public ApiResponse<List<GoogleReviewLocationResponse>> listForClient(@RequestParam String clientUuid) {
        return ApiResponse.ok(reviewLocationService.listForClient(clientUuid));
    }

    @GetMapping("/count")
    public ApiResponse<Long> count() {
        return ApiResponse.ok(reviewLocationService.countAll());
    }

    @PostMapping
    public ApiResponse<GoogleReviewLocationResponse> create(@RequestParam String clientUuid,
                                                              @Valid @RequestBody GoogleReviewLocationRequest request,
                                                              @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(reviewLocationService.createForClient(clientUuid, request, actor), "Location added");
    }

    @PutMapping("/{uuid}")
    public ApiResponse<GoogleReviewLocationResponse> update(@PathVariable String uuid,
                                                              @Valid @RequestBody GoogleReviewLocationRequest request,
                                                              @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(reviewLocationService.updateForAdmin(uuid, request, actor), "Location updated");
    }
}
