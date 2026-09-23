package com.nfcplatform.lead.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.lead.dto.LeadConvertRequest;
import com.nfcplatform.lead.dto.LeadConvertResponse;
import com.nfcplatform.lead.dto.LeadResponse;
import com.nfcplatform.lead.dto.LeadUpdateRequest;
import com.nfcplatform.lead.service.LeadService;
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
@RequestMapping("/api/v1/admin/leads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).LEAD_MANAGE)")
public class AdminLeadController {

    private final LeadService leadService;

    @GetMapping
    public ApiResponse<PageResponse<LeadResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(leadService.list(status, search, pageable));
    }

    @PatchMapping("/{uuid}")
    public ApiResponse<LeadResponse> update(@PathVariable String uuid, @Valid @RequestBody LeadUpdateRequest request,
                                             @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(leadService.update(uuid, request, actor), "Lead updated");
    }

    /** Requires CLIENT_CREATE in addition to the class-level LEAD_MANAGE gate - this creates a
     *  real client account, so LEAD_MANAGE alone must not be a backdoor around CLIENT_CREATE. */
    @PostMapping("/{uuid}/convert")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasAuthority(T(com.nfcplatform.permission.PermissionCodes).LEAD_MANAGE) " +
            "and hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_CREATE))")
    public ApiResponse<LeadConvertResponse> convert(@PathVariable String uuid,
                                                       @Valid @RequestBody LeadConvertRequest request,
                                                       @AuthenticationPrincipal UserPrincipal actor,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok(leadService.convertToClient(uuid, request, actor, httpRequest), "Lead converted to client");
    }
}
