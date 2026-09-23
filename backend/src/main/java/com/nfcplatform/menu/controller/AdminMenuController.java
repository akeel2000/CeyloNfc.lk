package com.nfcplatform.menu.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.menu.dto.*;
import com.nfcplatform.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/menu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(com.nfcplatform.permission.PermissionCodes).MENU_MANAGE)")
public class AdminMenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<MenuResponse> get(@RequestParam String clientUuid) {
        return ApiResponse.ok(menuService.getOrCreateMenuForClient(clientUuid));
    }

    @PutMapping
    public ApiResponse<MenuResponse> update(@RequestParam String clientUuid,
                                              @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.ok(menuService.updateMenuForClient(clientUuid, request), "Menu saved");
    }

    @PostMapping("/publish")
    public ApiResponse<MenuResponse> publish(@RequestParam String clientUuid, @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ApiResponse.ok(menuService.setPublishedForClient(clientUuid, published),
                published ? "Menu published" : "Menu unpublished");
    }

    @PostMapping("/categories")
    public ApiResponse<MenuResponse> createCategory(@RequestParam String clientUuid,
                                                       @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.createCategoryForClient(clientUuid, request), "Category added");
    }

    @PutMapping("/categories/{categoryUuid}")
    public ApiResponse<MenuResponse> updateCategory(@RequestParam String clientUuid,
                                                       @PathVariable String categoryUuid,
                                                       @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.updateCategoryForClient(clientUuid, categoryUuid, request), "Category updated");
    }

    @DeleteMapping("/categories/{categoryUuid}")
    public ApiResponse<MenuResponse> deleteCategory(@RequestParam String clientUuid,
                                                       @PathVariable String categoryUuid) {
        return ApiResponse.ok(menuService.deleteCategoryForClient(clientUuid, categoryUuid), "Category deleted");
    }

    @PostMapping("/categories/{categoryUuid}/items")
    public ApiResponse<MenuResponse> createItem(@RequestParam String clientUuid,
                                                   @PathVariable String categoryUuid,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.createItemForClient(clientUuid, categoryUuid, request), "Item added");
    }

    @PutMapping("/categories/{categoryUuid}/items/{itemUuid}")
    public ApiResponse<MenuResponse> updateItem(@RequestParam String clientUuid,
                                                   @PathVariable String categoryUuid,
                                                   @PathVariable String itemUuid,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.updateItemForClient(clientUuid, categoryUuid, itemUuid, request), "Item updated");
    }

    @DeleteMapping("/categories/{categoryUuid}/items/{itemUuid}")
    public ApiResponse<MenuResponse> deleteItem(@RequestParam String clientUuid,
                                                   @PathVariable String categoryUuid,
                                                   @PathVariable String itemUuid) {
        return ApiResponse.ok(menuService.deleteItemForClient(clientUuid, categoryUuid, itemUuid), "Item deleted");
    }
}
