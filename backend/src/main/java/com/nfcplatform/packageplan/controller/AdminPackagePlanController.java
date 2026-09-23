package com.nfcplatform.packageplan.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.packageplan.dto.PackagePlanRequest;
import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.packageplan.service.PackagePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/packages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).PACKAGE_MANAGE)")
public class AdminPackagePlanController {

    private final PackagePlanService packagePlanService;

    @GetMapping
    public ApiResponse<List<PackagePlanResponse>> list() {
        return ApiResponse.ok(packagePlanService.listAllForAdmin());
    }

    @PostMapping
    public ApiResponse<PackagePlanResponse> create(@Valid @RequestBody PackagePlanRequest request) {
        return ApiResponse.ok(packagePlanService.create(request), "Package created");
    }

    @PutMapping("/{uuid}")
    public ApiResponse<PackagePlanResponse> update(@PathVariable String uuid, @Valid @RequestBody PackagePlanRequest request) {
        return ApiResponse.ok(packagePlanService.update(uuid, request), "Package updated");
    }
}
