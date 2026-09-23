package com.nfcplatform.analytics.controller;

import com.nfcplatform.analytics.dto.AnalyticsSummaryResponse;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).ANALYTICS_VIEW)")
    public ApiResponse<AnalyticsSummaryResponse> summary(@RequestParam String clientUuid,
                                                            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(analyticsService.summaryForClient(clientUuid, days));
    }
}
