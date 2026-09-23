package com.nfcplatform.destination.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.common.exception.NfcCardInactiveException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.entity.DestinationType;
import com.nfcplatform.menu.service.MenuService;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the single shared branch point between the NFC tap and QR scan redirect
 * hot paths (docs/NFC_FLOW.md). Pure Mockito, no Spring context/DB.
 */
class DestinationResolverServiceTest {

    @Mock
    private ProfileService profileService;
    @Mock
    private MenuService menuService;
    @Mock
    private GoogleReviewLocationRepository reviewLocationRepository;

    private DestinationResolverService resolverService;

    private static final long CLIENT_ID = 3L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolverService = new DestinationResolverService(profileService, menuService, reviewLocationRepository);
    }

    @Test
    void inactiveDestinationIsAlwaysRejectedRegardlessOfType() {
        Destination destination = destination(DestinationType.WEBSITE, "https://example.com");
        destination.setActive(false);

        assertThatThrownBy(() -> resolverService.resolve(destination, client(ClientType.BUSINESS)))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void profileTypeDelegatesToProfileService() {
        Destination destination = destination(DestinationType.PROFILE, null);
        Client client = client(ClientType.INDIVIDUAL);
        when(profileService.resolvePublicUrl(CLIENT_ID, ClientType.INDIVIDUAL))
                .thenReturn("https://app.example.com/p/jane");

        String result = resolverService.resolve(destination, client);

        assertThat(result).isEqualTo("https://app.example.com/p/jane");
    }

    @Test
    void unpublishedProfileMasksAsCardInactiveNotAsResourceNotFound() {
        Destination destination = destination(DestinationType.COMPANY_PROFILE, null);
        Client client = client(ClientType.BUSINESS);
        when(profileService.resolvePublicUrl(CLIENT_ID, ClientType.BUSINESS))
                .thenThrow(new ResourceNotFoundException("not published"));

        assertThatThrownBy(() -> resolverService.resolve(destination, client))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void vcardTypeDelegatesToProfileService() {
        Destination destination = destination(DestinationType.VCARD, null);
        Client client = client(ClientType.BUSINESS);
        when(profileService.resolveVCardUrl(CLIENT_ID, ClientType.BUSINESS))
                .thenReturn("https://api.example.com/api/v1/public/company/acme/vcard");

        String result = resolverService.resolve(destination, client);

        assertThat(result).isEqualTo("https://api.example.com/api/v1/public/company/acme/vcard");
    }

    @Test
    void unpublishedVcardProfileMasksAsCardInactive() {
        Destination destination = destination(DestinationType.VCARD, null);
        when(profileService.resolveVCardUrl(CLIENT_ID, ClientType.INDIVIDUAL))
                .thenThrow(new ResourceNotFoundException("not published"));

        assertThatThrownBy(() -> resolverService.resolve(destination, client(ClientType.INDIVIDUAL)))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void menuTypeDelegatesToMenuService() {
        Destination destination = destination(DestinationType.MENU, null);
        Client client = client(ClientType.BUSINESS);
        when(menuService.resolvePublicUrl(CLIENT_ID)).thenReturn("https://app.example.com/menu/cafe");

        String result = resolverService.resolve(destination, client);

        assertThat(result).isEqualTo("https://app.example.com/menu/cafe");
    }

    @Test
    void unpublishedMenuMasksAsCardInactive() {
        Destination destination = destination(DestinationType.MENU, null);
        Client client = client(ClientType.BUSINESS);
        when(menuService.resolvePublicUrl(CLIENT_ID)).thenThrow(new ResourceNotFoundException("not published"));

        assertThatThrownBy(() -> resolverService.resolve(destination, client))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void googleReviewTypeReturnsTheLocationsReviewUrlWhenActive() {
        Destination destination = destination(DestinationType.GOOGLE_REVIEW, null);
        destination.setGoogleReviewLocationId(55L);
        GoogleReviewLocation location = new GoogleReviewLocation();
        location.setGoogleReviewUrl("https://g.page/r/abc/review");
        location.setActive(true);
        when(reviewLocationRepository.findById(55L)).thenReturn(Optional.of(location));

        String result = resolverService.resolve(destination, client(ClientType.BUSINESS));

        assertThat(result).isEqualTo("https://g.page/r/abc/review");
    }

    @Test
    void googleReviewTypeRejectsAnInactiveLocation() {
        Destination destination = destination(DestinationType.GOOGLE_REVIEW, null);
        destination.setGoogleReviewLocationId(55L);
        GoogleReviewLocation inactive = new GoogleReviewLocation();
        inactive.setActive(false);
        when(reviewLocationRepository.findById(55L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> resolverService.resolve(destination, client(ClientType.BUSINESS)))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void urlBasedTypeReturnsItsStoredExternalUrl() {
        Destination destination = destination(DestinationType.WEBSITE, "https://example.com");

        String result = resolverService.resolve(destination, client(ClientType.BUSINESS));

        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    void urlBasedTypeWithNoStoredUrlIsRejected() {
        Destination destination = destination(DestinationType.CUSTOM_URL, null);

        assertThatThrownBy(() -> resolverService.resolve(destination, client(ClientType.BUSINESS)))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    private Destination destination(DestinationType type, String externalUrl) {
        Destination destination = new Destination();
        destination.setClientId(CLIENT_ID);
        destination.setName("Test destination");
        destination.setType(type);
        destination.setExternalUrl(externalUrl);
        destination.setActive(true);
        return destination;
    }

    private Client client(ClientType type) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setType(type);
        client.setDisplayName("Test Client");
        return client;
    }
}
