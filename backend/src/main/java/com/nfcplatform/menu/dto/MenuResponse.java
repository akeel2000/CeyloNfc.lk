package com.nfcplatform.menu.dto;

import com.nfcplatform.menu.entity.Menu;

import java.util.List;

public record MenuResponse(
        String uuid,
        String slug,
        String name,
        String description,
        String logo,
        String currency,
        boolean published,
        String publicUrl,
        List<MenuCategoryResponse> categories
) {
    public static MenuResponse from(Menu menu, String frontendUrl, List<MenuCategoryResponse> categories) {
        return new MenuResponse(menu.getUuid(), menu.getSlug(), menu.getName(), menu.getDescription(),
                menu.getLogo(), menu.getCurrency(), menu.isPublished(), frontendUrl + "/menu/" + menu.getSlug(),
                categories);
    }
}
