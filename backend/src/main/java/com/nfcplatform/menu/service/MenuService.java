package com.nfcplatform.menu.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.util.Slugify;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.menu.dto.*;
import com.nfcplatform.menu.entity.Menu;
import com.nfcplatform.menu.entity.MenuCategory;
import com.nfcplatform.menu.entity.MenuItem;
import com.nfcplatform.menu.repository.MenuCategoryRepository;
import com.nfcplatform.menu.repository.MenuItemRepository;
import com.nfcplatform.menu.repository.MenuRepository;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final ClientRepository clientRepository;
    private final AppProperties appProperties;
    private final AnalyticsService analyticsService;

    @Transactional
    public MenuResponse getOrCreateOwnMenu(UserPrincipal actor) {
        return toResponse(getOrCreateMenu(requireOwnClient(actor)));
    }

    /** Admin equivalent of {@link #getOrCreateOwnMenu} - MENU_MANAGE, any client by uuid. */
    @Transactional
    public MenuResponse getOrCreateMenuForClient(String clientUuid) {
        return toResponse(getOrCreateMenu(requireClient(clientUuid)));
    }

    @Transactional
    public MenuResponse updateOwnMenu(UserPrincipal actor, MenuUpdateRequest request) {
        return updateMenu(requireOwnClient(actor), request);
    }

    /** Admin equivalent of {@link #updateOwnMenu} - MENU_MANAGE, any client by uuid. */
    @Transactional
    public MenuResponse updateMenuForClient(String clientUuid, MenuUpdateRequest request) {
        return updateMenu(requireClient(clientUuid), request);
    }

    private MenuResponse updateMenu(Client client, MenuUpdateRequest request) {
        Menu menu = getOrCreateMenu(client);

        if (menuRepository.existsBySlugAndClientIdNot(request.slug(), client.getId())) {
            throw new ConflictException("This URL slug is already taken - choose another");
        }

        menu.setSlug(request.slug());
        menu.setName(request.name());
        menu.setDescription(request.description());
        menu.setLogo(request.logo());
        if (request.currency() != null && !request.currency().isBlank()) {
            menu.setCurrency(request.currency());
        }
        menu = menuRepository.save(menu);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse setPublished(UserPrincipal actor, boolean published) {
        return setPublished(requireOwnClient(actor), published);
    }

    /** Admin equivalent of {@link #setPublished(UserPrincipal, boolean)} - any client by uuid. */
    @Transactional
    public MenuResponse setPublishedForClient(String clientUuid, boolean published) {
        return setPublished(requireClient(clientUuid), published);
    }

    private MenuResponse setPublished(Client client, boolean published) {
        Menu menu = getOrCreateMenu(client);
        menu.setPublished(published);
        return toResponse(menuRepository.save(menu));
    }

    @Transactional
    public MenuResponse createCategory(UserPrincipal actor, MenuCategoryRequest request) {
        return createCategory(requireOwnClient(actor), request);
    }

    /** Admin equivalent of {@link #createCategory(UserPrincipal, MenuCategoryRequest)} - any client by uuid. */
    @Transactional
    public MenuResponse createCategoryForClient(String clientUuid, MenuCategoryRequest request) {
        return createCategory(requireClient(clientUuid), request);
    }

    private MenuResponse createCategory(Client client, MenuCategoryRequest request) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = new MenuCategory();
        category.setMenuId(menu.getId());
        category.setName(request.name());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder()
                : categoryRepository.findAllByMenuIdOrderBySortOrderAsc(menu.getId()).size());
        category.setActive(request.active() == null || request.active());
        categoryRepository.save(category);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse updateCategory(UserPrincipal actor, String categoryUuid, MenuCategoryRequest request) {
        return updateCategory(requireOwnClient(actor), categoryUuid, request);
    }

    /** Admin equivalent of {@link #updateCategory(UserPrincipal, String, MenuCategoryRequest)} - any client by uuid. */
    @Transactional
    public MenuResponse updateCategoryForClient(String clientUuid, String categoryUuid, MenuCategoryRequest request) {
        return updateCategory(requireClient(clientUuid), categoryUuid, request);
    }

    private MenuResponse updateCategory(Client client, String categoryUuid, MenuCategoryRequest request) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = categoryRepository.findByUuidAndMenuId(categoryUuid, menu.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category was not found"));

        category.setName(request.name());
        if (request.sortOrder() != null) category.setSortOrder(request.sortOrder());
        if (request.active() != null) category.setActive(request.active());
        categoryRepository.save(category);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse deleteCategory(UserPrincipal actor, String categoryUuid) {
        return deleteCategory(requireOwnClient(actor), categoryUuid);
    }

    /** Admin equivalent of {@link #deleteCategory(UserPrincipal, String)} - any client by uuid. */
    @Transactional
    public MenuResponse deleteCategoryForClient(String clientUuid, String categoryUuid) {
        return deleteCategory(requireClient(clientUuid), categoryUuid);
    }

    private MenuResponse deleteCategory(Client client, String categoryUuid) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = categoryRepository.findByUuidAndMenuId(categoryUuid, menu.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category was not found"));
        categoryRepository.delete(category);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse createItem(UserPrincipal actor, String categoryUuid, MenuItemRequest request) {
        return createItem(requireOwnClient(actor), categoryUuid, request);
    }

    /** Admin equivalent of {@link #createItem(UserPrincipal, String, MenuItemRequest)} - any client by uuid. */
    @Transactional
    public MenuResponse createItemForClient(String clientUuid, String categoryUuid, MenuItemRequest request) {
        return createItem(requireClient(clientUuid), categoryUuid, request);
    }

    private MenuResponse createItem(Client client, String categoryUuid, MenuItemRequest request) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = categoryRepository.findByUuidAndMenuId(categoryUuid, menu.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category was not found"));

        MenuItem item = new MenuItem();
        item.setCategoryId(category.getId());
        applyItem(item, request, itemRepository.findAllByCategoryIdOrderBySortOrderAsc(category.getId()).size());
        itemRepository.save(item);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse updateItem(UserPrincipal actor, String categoryUuid, String itemUuid, MenuItemRequest request) {
        return updateItem(requireOwnClient(actor), categoryUuid, itemUuid, request);
    }

    /** Admin equivalent of {@link #updateItem(UserPrincipal, String, String, MenuItemRequest)} - any client by uuid. */
    @Transactional
    public MenuResponse updateItemForClient(String clientUuid, String categoryUuid, String itemUuid, MenuItemRequest request) {
        return updateItem(requireClient(clientUuid), categoryUuid, itemUuid, request);
    }

    private MenuResponse updateItem(Client client, String categoryUuid, String itemUuid, MenuItemRequest request) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = categoryRepository.findByUuidAndMenuId(categoryUuid, menu.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category was not found"));
        MenuItem item = itemRepository.findByUuidAndCategoryId(itemUuid, category.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item was not found"));

        applyItem(item, request, item.getSortOrder());
        itemRepository.save(item);
        return toResponse(menu);
    }

    @Transactional
    public MenuResponse deleteItem(UserPrincipal actor, String categoryUuid, String itemUuid) {
        return deleteItem(requireOwnClient(actor), categoryUuid, itemUuid);
    }

    /** Admin equivalent of {@link #deleteItem(UserPrincipal, String, String)} - any client by uuid. */
    @Transactional
    public MenuResponse deleteItemForClient(String clientUuid, String categoryUuid, String itemUuid) {
        return deleteItem(requireClient(clientUuid), categoryUuid, itemUuid);
    }

    private MenuResponse deleteItem(Client client, String categoryUuid, String itemUuid) {
        Menu menu = getOrCreateMenu(client);
        MenuCategory category = categoryRepository.findByUuidAndMenuId(categoryUuid, menu.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category was not found"));
        MenuItem item = itemRepository.findByUuidAndCategoryId(itemUuid, category.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item was not found"));
        itemRepository.delete(item);
        return toResponse(menu);
    }

    @Transactional(readOnly = true)
    public MenuResponse getPublicMenu(String slug) {
        Menu menu = menuRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Menu was not found"));

        List<MenuCategoryResponse> categories = categoryRepository.findAllByMenuIdOrderBySortOrderAsc(menu.getId())
                .stream()
                .filter(MenuCategory::isActive)
                .map(cat -> MenuCategoryResponse.from(cat, itemsFor(cat)))
                .toList();

        return MenuResponse.from(menu, appProperties.getFrontendUrl(), categories);
    }

    /** Client-side beacon call, same reasoning as ProfileService#recordIndividualProfileView -
     *  see that method's doc comment. Silently no-ops on an unknown/unpublished slug. */
    @Transactional
    public void recordMenuView(String slug, String userAgent, String referrer) {
        menuRepository.findBySlugAndPublishedTrue(slug)
                .ifPresent(menu -> analyticsService.recordEvent(menu.getClientId(), null, null, null,
                        AnalyticsEventType.MENU_VIEW, userAgent, referrer));
    }

    /** Used by DestinationService/DestinationResolverService for the MENU destination type. */
    @Transactional(readOnly = true)
    public boolean hasPublishedMenu(Long clientId) {
        return menuRepository.findByClientId(clientId).map(Menu::isPublished).orElse(false);
    }

    @Transactional(readOnly = true)
    public String resolvePublicUrl(Long clientId) {
        Menu menu = menuRepository.findByClientId(clientId)
                .filter(Menu::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("No published menu"));
        return appProperties.getFrontendUrl() + "/menu/" + menu.getSlug();
    }

    private void applyItem(MenuItem item, MenuItemRequest request, int defaultSortOrder) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setImage(request.image());
        item.setPrice(request.price());
        item.setAvailable(request.available() == null || request.available());
        item.setFeatured(request.featured() != null && request.featured());
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : defaultSortOrder);
    }

    private Menu getOrCreateMenu(Client client) {
        return menuRepository.findByClientId(client.getId()).orElseGet(() -> {
            Menu menu = new Menu();
            menu.setClientId(client.getId());
            menu.setName(client.getDisplayName() + " Menu");
            menu.setSlug(uniqueSlug(client.getDisplayName()));
            return menuRepository.save(menu);
        });
    }

    private String uniqueSlug(String base) {
        String candidate = Slugify.of(base);
        String result = candidate;
        int suffix = 1;
        while (menuRepository.existsBySlug(result)) {
            result = candidate + "-" + (++suffix);
        }
        return result;
    }

    private List<MenuItemResponse> itemsFor(MenuCategory category) {
        return itemRepository.findAllByCategoryIdOrderBySortOrderAsc(category.getId()).stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    private MenuResponse toResponse(Menu menu) {
        List<MenuCategory> categories = categoryRepository.findAllByMenuIdOrderBySortOrderAsc(menu.getId());
        List<MenuCategoryResponse> categoryResponses = categories.stream()
                .sorted(Comparator.comparingInt(MenuCategory::getSortOrder))
                .map(cat -> MenuCategoryResponse.from(cat, itemsFor(cat)))
                .toList();
        return MenuResponse.from(menu, appProperties.getFrontendUrl(), categoryResponses);
    }

    private Client requireOwnClient(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }

    private Client requireClient(String clientUuid) {
        return clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
    }
}
