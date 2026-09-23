package com.nfcplatform.client.controller;

import com.nfcplatform.client.dto.*;
import com.nfcplatform.client.service.ClientService;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
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
@RequestMapping("/api/v1/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_VIEW)")
    public ApiResponse<PageResponse<ClientResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(clientService.listClients(status, type, search, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_CREATE)")
    public ApiResponse<ClientCreateResponse> create(@Valid @RequestBody ClientCreateRequest request,
                                                      @AuthenticationPrincipal UserPrincipal actor,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(clientService.createClient(request, actor, httpRequest), "Client created");
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_VIEW)")
    public ApiResponse<ClientResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(clientService.getClient(uuid));
    }

    @PutMapping("/{uuid}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_UPDATE)")
    public ApiResponse<ClientResponse> update(@PathVariable String uuid,
                                                @Valid @RequestBody ClientUpdateRequest request,
                                                @AuthenticationPrincipal UserPrincipal actor,
                                                HttpServletRequest httpRequest) {
        return ApiResponse.ok(clientService.updateClient(uuid, request, actor, httpRequest), "Client updated");
    }

    @PatchMapping("/{uuid}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_SUSPEND)")
    public ApiResponse<ClientResponse> updateStatus(@PathVariable String uuid,
                                                       @Valid @RequestBody ClientStatusUpdateRequest request,
                                                       @AuthenticationPrincipal UserPrincipal actor,
                                                       HttpServletRequest httpRequest) {
        return ApiResponse.ok(clientService.updateStatus(uuid, request, actor, httpRequest), "Client status updated");
    }

    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).CLIENT_DELETE)")
    public ApiResponse<Void> delete(@PathVariable String uuid,
                                      @AuthenticationPrincipal UserPrincipal actor,
                                      HttpServletRequest httpRequest) {
        clientService.deleteClient(uuid, actor, httpRequest);
        return ApiResponse.ok(null, "Client deleted");
    }
}
