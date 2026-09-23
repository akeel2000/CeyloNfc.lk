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
@RequestMapping("/api/v1/client/google-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientGoogleReviewController {

    private final GoogleReviewLocationService reviewLocationService;

    @GetMapping
    public ApiResponse<List<GoogleReviewLocationResponse>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(reviewLocationService.listForOwnClient(actor));
    }

    @PostMapping
    public ApiResponse<GoogleReviewLocationResponse> create(@AuthenticationPrincipal UserPrincipal actor,
                                                               @Valid @RequestBody GoogleReviewLocationRequest request) {
        return ApiResponse.ok(reviewLocationService.createForOwnClient(actor, request), "Location added");
    }

    @PutMapping("/{uuid}")
    public ApiResponse<GoogleReviewLocationResponse> update(@AuthenticationPrincipal UserPrincipal actor,
                                                               @PathVariable String uuid,
                                                               @Valid @RequestBody GoogleReviewLocationRequest request) {
        return ApiResponse.ok(reviewLocationService.updateForOwnClient(actor, uuid, request), "Location updated");
    }
}
