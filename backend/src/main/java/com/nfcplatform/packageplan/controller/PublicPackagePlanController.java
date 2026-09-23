package com.nfcplatform.packageplan.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.packageplan.service.PackagePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/packages")
@RequiredArgsConstructor
public class PublicPackagePlanController {

    private final PackagePlanService packagePlanService;

    @GetMapping
    public ApiResponse<List<PackagePlanResponse>> list() {
        return ApiResponse.ok(packagePlanService.listActiveForPublic());
    }
}
