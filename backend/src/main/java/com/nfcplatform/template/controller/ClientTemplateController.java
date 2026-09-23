package com.nfcplatform.template.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.template.dto.TemplateGalleryItem;
import com.nfcplatform.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientTemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ApiResponse<List<TemplateGalleryItem>> gallery(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(templateService.listGalleryForOwnClient(actor));
    }
}
