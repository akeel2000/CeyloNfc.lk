package com.nfcplatform.review.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.destination.service.DestinationUrlValidator;
import com.nfcplatform.review.dto.GoogleReviewLocationRequest;
import com.nfcplatform.review.dto.GoogleReviewLocationResponse;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleReviewLocationService {

    private final GoogleReviewLocationRepository reviewLocationRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Transactional
    public GoogleReviewLocationResponse createForOwnClient(UserPrincipal actor, GoogleReviewLocationRequest request) {
        return create(requireOwnClient(actor), request, actor);
    }

    @Transactional
    public GoogleReviewLocationResponse createForClient(String clientUuid, GoogleReviewLocationRequest request, UserPrincipal actor) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return create(client, request, actor);
    }

    private GoogleReviewLocationResponse create(Client client, GoogleReviewLocationRequest request, UserPrincipal actor) {
        DestinationUrlValidator.validate(request.googleReviewUrl());

        GoogleReviewLocation location = new GoogleReviewLocation();
        location.setClientId(client.getId());
        apply(location, request);
        location = reviewLocationRepository.save(location);

        auditService.record(actor.getId(), "GOOGLE_REVIEW_CREATE", "GoogleReviewLocation", location.getUuid(), null, null);
        return GoogleReviewLocationResponse.from(location);
    }

    @Transactional(readOnly = true)
    public List<GoogleReviewLocationResponse> listForOwnClient(UserPrincipal actor) {
        Client client = requireOwnClient(actor);
        return reviewLocationRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(GoogleReviewLocationResponse::from)
                .toList();
    }

    @Transactional
    public GoogleReviewLocationResponse updateForOwnClient(UserPrincipal actor, String uuid, GoogleReviewLocationRequest request) {
        Client client = requireOwnClient(actor);
        GoogleReviewLocation location = reviewLocationRepository.findByUuidAndClientId(uuid, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Google Review location was not found"));
        return update(location, request, actor);
    }

    @Transactional
    public GoogleReviewLocationResponse updateForAdmin(String uuid, GoogleReviewLocationRequest request, UserPrincipal actor) {
        GoogleReviewLocation location = reviewLocationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Google Review location was not found"));
        return update(location, request, actor);
    }

    private GoogleReviewLocationResponse update(GoogleReviewLocation location, GoogleReviewLocationRequest request, UserPrincipal actor) {
        DestinationUrlValidator.validate(request.googleReviewUrl());
        apply(location, request);
        location = reviewLocationRepository.save(location);

        auditService.record(actor.getId(), "GOOGLE_REVIEW_UPDATE", "GoogleReviewLocation", location.getUuid(), null, null);
        return GoogleReviewLocationResponse.from(location);
    }

    @Transactional(readOnly = true)
    public List<GoogleReviewLocationResponse> listForClient(String clientUuid) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return reviewLocationRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(GoogleReviewLocationResponse::from)
                .toList();
    }

    /** Platform-wide total for the admin dashboard KPI card - not scoped to any one client. */
    @Transactional(readOnly = true)
    public long countAll() {
        return reviewLocationRepository.count();
    }

    private void apply(GoogleReviewLocation location, GoogleReviewLocationRequest request) {
        location.setBusinessName(request.businessName());
        location.setLocationName(request.locationName());
        location.setAddress(request.address());
        location.setGoogleMapsUrl(request.googleMapsUrl());
        location.setGoogleReviewUrl(request.googleReviewUrl());
        location.setGooglePlaceId(request.googlePlaceId());
    }

    private Client requireOwnClient(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }
}
