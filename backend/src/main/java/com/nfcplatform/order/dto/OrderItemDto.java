package com.nfcplatform.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productUuid,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
}
