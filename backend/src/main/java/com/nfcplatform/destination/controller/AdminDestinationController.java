package com.nfcplatform.destination.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.destination.dto.DestinationCreateRequest;
import com.nfcplatform.destination.dto.DestinationResponse;
import com.nfcplatform.destination.service.DestinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/destinations")
@RequiredArgsConstructor
public class AdminDestinationController {

    private final DestinationService destinationService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).DESTINATION_MANAGE)")
    public ApiResponse<List<DestinationResponse>> listForClient(@RequestParam String clientUuid) {
        return ApiResponse.ok(destinationService.listForClient(clientUuid));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).DESTINATION_MANAGE)")
    public ApiResponse<DestinationResponse> create(@Valid @RequestBody DestinationCreateRequest request) {
        return ApiResponse.ok(destinationService.create(request), "Destination created");
    }
}
