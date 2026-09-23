package com.nfcplatform.nfc.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.nfc.dto.NfcCardResponse;
import com.nfcplatform.nfc.service.NfcCardService;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Client self-service: every query is scoped to the caller's own client record. */
@RestController
@RequestMapping("/api/v1/client/nfc-cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientNfcCardController {

    private final NfcCardService nfcCardService;

    @GetMapping
    public ApiResponse<List<NfcCardResponse>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(nfcCardService.listForOwnClient(actor));
    }

    @GetMapping("/{uuid}")
    public ApiResponse<NfcCardResponse> get(@PathVariable String uuid, @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(nfcCardService.getForOwnClient(uuid, actor));
    }
}
