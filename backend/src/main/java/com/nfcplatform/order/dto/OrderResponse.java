package com.nfcplatform.order.dto;

import com.nfcplatform.order.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String uuid,
        String orderNumber,
        String clientUuid,
        String clientDisplayName,
        String status,
        String paymentStatus,
        BigDecimal subtotal,
        BigDecimal total,
        String notes,
        List<OrderItemDto> items,
        Instant createdAt
) {
    public static OrderResponse from(Order order, String clientUuid, String clientDisplayName, List<OrderItemDto> items) {
        return new OrderResponse(order.getUuid(), order.getOrderNumber(), clientUuid, clientDisplayName,
                order.getStatus().name(), order.getPaymentStatus().name(), order.getSubtotal(), order.getTotal(),
                order.getNotes(), items, order.getCreatedAt());
    }
}
