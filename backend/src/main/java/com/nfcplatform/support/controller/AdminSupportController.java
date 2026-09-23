package com.nfcplatform.support.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.support.dto.MessageCreateRequest;
import com.nfcplatform.support.dto.SupportTicketResponse;
import com.nfcplatform.support.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).SUPPORT_MANAGE)")
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping
    public ApiResponse<PageResponse<SupportTicketResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(supportService.listForAdmin(status, search, pageable));
    }

    @GetMapping("/{uuid}")
    public ApiResponse<SupportTicketResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(supportService.getForAdmin(uuid));
    }

    @PostMapping("/{uuid}/messages")
    public ApiResponse<SupportTicketResponse> addMessage(@PathVariable String uuid,
                                                           @Valid @RequestBody MessageCreateRequest request,
                                                           @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(supportService.addMessageAdmin(uuid, request, actor), "Reply sent");
    }

    @PatchMapping("/{uuid}/status")
    public ApiResponse<SupportTicketResponse> updateStatus(@PathVariable String uuid,
                                                             @RequestParam String status,
                                                             @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(supportService.updateStatus(uuid, status, actor), "Ticket status updated");
    }
}
