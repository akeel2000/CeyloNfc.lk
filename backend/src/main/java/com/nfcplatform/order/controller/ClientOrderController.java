package com.nfcplatform.order.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.order.dto.OrderResponse;
import com.nfcplatform.order.service.OrderService;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> listOwn(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(orderService.listForOwnClient(actor));
    }
}
