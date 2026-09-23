package com.nfcplatform.destination.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.destination.dto.DestinationCreateRequest;
import com.nfcplatform.destination.dto.DestinationResponse;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.entity.DestinationType;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final ClientRepository clientRepository;
    private final GoogleReviewLocationRepository reviewLocationRepository;

    @Transactional
    public DestinationResponse create(DestinationCreateRequest request) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(request.clientUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));

        DestinationType type = parseType(request.type());
        Long reviewLocationId = null;

        if (type == DestinationType.PROFILE || type == DestinationType.COMPANY_PROFILE || type == DestinationType.VCARD) {
            boolean matchesClientType = type == DestinationType.VCARD
                    || (type == DestinationType.PROFILE) == (client.getType() == ClientType.INDIVIDUAL);
            if (!matchesClientType) {
                throw new ValidationException(
                        "Destination type " + type + " does not match this client's type (" + client.getType() + ")");
            }
            // Resolved dynamically at redirect time from the client's own profile (see
            // ProfileService.resolvePublicUrl) - no external_url stored, so profile edits
            // never require touching the destination or the physical card.
        } else if (type == DestinationType.MENU) {
            // Resolved dynamically at redirect time from the client's own menu (see
            // MenuService.resolvePublicUrl), same pattern as PROFILE/COMPANY_PROFILE.
        } else if (type == DestinationType.GOOGLE_REVIEW) {
            if (request.googleReviewLocationUuid() == null || request.googleReviewLocationUuid().isBlank()) {
                throw new ValidationException("A Google Review location is required for this destination type");
            }
            GoogleReviewLocation location = reviewLocationRepository
                    .findByUuidAndClientId(request.googleReviewLocationUuid(), client.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Google Review location was not found for this client"));
            reviewLocationId = location.getId();
        } else if (type.isUrlBased()) {
            DestinationUrlValidator.validate(request.externalUrl());
        } else {
            throw new ValidationException(
                    "Destination type " + type + " is not yet supported - its module hasn't been built yet");
        }

        Destination destination = new Destination();
        destination.setClientId(client.getId());
        destination.setName(request.name());
        destination.setType(type);
        destination.setExternalUrl(type.isUrlBased() ? request.externalUrl() : null);
        destination.setGoogleReviewLocationId(reviewLocationId);
        destination = destinationRepository.save(destination);

        return DestinationResponse.from(destination);
    }

    @Transactional(readOnly = true)
    public List<DestinationResponse> listForClient(String clientUuid) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return destinationRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(DestinationResponse::from)
                .toList();
    }

    private DestinationType parseType(String value) {
        try {
            return DestinationType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid destination type: " + value);
        }
    }
}
