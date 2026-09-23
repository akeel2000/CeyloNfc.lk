package com.nfcplatform.order.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.order.dto.OrderCreateRequest;
import com.nfcplatform.order.dto.OrderResponse;
import com.nfcplatform.order.service.OrderService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).ORDER_MANAGE)")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(orderService.list(status, search, pageable));
    }

    @GetMapping("/{uuid}")
    public ApiResponse<OrderResponse> get(@PathVariable String uuid) {
        return ApiResponse.ok(orderService.getByUuid(uuid));
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request,
                                              @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(orderService.create(request, actor), "Order created");
    }

    @PatchMapping("/{uuid}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable String uuid,
                                                     @RequestParam String status,
                                                     @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(orderService.updateStatus(uuid, status, actor), "Order status updated");
    }

    @PatchMapping("/{uuid}/payment-status")
    public ApiResponse<OrderResponse> updatePaymentStatus(@PathVariable String uuid,
                                                            @RequestParam String paymentStatus,
                                                            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(orderService.updatePaymentStatus(uuid, paymentStatus, actor), "Payment status updated");
    }
}
