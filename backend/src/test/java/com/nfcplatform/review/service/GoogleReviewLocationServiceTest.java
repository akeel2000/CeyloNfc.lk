package com.nfcplatform.review.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.review.dto.GoogleReviewLocationRequest;
import com.nfcplatform.review.dto.GoogleReviewLocationResponse;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Google Review location CRUD and the shared DestinationUrlValidator reuse -
 * see docs/SECURITY.md's open-redirect protection note, which applies here too since
 * googleReviewUrl becomes a destination's external_url once a QR/NFC card points at it. Pure
 * Mockito, no Spring context/DB.
 */
class GoogleReviewLocationServiceTest {

    @Mock
    private GoogleReviewLocationRepository reviewLocationRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AuditService auditService;

    private GoogleReviewLocationService reviewLocationService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;
    private static final String CLIENT_UUID = "client-uuid";
    private static final String LOCATION_UUID = "location-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reviewLocationService = new GoogleReviewLocationService(reviewLocationRepository, clientRepository, auditService);
        when(reviewLocationRepository.save(any(GoogleReviewLocation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRejectsAMalformedReviewUrl() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> reviewLocationService.createForOwnClient(principal(),
                new GoogleReviewLocationRequest("My Business", null, null, null, "not-a-url", null)))
                .isInstanceOf(ValidationException.class);

        verify(reviewLocationRepository, never()).save(any());
    }

    @Test
    void createForClientResolvesByUuidRatherThanTheAuthenticatedUser() {
        Client client = client();
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));

        GoogleReviewLocationResponse response = reviewLocationService.createForClient(CLIENT_UUID,
                new GoogleReviewLocationRequest("My Business", null, null, null, "https://g.page/r/example", null),
                principal());

        assertThat(response.businessName()).isEqualTo("My Business");
        verify(clientRepository, never()).findByOwnerUserIdAndDeletedAtIsNull(any());
    }

    @Test
    void updateForOwnClientThrowsWhenTheLocationDoesNotBelongToThem() {
        Client client = client();
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.of(client));
        when(reviewLocationRepository.findByUuidAndClientId(LOCATION_UUID, CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewLocationService.updateForOwnClient(principal(), LOCATION_UUID,
                new GoogleReviewLocationRequest("My Business", null, null, null, "https://g.page/r/example", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateForAdminLooksUpTheLocationGloballyRatherThanScopedToAnyClient() {
        GoogleReviewLocation location = location();
        when(reviewLocationRepository.findByUuid(LOCATION_UUID)).thenReturn(Optional.of(location));

        GoogleReviewLocationResponse response = reviewLocationService.updateForAdmin(LOCATION_UUID,
                new GoogleReviewLocationRequest("Updated Name", null, null, null, "https://g.page/r/example", null),
                principal());

        assertThat(response.businessName()).isEqualTo("Updated Name");
        verify(reviewLocationRepository, never()).findByUuidAndClientId(any(), any());
    }

    @Test
    void updateRejectsAMalformedReviewUrlTooNotJustCreate() {
        GoogleReviewLocation location = location();
        when(reviewLocationRepository.findByUuid(LOCATION_UUID)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> reviewLocationService.updateForAdmin(LOCATION_UUID,
                new GoogleReviewLocationRequest("My Business", null, null, null, "javascript:alert(1)", null),
                principal()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void listForClientThrowsForAnUnknownClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewLocationService.listForClient(CLIENT_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void countAllDelegatesToTheRepositoryCount() {
        when(reviewLocationRepository.count()).thenReturn(4L);

        assertThat(reviewLocationService.countAll()).isEqualTo(4L);
    }

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }

    private GoogleReviewLocation location() {
        GoogleReviewLocation location = new GoogleReviewLocation();
        location.setId(1L);
        location.setClientId(CLIENT_ID);
        location.setBusinessName("Original Name");
        return location;
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(OWNER_USER_ID);
        user.setEmail("client@test.local");
        user.setPasswordHash("hash");
        Role role = new Role();
        role.setCode(RoleCode.CLIENT);
        role.setName(RoleCode.CLIENT.name());
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
