package com.nfcplatform.menu.dto;

import com.nfcplatform.menu.entity.MenuCategory;

import java.util.List;

public record MenuCategoryResponse(
        String uuid,
        String name,
        int sortOrder,
        boolean active,
        List<MenuItemResponse> items
) {
    public static MenuCategoryResponse from(MenuCategory category, List<MenuItemResponse> items) {
        return new MenuCategoryResponse(category.getUuid(), category.getName(), category.getSortOrder(),
                category.isActive(), items);
    }
}
