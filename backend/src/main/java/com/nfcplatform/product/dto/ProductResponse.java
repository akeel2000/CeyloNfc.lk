package com.nfcplatform.product.dto;

import com.nfcplatform.product.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        String uuid,
        String name,
        String sku,
        String description,
        BigDecimal price,
        String image,
        String type,
        boolean active
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getUuid(), product.getName(), product.getSku(), product.getDescription(),
                product.getPrice(), product.getImage(), product.getType().name(), product.isActive());
    }
}
