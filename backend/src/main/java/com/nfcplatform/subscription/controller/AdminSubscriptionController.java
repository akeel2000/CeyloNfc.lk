package com.nfcplatform.subscription.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.dto.SubscriptionAssignRequest;
import com.nfcplatform.subscription.dto.SubscriptionResponse;
import com.nfcplatform.subscription.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).SUBSCRIPTION_MANAGE)")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ApiResponse<SubscriptionResponse> getForClient(@RequestParam String clientUuid) {
        return ApiResponse.ok(subscriptionService.getForClient(clientUuid));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<SubscriptionResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String packagePlanUuid,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(subscriptionService.list(status, packagePlanUuid, pageable));
    }

    @PostMapping
    public ApiResponse<SubscriptionResponse> assign(@RequestParam String clientUuid,
                                                       @Valid @RequestBody SubscriptionAssignRequest request,
                                                       @AuthenticationPrincipal UserPrincipal actor,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok(subscriptionService.assignForClient(clientUuid, request, actor, httpRequest),
                "Subscription updated");
    }
}
