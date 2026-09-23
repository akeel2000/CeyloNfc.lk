package com.nfcplatform.audit.controller;

import com.nfcplatform.audit.dto.AuditLogFiltersResponse;
import com.nfcplatform.audit.dto.AuditLogResponse;
import com.nfcplatform.audit.service.AuditLogQueryService;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).AUDIT_VIEW)")
public class AdminAuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorUuid,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 25, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(auditLogQueryService.search(action, entityType, actorUuid, search,
                parseInstant(from), parseInstant(to), pageable));
    }

    @GetMapping("/filters")
    public ApiResponse<AuditLogFiltersResponse> filters() {
        return ApiResponse.ok(auditLogQueryService.filters());
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            throw new ValidationException("Invalid date/time: " + value);
        }
    }
}
