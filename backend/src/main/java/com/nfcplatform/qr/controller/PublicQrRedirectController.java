package com.nfcplatform.qr.controller;

import com.nfcplatform.common.exception.RateLimitedException;
import com.nfcplatform.qr.service.QrCodeService;
import com.nfcplatform.security.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

/** QR scan redirect - identical pipeline to PublicRedirectController (NFC taps). */
@RestController
@RequestMapping("/api/v1/public/q")
@RequiredArgsConstructor
public class PublicQrRedirectController {

    private static final int RATE_LIMIT_CAPACITY = 30;
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    private final QrCodeService qrCodeService;
    private final RateLimiterService rateLimiterService;

    @GetMapping("/{token}")
    public ResponseEntity<Void> redirect(@PathVariable String token, HttpServletRequest request) {
        String rateKey = "qr-redirect:" + clientIp(request);
        if (!rateLimiterService.tryConsume(rateKey, RATE_LIMIT_CAPACITY, RATE_LIMIT_PERIOD)) {
            throw new RateLimitedException("Too many requests - please try again shortly");
        }

        String targetUrl = qrCodeService.resolveRedirectTarget(token, request.getHeader("User-Agent"), request.getHeader("Referer"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
