package com.nfcplatform.menu.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.menu.dto.MenuCategoryRequest;
import com.nfcplatform.menu.dto.MenuItemRequest;
import com.nfcplatform.menu.dto.MenuResponse;
import com.nfcplatform.menu.dto.MenuUpdateRequest;
import com.nfcplatform.menu.entity.Menu;
import com.nfcplatform.menu.entity.MenuCategory;
import com.nfcplatform.menu.repository.MenuCategoryRepository;
import com.nfcplatform.menu.repository.MenuItemRepository;
import com.nfcplatform.menu.repository.MenuRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MenuService's own-vs-admin duality added in
 * docs/PROJECT_PROGRESS.md#post-roadmap-admin-sidebar-audit - every mutating method now has a
 * private Client-based core plus two public entry points (own client / any client by uuid for
 * admin), following ProfileService's established pattern. These tests exist to prove that
 * refactor didn't silently change behavior for either path. Pure Mockito, no Spring context/DB.
 */
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;
    @Mock
    private MenuCategoryRepository categoryRepository;
    @Mock
    private MenuItemRepository itemRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AnalyticsService analyticsService;

    private MenuService menuService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;
    private static final String CLIENT_UUID = "client-uuid";
    private static final String CATEGORY_UUID = "category-uuid";
    private static final String ITEM_UUID = "item-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        menuService = new MenuService(menuRepository, categoryRepository, itemRepository, clientRepository,
                appProperties, analyticsService);

        when(categoryRepository.findAllByMenuIdOrderBySortOrderAsc(any())).thenReturn(Collections.emptyList());
        when(menuRepository.save(any(Menu.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getOrCreateOwnMenuSlugifiesTheClientDisplayNameWhenNoneExistsYet() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(menuRepository.existsBySlug("test-client")).thenReturn(false);

        MenuResponse response = menuService.getOrCreateOwnMenu(principal());

        assertThat(response.slug()).isEqualTo("test-client");
        assertThat(response.name()).isEqualTo("Test Client Menu");
    }

    @Test
    void getOrCreateMenuForClientResolvesByUuidRatherThanTheAuthenticatedUser() {
        Client client = client();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));

        MenuResponse response = menuService.getOrCreateMenuForClient(CLIENT_UUID);

        assertThat(response.slug()).isEqualTo("existing-slug");
        verify(clientRepository, never()).findByOwnerUserIdAndDeletedAtIsNull(any());
    }

    @Test
    void updateOwnMenuRejectsASlugAlreadyTakenByAnotherClient() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(menuRepository.existsBySlugAndClientIdNot("taken-slug", CLIENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> menuService.updateOwnMenu(principal(),
                new MenuUpdateRequest("taken-slug", "My Menu", null, null, "LKR")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateMenuForClientSavesTheSuppliedFieldsThroughTheAdminPath() {
        Client client = client();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(menuRepository.existsBySlugAndClientIdNot(any(), eq(CLIENT_ID))).thenReturn(false);

        MenuResponse response = menuService.updateMenuForClient(CLIENT_UUID,
                new MenuUpdateRequest("new-slug", "Updated Menu", "A description", null, "USD"));

        assertThat(response.slug()).isEqualTo("new-slug");
        assertThat(response.name()).isEqualTo("Updated Menu");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void setPublishedTogglesTheFlagForTheOwnClientPath() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));

        MenuResponse response = menuService.setPublished(principal(), true);

        assertThat(response.published()).isTrue();
    }

    @Test
    void setPublishedForClientTogglesTheFlagForTheAdminPath() {
        Client client = client();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));

        MenuResponse response = menuService.setPublishedForClient(CLIENT_UUID, true);

        assertThat(response.published()).isTrue();
    }

    @Test
    void createCategoryForClientDefaultsSortOrderToTheCurrentCategoryCount() {
        Client client = client();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(categoryRepository.findAllByMenuIdOrderBySortOrderAsc(existingMenu().getId()))
                .thenReturn(List.of(new MenuCategory(), new MenuCategory()));

        menuService.createCategoryForClient(CLIENT_UUID, new MenuCategoryRequest("Drinks", null, null));

        org.mockito.ArgumentCaptor<MenuCategory> captor = org.mockito.ArgumentCaptor.forClass(MenuCategory.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getSortOrder()).isEqualTo(2);
    }

    @Test
    void updateCategoryThrowsForAnUnknownCategoryUuid() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(categoryRepository.findByUuidAndMenuId(CATEGORY_UUID, existingMenu().getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.updateCategory(principal(), CATEGORY_UUID,
                new MenuCategoryRequest("Renamed", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCategoryForClientRemovesItThroughTheAdminPath() {
        Client client = client();
        MenuCategory category = category();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(categoryRepository.findByUuidAndMenuId(CATEGORY_UUID, existingMenu().getId())).thenReturn(Optional.of(category));

        menuService.deleteCategoryForClient(CLIENT_UUID, CATEGORY_UUID);

        verify(categoryRepository).delete(category);
    }

    @Test
    void createItemThrowsForAnUnknownCategoryUuid() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(categoryRepository.findByUuidAndMenuId(CATEGORY_UUID, existingMenu().getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.createItem(principal(), CATEGORY_UUID,
                new MenuItemRequest("Fish Cutlets", null, null, BigDecimal.TEN, true, false, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createItemForClientSucceedsThroughTheAdminPath() {
        Client client = client();
        MenuCategory category = category();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existingMenu()));
        when(categoryRepository.findByUuidAndMenuId(CATEGORY_UUID, existingMenu().getId())).thenReturn(Optional.of(category));
        when(itemRepository.findAllByCategoryIdOrderBySortOrderAsc(category.getId())).thenReturn(Collections.emptyList());

        menuService.createItemForClient(CLIENT_UUID, CATEGORY_UUID,
                new MenuItemRequest("Fish Cutlets", null, null, new BigDecimal("450.00"), true, false, null));

        verify(itemRepository).save(any());
    }

    @Test
    void hasPublishedMenuIsFalseWhenNoMenuRowExistsAtAll() {
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertThat(menuService.hasPublishedMenu(CLIENT_ID)).isFalse();
    }

    @Test
    void resolvePublicUrlThrowsWhenTheMenuIsNotPublished() {
        Menu unpublished = existingMenu();
        unpublished.setPublished(false);
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(unpublished));

        assertThatThrownBy(() -> menuService.resolvePublicUrl(CLIENT_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolvePublicUrlBuildsTheMenuPathWhenPublished() {
        Menu published = existingMenu();
        published.setPublished(true);
        when(menuRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(published));

        assertThat(menuService.resolvePublicUrl(CLIENT_ID)).isEqualTo("https://ceylonfc.com/menu/existing-slug");
    }

    @Test
    void recordMenuViewSilentlyNoOpsForAnUnknownSlug() {
        when(menuRepository.findBySlugAndPublishedTrue("unknown")).thenReturn(Optional.empty());

        menuService.recordMenuView("unknown", "some-agent", "https://ref.example");

        verifyNoInteractions(analyticsService);
    }

    @Test
    void recordMenuViewRecordsAnEventForTheMenusOwningClient() {
        Menu menu = existingMenu();
        when(menuRepository.findBySlugAndPublishedTrue("existing-slug")).thenReturn(Optional.of(menu));

        menuService.recordMenuView("existing-slug", "some-agent", "https://ref.example");

        verify(analyticsService).recordEvent(eq(CLIENT_ID), eq(null), eq(null), eq(null),
                eq(AnalyticsEventType.MENU_VIEW), eq("some-agent"), eq("https://ref.example"));
    }

    private Menu existingMenu() {
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setClientId(CLIENT_ID);
        menu.setSlug("existing-slug");
        menu.setName("My Menu");
        menu.setCurrency("LKR");
        return menu;
    }

    private MenuCategory category() {
        MenuCategory category = new MenuCategory();
        category.setId(1L);
        category.setMenuId(1L);
        category.setName("Starters");
        return category;
    }

    private UserPrincipal principal() {
        Role role = new Role();
        role.setCode(RoleCode.CLIENT);
        role.setName(RoleCode.CLIENT.name());

        User user = new User();
        user.setId(OWNER_USER_ID);
        user.setEmail("client@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setType(ClientType.INDIVIDUAL);
        client.setDisplayName("Test Client");
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }
}
