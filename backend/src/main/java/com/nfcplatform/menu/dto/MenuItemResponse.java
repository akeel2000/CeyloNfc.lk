package com.nfcplatform.menu.dto;

import com.nfcplatform.menu.entity.MenuItem;

import java.math.BigDecimal;

public record MenuItemResponse(
        String uuid,
        String name,
        String description,
        String image,
        BigDecimal price,
        boolean available,
        boolean featured,
        int sortOrder
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(item.getUuid(), item.getName(), item.getDescription(), item.getImage(),
                item.getPrice(), item.isAvailable(), item.isFeatured(), item.getSortOrder());
    }
}
