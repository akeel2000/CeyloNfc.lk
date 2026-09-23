package com.nfcplatform.nfc.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.nfc.dto.*;
import com.nfcplatform.nfc.service.NfcCardService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/nfc-cards")
@RequiredArgsConstructor
public class AdminNfcCardController {

    private final NfcCardService nfcCardService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_VIEW)")
    public ApiResponse<PageResponse<NfcCardResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clientUuid,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(nfcCardService.list(status, clientUuid, search, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_CREATE)")
    public ApiResponse<NfcCardRegisterResponse> register(@Valid @RequestBody NfcCardRegisterRequest request,
                                                            @AuthenticationPrincipal UserPrincipal actor,
                                                            HttpServletRequest httpRequest) {
        return ApiResponse.ok(nfcCardService.register(request, actor, httpRequest), "NFC card registered");
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_VIEW)")
    public ApiResponse<NfcCardResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(nfcCardService.get(uuid));
    }

    @PostMapping("/{uuid}/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_ASSIGN)")
    public ApiResponse<NfcCardResponse> assign(@PathVariable String uuid,
                                                 @Valid @RequestBody NfcCardAssignRequest request,
                                                 @AuthenticationPrincipal UserPrincipal actor,
                                                 HttpServletRequest httpRequest) {
        return ApiResponse.ok(nfcCardService.assign(uuid, request, actor, httpRequest), "NFC card assigned");
    }

    @PostMapping("/{uuid}/replace")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_CREATE)")
    public ApiResponse<NfcCardRegisterResponse> replace(@PathVariable String uuid,
                                                           @Valid @RequestBody NfcCardReplaceRequest request,
                                                           @AuthenticationPrincipal UserPrincipal actor,
                                                           HttpServletRequest httpRequest) {
        return ApiResponse.ok(nfcCardService.replace(uuid, request, actor, httpRequest), "NFC card replaced");
    }

    @PostMapping("/{uuid}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_ACTIVATE)")
    public ApiResponse<NfcCardResponse> activate(@PathVariable String uuid,
                                                   @AuthenticationPrincipal UserPrincipal actor,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.ok(nfcCardService.setStatus(uuid, "ACTIVE", actor, httpRequest), "NFC card activated");
    }

    @PostMapping("/{uuid}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).NFC_SUSPEND)")
    public ApiResponse<NfcCardResponse> suspend(@PathVariable String uuid,
                                                  @AuthenticationPrincipal UserPrincipal actor,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.ok(nfcCardService.setStatus(uuid, "SUSPENDED", actor, httpRequest), "NFC card suspended");
    }
}
