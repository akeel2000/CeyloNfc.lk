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
@RequestMapping("/api/v1/admin/qr-codes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).QR_MANAGE)")
public class AdminQrCodeController {

    private final QrCodeService qrCodeService;

    @GetMapping
    public ApiResponse<List<QrCodeResponse>> listForClient(@RequestParam String clientUuid) {
        return ApiResponse.ok(qrCodeService.listForClient(clientUuid));
    }

    @GetMapping("/count")
    public ApiResponse<Long> count() {
        return ApiResponse.ok(qrCodeService.countAll());
    }

    @PostMapping
    public ApiResponse<QrCodeCreateResponse> create(@RequestParam String clientUuid,
                                                      @Valid @RequestBody QrCodeCreateRequest request,
                                                      @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(qrCodeService.createForClient(clientUuid, request, actor), "QR code created");
    }

    @PatchMapping("/{uuid}/status")
    public ApiResponse<QrCodeResponse> setStatus(@PathVariable String uuid, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ApiResponse.ok(qrCodeService.setStatusForAdmin(uuid, active),
                active ? "QR code activated" : "QR code suspended");
    }
}
