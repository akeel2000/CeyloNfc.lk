package com.nfcplatform.profile.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.profile.dto.ProfileResponse;
import com.nfcplatform.profile.dto.ProfileUpdateRequest;
import com.nfcplatform.profile.dto.PublicProfileResponse;
import com.nfcplatform.profile.entity.CompanyProfile;
import com.nfcplatform.profile.entity.IndividualProfile;
import com.nfcplatform.profile.repository.BusinessHourRepository;
import com.nfcplatform.profile.repository.CompanyProfileRepository;
import com.nfcplatform.profile.repository.IndividualProfileRepository;
import com.nfcplatform.profile.repository.SocialLinkRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.template.entity.Template;
import com.nfcplatform.template.entity.TemplateLayout;
import com.nfcplatform.template.repository.TemplateRepository;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
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
 * Unit tests for slug uniqueness, lazy profile creation, and public-profile resolution -
 * see docs/PROJECT_PROGRESS.md#post-roadmap-template-system for why lazy creation matters
 * (TemplateService.applyToOwnProfile depends on it). Pure Mockito, no Spring context/DB.
 */
class ProfileServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private IndividualProfileRepository individualProfileRepository;
    @Mock
    private CompanyProfileRepository companyProfileRepository;
    @Mock
    private SocialLinkRepository socialLinkRepository;
    @Mock
    private BusinessHourRepository businessHourRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private AnalyticsService analyticsService;

    private ProfileService profileService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        profileService = new ProfileService(clientRepository, individualProfileRepository, companyProfileRepository,
                socialLinkRepository, businessHourRepository, templateRepository, appProperties, analyticsService);

        when(socialLinkRepository.findAllByClientIdOrderByDisplayOrderAsc(any())).thenReturn(Collections.emptyList());
        when(businessHourRepository.findAllByCompanyProfileIdOrderByDayOfWeekAsc(any())).thenReturn(Collections.emptyList());
        when(individualProfileRepository.save(any(IndividualProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getOrCreateIndividualSlugifiesTheClientDisplayNameWhenNoProfileExistsYet() {
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(individualProfileRepository.existsBySlug("jane-doe")).thenReturn(false);

        IndividualProfile profile = profileService.getOrCreateIndividual(client);

        assertThat(profile.getSlug()).isEqualTo("jane-doe");
        assertThat(profile.getFullName()).isEqualTo("Jane Doe");
        verify(individualProfileRepository).save(profile);
    }

    @Test
    void getOrCreateIndividualAppendsASuffixWhenTheSlugIsAlreadyTaken() {
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(individualProfileRepository.existsBySlug("jane-doe")).thenReturn(true);
        when(individualProfileRepository.existsBySlug("jane-doe-2")).thenReturn(true);
        when(individualProfileRepository.existsBySlug("jane-doe-3")).thenReturn(false);

        IndividualProfile profile = profileService.getOrCreateIndividual(client);

        assertThat(profile.getSlug()).isEqualTo("jane-doe-3");
    }

    @Test
    void getOrCreateIndividualReturnsTheExistingProfileWithoutTouchingSlugGenerationOrSaving() {
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        IndividualProfile existing = new IndividualProfile();
        existing.setSlug("already-set");
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(existing));

        IndividualProfile profile = profileService.getOrCreateIndividual(client);

        assertThat(profile).isSameAs(existing);
        verify(individualProfileRepository, never()).existsBySlug(any());
        verify(individualProfileRepository, never()).save(any());
    }

    @Test
    void updateProfileRejectsABlankFullNameForAnIndividualClient() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(individualProfile()));
        when(individualProfileRepository.existsBySlugAndClientIdNot(any(), eq(CLIENT_ID))).thenReturn(false);

        ProfileUpdateRequest request = updateRequest("my-slug", "   ");

        assertThatThrownBy(() -> profileService.updateOwnProfile(actor, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Full name");
    }

    @Test
    void updateProfileRejectsASlugAlreadyTakenByAnotherClient() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(individualProfile()));
        when(individualProfileRepository.existsBySlugAndClientIdNot("taken-slug", CLIENT_ID)).thenReturn(true);

        ProfileUpdateRequest request = updateRequest("taken-slug", "Jane Doe");

        assertThatThrownBy(() -> profileService.updateOwnProfile(actor, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void updateProfileSavesAllSuppliedFieldsForAnIndividualClient() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.INDIVIDUAL, "Jane Doe");
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(individualProfile()));
        when(individualProfileRepository.existsBySlugAndClientIdNot(any(), eq(CLIENT_ID))).thenReturn(false);

        ProfileResponse response = profileService.updateOwnProfile(actor, updateRequest("new-slug", "Jane Doe"));

        assertThat(response.slug()).isEqualTo("new-slug");
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.publicUrl()).isEqualTo("https://ceylonfc.com/p/new-slug");
    }

    @Test
    void updateProfileRejectsABlankCompanyNameForABusinessClient() {
        UserPrincipal actor = principal();
        Client client = client(ClientType.BUSINESS, "Acme Ltd");
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(companyProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(companyProfile()));
        when(companyProfileRepository.existsBySlugAndClientIdNot(any(), eq(CLIENT_ID))).thenReturn(false);

        ProfileUpdateRequest request = new ProfileUpdateRequest("acme-slug", null, null, "", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> profileService.updateOwnProfile(actor, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Company name");
    }

    @Test
    void hasPublishedProfileReflectsThePublishedFlagForAnIndividualProfile() {
        IndividualProfile published = new IndividualProfile();
        published.setPublished(true);
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(published));

        assertThat(profileService.hasPublishedProfile(CLIENT_ID, ClientType.INDIVIDUAL)).isTrue();
    }

    @Test
    void hasPublishedProfileIsFalseWhenNoProfileRowExistsAtAll() {
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertThat(profileService.hasPublishedProfile(CLIENT_ID, ClientType.INDIVIDUAL)).isFalse();
    }

    @Test
    void resolvePublicUrlThrowsWhenTheProfileIsNotPublished() {
        IndividualProfile unpublished = new IndividualProfile();
        unpublished.setPublished(false);
        when(individualProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(unpublished));

        assertThatThrownBy(() -> profileService.resolvePublicUrl(CLIENT_ID, ClientType.INDIVIDUAL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolvePublicUrlBuildsTheCompanyPathForABusinessClient() {
        CompanyProfile published = new CompanyProfile();
        published.setPublished(true);
        published.setSlug("acme-ltd");
        when(companyProfileRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(published));

        assertThat(profileService.resolvePublicUrl(CLIENT_ID, ClientType.BUSINESS))
                .isEqualTo("https://ceylonfc.com/company/acme-ltd");
    }

    @Test
    void getPublicIndividualProfileThrowsForAnUnknownOrUnpublishedSlug() {
        when(individualProfileRepository.findBySlugAndPublishedTrue("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getPublicIndividualProfile("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublicIndividualProfileIncludesTheAppliedTemplatesThemeWhenOneIsSet() {
        IndividualProfile profile = new IndividualProfile();
        profile.setSlug("jane-doe");
        profile.setPublished(true);
        profile.setTemplateId(5L);
        Template template = new Template();
        template.setPrimaryColor("#b45309");
        template.setLayout(TemplateLayout.MINIMAL);
        when(individualProfileRepository.findBySlugAndPublishedTrue("jane-doe")).thenReturn(Optional.of(profile));
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));

        PublicProfileResponse response = profileService.getPublicIndividualProfile("jane-doe");

        assertThat(response.templatePrimaryColor()).isEqualTo("#b45309");
        assertThat(response.templateLayout()).isEqualTo("MINIMAL");
    }

    @Test
    void getPublicIndividualProfileOmitsTemplateThemeWhenNoneIsApplied() {
        IndividualProfile profile = new IndividualProfile();
        profile.setSlug("jane-doe");
        profile.setPublished(true);
        when(individualProfileRepository.findBySlugAndPublishedTrue("jane-doe")).thenReturn(Optional.of(profile));

        PublicProfileResponse response = profileService.getPublicIndividualProfile("jane-doe");

        assertThat(response.templatePrimaryColor()).isNull();
        assertThat(response.templateLayout()).isNull();
        verify(templateRepository, never()).findById(any());
    }

    @Test
    void recordIndividualProfileViewSilentlyNoOpsForAnUnknownSlug() {
        when(individualProfileRepository.findBySlugAndPublishedTrue("unknown")).thenReturn(Optional.empty());

        profileService.recordIndividualProfileView("unknown", "some-agent", "https://ref.example");

        verifyNoInteractions(analyticsService);
    }

    @Test
    void recordIndividualProfileViewRecordsAnEventForTheProfilesOwningClient() {
        IndividualProfile profile = new IndividualProfile();
        profile.setClientId(CLIENT_ID);
        when(individualProfileRepository.findBySlugAndPublishedTrue("jane-doe")).thenReturn(Optional.of(profile));

        profileService.recordIndividualProfileView("jane-doe", "some-agent", "https://ref.example");

        ArgumentCaptor<Long> clientIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(analyticsService).recordEvent(clientIdCaptor.capture(), eq(null), eq(null), eq(null),
                eq(AnalyticsEventType.PROFILE_VIEW), eq("some-agent"), eq("https://ref.example"));
        assertThat(clientIdCaptor.getValue()).isEqualTo(CLIENT_ID);
    }

    private IndividualProfile individualProfile() {
        IndividualProfile profile = new IndividualProfile();
        profile.setClientId(CLIENT_ID);
        return profile;
    }

    private CompanyProfile companyProfile() {
        CompanyProfile profile = new CompanyProfile();
        profile.setClientId(CLIENT_ID);
        return profile;
    }

    private ProfileUpdateRequest updateRequest(String slug, String fullName) {
        return new ProfileUpdateRequest(slug, fullName, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
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

    private Client client(ClientType type, String displayName) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setType(type);
        client.setDisplayName(displayName);
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }
}
