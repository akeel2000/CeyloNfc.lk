package com.nfcplatform.menu.controller;

import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.menu.dto.*;
import com.nfcplatform.menu.service.MenuService;
import com.nfcplatform.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/menu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientMenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<MenuResponse> get(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(menuService.getOrCreateOwnMenu(actor));
    }

    @PutMapping
    public ApiResponse<MenuResponse> update(@AuthenticationPrincipal UserPrincipal actor,
                                              @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.ok(menuService.updateOwnMenu(actor, request), "Menu saved");
    }

    @PostMapping("/publish")
    public ApiResponse<MenuResponse> publish(@AuthenticationPrincipal UserPrincipal actor,
                                               @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ApiResponse.ok(menuService.setPublished(actor, published),
                published ? "Menu published" : "Menu unpublished");
    }

    @PostMapping("/categories")
    public ApiResponse<MenuResponse> createCategory(@AuthenticationPrincipal UserPrincipal actor,
                                                       @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.createCategory(actor, request), "Category added");
    }

    @PutMapping("/categories/{categoryUuid}")
    public ApiResponse<MenuResponse> updateCategory(@AuthenticationPrincipal UserPrincipal actor,
                                                       @PathVariable String categoryUuid,
                                                       @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.updateCategory(actor, categoryUuid, request), "Category updated");
    }

    @DeleteMapping("/categories/{categoryUuid}")
    public ApiResponse<MenuResponse> deleteCategory(@AuthenticationPrincipal UserPrincipal actor,
                                                       @PathVariable String categoryUuid) {
        return ApiResponse.ok(menuService.deleteCategory(actor, categoryUuid), "Category deleted");
    }

    @PostMapping("/categories/{categoryUuid}/items")
    public ApiResponse<MenuResponse> createItem(@AuthenticationPrincipal UserPrincipal actor,
                                                   @PathVariable String categoryUuid,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.createItem(actor, categoryUuid, request), "Item added");
    }

    @PutMapping("/categories/{categoryUuid}/items/{itemUuid}")
    public ApiResponse<MenuResponse> updateItem(@AuthenticationPrincipal UserPrincipal actor,
                                                   @PathVariable String categoryUuid,
                                                   @PathVariable String itemUuid,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.updateItem(actor, categoryUuid, itemUuid, request), "Item updated");
    }

    @DeleteMapping("/categories/{categoryUuid}/items/{itemUuid}")
    public ApiResponse<MenuResponse> deleteItem(@AuthenticationPrincipal UserPrincipal actor,
                                                   @PathVariable String categoryUuid,
                                                   @PathVariable String itemUuid) {
        return ApiResponse.ok(menuService.deleteItem(actor, categoryUuid, itemUuid), "Item deleted");
    }
}
