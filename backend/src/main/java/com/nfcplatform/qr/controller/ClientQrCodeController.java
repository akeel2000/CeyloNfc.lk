package com.nfcplatform.qr.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.qr.dto.QrCodeCreateRequest;
import com.nfcplatform.qr.dto.QrCodeCreateResponse;
import com.nfcplatform.qr.dto.QrCodeResponse;
import com.nfcplatform.qr.service.QrCodeService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/qr-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientQrCodeController {

    private final QrCodeService qrCodeService;

    @GetMapping
    public ApiResponse<List<QrCodeResponse>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(qrCodeService.listForOwnClient(actor));
    }

    @PostMapping
    public ApiResponse<QrCodeCreateResponse> create(@AuthenticationPrincipal UserPrincipal actor,
                                                       @Valid @RequestBody QrCodeCreateRequest request) {
        return ApiResponse.ok(qrCodeService.createForOwnClient(actor, request), "QR code created");
    }

    @PatchMapping("/{uuid}/status")
    public ApiResponse<QrCodeResponse> setStatus(@AuthenticationPrincipal UserPrincipal actor,
                                                    @PathVariable String uuid,
                                                    @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ApiResponse.ok(qrCodeService.setStatusForOwnClient(actor, uuid, active),
                active ? "QR code activated" : "QR code suspended");
    }
}
