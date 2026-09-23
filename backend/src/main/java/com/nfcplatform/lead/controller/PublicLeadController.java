package com.nfcplatform.lead.controller;

import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.common.web.ClientIpResolver;
import com.nfcplatform.lead.dto.LeadCreateRequest;
import com.nfcplatform.lead.dto.LeadResponse;
import com.nfcplatform.lead.service.LeadService;
import com.nfcplatform.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public/leads")
@RequiredArgsConstructor
public class PublicLeadController {

    private static final int RATE_LIMIT_CAPACITY = 5;
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final LeadService leadService;
    private final RateLimiterService rateLimiterService;

    @PostMapping
    public ApiResponse<LeadResponse> create(@Valid @RequestBody LeadCreateRequest request,
                                             HttpServletRequest httpRequest) {
        String rateKey = "public-leads:" + ClientIpResolver.resolve(httpRequest);
        if (!rateLimiterService.tryConsume(rateKey, RATE_LIMIT_CAPACITY, RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }
        return ApiResponse.ok(leadService.createPublic(request), "Thanks - we'll be in touch shortly");
    }
}
