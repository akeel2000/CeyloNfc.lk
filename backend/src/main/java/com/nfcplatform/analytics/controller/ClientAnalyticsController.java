package com.nfcplatform.analytics.controller;

import com.nfcplatform.analytics.dto.AnalyticsSummaryResponse;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal actor,
                                                            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(analyticsService.summaryForOwnClient(actor, days));
    }
}
