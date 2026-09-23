package com.nfcplatform.subscription.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.dto.SubscriptionResponse;
import com.nfcplatform.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/subscription")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ApiResponse<SubscriptionResponse> getOwn(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(subscriptionService.getForOwnClient(actor));
    }
}
