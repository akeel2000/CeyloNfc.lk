package com.nfcplatform.menu.controller;

import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.menu.dto.MenuResponse;
import com.nfcplatform.menu.service.MenuService;
import com.nfcplatform.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public/menu")
@RequiredArgsConstructor
public class PublicMenuController {

    private static final int VIEW_RATE_LIMIT_CAPACITY = 20;
    private static final Duration VIEW_RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final MenuService menuService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/{slug}")
    public ApiResponse<MenuResponse> get(@PathVariable String slug) {
        return ApiResponse.ok(menuService.getPublicMenu(slug));
    }

    @PostMapping("/{slug}/view")
    public ApiResponse<Void> recordView(@PathVariable String slug, HttpServletRequest request) {
        String rateKey = "menu-view:" + com.nfcplatform.common.web.ClientIpResolver.resolve(request);
        if (!rateLimiterService.tryConsume(rateKey, VIEW_RATE_LIMIT_CAPACITY, VIEW_RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }
        menuService.recordMenuView(slug, request.getHeader("User-Agent"), request.getHeader("Referer"));
        return ApiResponse.ok(null);
    }
}
