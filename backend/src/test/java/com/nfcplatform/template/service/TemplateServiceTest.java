package com.nfcplatform.template.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.profile.entity.CompanyProfile;
import com.nfcplatform.profile.entity.IndividualProfile;
import com.nfcplatform.profile.repository.CompanyProfileRepository;
import com.nfcplatform.profile.repository.IndividualProfileRepository;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.service.SubscriptionService;
import com.nfcplatform.template.dto.TemplateGalleryItem;
import com.nfcplatform.template.entity.Template;
import com.nfcplatform.template.entity.TemplateLayout;
import com.nfcplatform.template.repository.TemplateRepository;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the premium-template gate described in
 * docs/PROJECT_PROGRESS.md#post-roadmap-template-system - reuses the same "no subscription =
 * unmanaged/unlimited" precedent as SubscriptionService.assertCanAssignCard, enforced
 * server-side in applyToOwnProfile, not just hidden in the gallery's `locked` flag. Pure
 * Mockito, no Spring context/DB.
 */
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private IndividualProfileRepository individualProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private ProfileService profileService;
    @Mock
    private SubscriptionService subscriptionService;

    private TemplateService templateService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;
    private static final String TEMPLATE_UUID = "template-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        templateService = new TemplateService(templateRepository, clientRepository, individualProfileRepository,
                companyProfileRepository, profileService, subscriptionService);
    }

    @Test
    void applyingAFreeTemplateNeverConsultsSubscriptionAccess() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template freeTemplate = template(false, true);
        IndividualProfile profile = new IndividualProfile();

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.of(freeTemplate));
        when(profileService.getOrCreateIndividual(client)).thenReturn(profile);

        templateService.applyToOwnProfile(actor, TEMPLATE_UUID);

        assertThat(profile.getTemplateId()).isEqualTo(freeTemplate.getId());
        verify(subscriptionService, org.mockito.Mockito.never()).clientHasPremiumTemplateAccess(any());
    }

    @Test
    void applyingAPremiumTemplateIsRejectedWhenTheClientLacksAccess() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template premiumTemplate = template(true, true);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.of(premiumTemplate));
        when(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> templateService.applyToOwnProfile(actor, TEMPLATE_UUID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("premium");

        verify(profileService, org.mockito.Mockito.never()).getOrCreateIndividual(any());
    }

    @Test
    void applyingAPremiumTemplateSucceedsWhenTheClientHasAccess() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template premiumTemplate = template(true, true);
        IndividualProfile profile = new IndividualProfile();

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.of(premiumTemplate));
        when(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).thenReturn(true);
        when(profileService.getOrCreateIndividual(client)).thenReturn(profile);

        templateService.applyToOwnProfile(actor, TEMPLATE_UUID);

        assertThat(profile.getTemplateId()).isEqualTo(premiumTemplate.getId());
    }

    @Test
    void applyingAnInactiveTemplateIsRejectedEvenIfItWasSelectableWhenActive() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template deactivatedTemplate = template(false, false);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.of(deactivatedTemplate));

        assertThatThrownBy(() -> templateService.applyToOwnProfile(actor, TEMPLATE_UUID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void applyingToAClientWithNoProfileYetLazilyCreatesOneInsteadOfFailing() {
        // Regression test: a client who never once loaded/saved their own profile page has no
        // individual_profiles row yet (rows are created lazily by ProfileService.getOrCreate*),
        // so applyToOwnProfile must go through that same lazy path rather than a throwing lookup.
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template freeTemplate = template(false, true);
        IndividualProfile lazilyCreatedProfile = new IndividualProfile();

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.of(freeTemplate));
        when(profileService.getOrCreateIndividual(client)).thenReturn(lazilyCreatedProfile);

        templateService.applyToOwnProfile(actor, TEMPLATE_UUID);

        assertThat(lazilyCreatedProfile.getTemplateId()).isEqualTo(freeTemplate.getId());
        verify(individualProfileRepository).save(lazilyCreatedProfile);
    }

    @Test
    void applyingToAKnownTemplateUuidThrowsResourceNotFound() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(templateRepository.findByUuid(TEMPLATE_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.applyToOwnProfile(actor, TEMPLATE_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void galleryMarksAPremiumTemplateLockedWhenTheClientLacksAccess() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template freeTemplate = template(false, true);
        Template premiumTemplate = template(true, true);

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).thenReturn(false);
        when(templateRepository.findAllByActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(freeTemplate, premiumTemplate));
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        List<TemplateGalleryItem> gallery = templateService.listGalleryForOwnClient(actor);

        assertThat(gallery).hasSize(2);
        assertThat(gallery.get(0).locked()).isFalse();
        assertThat(gallery.get(1).locked()).isTrue();
    }

    @Test
    void galleryMarksTheCurrentlyAppliedTemplateAsSelected() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL);
        Template appliedTemplate = template(false, true);
        Template otherTemplate = template(false, true);
        IndividualProfile profile = new IndividualProfile();
        profile.setTemplateId(appliedTemplate.getId());

        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).thenReturn(true);
        when(templateRepository.findAllByActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(appliedTemplate, otherTemplate));
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(profile));

        List<TemplateGalleryItem> gallery = templateService.listGalleryForOwnClient(actor);

        assertThat(gallery).filteredOn(TemplateGalleryItem::selected).hasSize(1);
        assertThat(gallery.stream().filter(TemplateGalleryItem::selected).findFirst().orElseThrow().template().uuid())
                .isEqualTo(appliedTemplate.getUuid());
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

    private Client client(ClientType type) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setType(type);
        client.setDisplayName("Test Client");
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }

    private static long nextTemplateId = 1;

    private Template template(boolean premium, boolean active) {
        Template template = new Template();
        template.setId(nextTemplateId++);
        template.setName("Template " + template.getId());
        template.setPrimaryColor("#4338ca");
        template.setLayout(TemplateLayout.CLASSIC);
        template.setPremium(premium);
        template.setActive(active);
        return template;
    }
}
