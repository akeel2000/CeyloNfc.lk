package com.nfcplatform.support.controller;

import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.RateLimiterService;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.support.dto.MessageCreateRequest;
import com.nfcplatform.support.dto.SupportTicketResponse;
import com.nfcplatform.support.dto.TicketCreateRequest;
import com.nfcplatform.support.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/client/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientSupportController {

    private static final int RATE_LIMIT_CAPACITY = 10;
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final SupportService supportService;
    private final RateLimiterService rateLimiterService;

    @GetMapping
    public ApiResponse<List<SupportTicketResponse>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(supportService.listForOwnClient(actor));
    }

    @PostMapping
    public ApiResponse<SupportTicketResponse> create(@Valid @RequestBody TicketCreateRequest request,
                                                       @AuthenticationPrincipal UserPrincipal actor) {
        requireWithinLimit(actor);
        return ApiResponse.ok(supportService.createForOwnClient(request, actor), "Support ticket created");
    }

    @GetMapping("/{uuid}")
    public ApiResponse<SupportTicketResponse> get(@PathVariable String uuid,
                                                    @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(supportService.getForOwnClient(uuid, actor));
    }

    @PostMapping("/{uuid}/messages")
    public ApiResponse<SupportTicketResponse> addMessage(@PathVariable String uuid,
                                                           @Valid @RequestBody MessageCreateRequest request,
                                                           @AuthenticationPrincipal UserPrincipal actor) {
        requireWithinLimit(actor);
        return ApiResponse.ok(supportService.addMessageOwnClient(uuid, request, actor), "Reply sent");
    }

    private void requireWithinLimit(UserPrincipal actor) {
        String rateKey = "client-support:" + actor.getUuid();
        if (!rateLimiterService.tryConsume(rateKey, RATE_LIMIT_CAPACITY, RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }
    }
}
