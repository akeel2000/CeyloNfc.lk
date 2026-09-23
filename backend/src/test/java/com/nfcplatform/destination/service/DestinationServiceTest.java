package com.nfcplatform.destination.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.destination.dto.DestinationCreateRequest;
import com.nfcplatform.destination.dto.DestinationResponse;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for destination-type dispatch - the type-specific validation and storage rules
 * DestinationService.create branches on. Pure Mockito, no Spring context/DB.
 */
class DestinationServiceTest {

    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private GoogleReviewLocationRepository reviewLocationRepository;

    private DestinationService destinationService;

    private static final long CLIENT_ID = 7L;
    private static final String CLIENT_UUID = "client-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        destinationService = new DestinationService(destinationRepository, clientRepository, reviewLocationRepository);
        when(destinationRepository.save(any(Destination.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createThrowsForAnUnrecognizedDestinationType() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.INDIVIDUAL)));

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Site", "NOT_A_TYPE", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsAProfileDestinationForABusinessClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Profile", "PROFILE", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAllowsAProfileDestinationForAnIndividualClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.INDIVIDUAL)));

        DestinationResponse response = destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Profile", "PROFILE", null, null));

        assertThat(response.type()).isEqualTo("PROFILE");
        assertThat(response.externalUrl()).isNull();
    }

    @Test
    void createRejectsACompanyProfileDestinationForAnIndividualClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.INDIVIDUAL)));

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Company", "COMPANY_PROFILE", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAllowsAMenuDestinationRegardlessOfClientType() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.INDIVIDUAL)));

        DestinationResponse response = destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Menu", "MENU", null, null));

        assertThat(response.type()).isEqualTo("MENU");
    }

    @Test
    void createRejectsAGoogleReviewDestinationWithNoLocationSpecified() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "Reviews", "GOOGLE_REVIEW", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsAGoogleReviewDestinationWhoseLocationDoesNotBelongToThisClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));
        when(reviewLocationRepository.findByUuidAndClientId("loc-uuid", CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "Reviews", "GOOGLE_REVIEW", null, "loc-uuid")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createStoresTheReviewLocationIdRatherThanAnExternalUrl() {
        GoogleReviewLocation location = new GoogleReviewLocation();
        location.setId(99L);
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));
        when(reviewLocationRepository.findByUuidAndClientId("loc-uuid", CLIENT_ID)).thenReturn(Optional.of(location));

        destinationService.create(new DestinationCreateRequest(CLIENT_UUID, "Reviews", "GOOGLE_REVIEW", null, "loc-uuid"));

        org.mockito.ArgumentCaptor<Destination> captor = org.mockito.ArgumentCaptor.forClass(Destination.class);
        org.mockito.Mockito.verify(destinationRepository).save(captor.capture());
        assertThat(captor.getValue().getGoogleReviewLocationId()).isEqualTo(99L);
        assertThat(captor.getValue().getExternalUrl()).isNull();
    }

    @Test
    void createRejectsAMalformedExternalUrlForAUrlBasedType() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));

        assertThatThrownBy(() -> destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Site", "WEBSITE", "not-a-url", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createStoresTheExternalUrlForAUrlBasedType() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));

        DestinationResponse response = destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Site", "WEBSITE", "https://example.com", null));

        assertThat(response.externalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void createAllowsAVcardDestinationForAnyClientType() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client(ClientType.BUSINESS)));

        DestinationResponse response = destinationService.create(
                new DestinationCreateRequest(CLIENT_UUID, "My Card", "VCARD", null, null));

        assertThat(response.type()).isEqualTo("VCARD");
        assertThat(response.externalUrl()).isNull();
    }

    @Test
    void listForClientThrowsForAnUnknownClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> destinationService.listForClient(CLIENT_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Client client(ClientType type) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setType(type);
        return client;
    }
}
