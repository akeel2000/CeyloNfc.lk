package com.nfcplatform.template.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.template.dto.TemplateRequest;
import com.nfcplatform.template.dto.TemplateResponse;
import com.nfcplatform.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).TEMPLATE_MANAGE)")
public class AdminTemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ApiResponse<List<TemplateResponse>> list() {
        return ApiResponse.ok(templateService.listAllForAdmin());
    }

    @PostMapping
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.create(request), "Template created");
    }

    @PutMapping("/{uuid}")
    public ApiResponse<TemplateResponse> update(@PathVariable String uuid, @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.ok(templateService.update(uuid, request), "Template updated");
    }
}
