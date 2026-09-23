package com.nfcplatform.destination.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.common.exception.NfcCardInactiveException;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.entity.DestinationType;
import com.nfcplatform.menu.service.MenuService;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.review.entity.GoogleReviewLocation;
import com.nfcplatform.review.repository.GoogleReviewLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.nfcplatform.common.exception.ResourceNotFoundException;

/**
 * Resolves a Destination to a concrete target URL, shared by the NFC tap and QR scan
 * redirect hot paths (docs/NFC_FLOW.md) so this branching logic exists in exactly one
 * place. Throws NfcCardInactiveException (reused generically - it maps to the same "this
 * link isn't usable right now" 410 response for both NFC and QR) when the destination's
 * backing resource (profile, review location) isn't in a redirectable state.
 */
@Service
@RequiredArgsConstructor
public class DestinationResolverService {

    private final ProfileService profileService;
    private final MenuService menuService;
    private final GoogleReviewLocationRepository reviewLocationRepository;

    public String resolve(Destination destination, Client client) {
        if (!destination.isActive()) {
            throw new NfcCardInactiveException("This link has no active destination");
        }

        if (destination.getType() == DestinationType.PROFILE || destination.getType() == DestinationType.COMPANY_PROFILE) {
            try {
                return profileService.resolvePublicUrl(client.getId(), client.getType());
            } catch (ResourceNotFoundException e) {
                throw new NfcCardInactiveException("This link's profile is not published yet");
            }
        }

        if (destination.getType() == DestinationType.VCARD) {
            try {
                return profileService.resolveVCardUrl(client.getId(), client.getType());
            } catch (ResourceNotFoundException e) {
                throw new NfcCardInactiveException("This link's profile is not published yet");
            }
        }

        if (destination.getType() == DestinationType.MENU) {
            try {
                return menuService.resolvePublicUrl(client.getId());
            } catch (ResourceNotFoundException e) {
                throw new NfcCardInactiveException("This link's menu is not published yet");
            }
        }

        if (destination.getType() == DestinationType.GOOGLE_REVIEW) {
            GoogleReviewLocation location = reviewLocationRepository.findById(destination.getGoogleReviewLocationId())
                    .filter(GoogleReviewLocation::isActive)
                    .orElseThrow(() -> new NfcCardInactiveException("This link's review destination is unavailable"));
            return location.getGoogleReviewUrl();
        }

        if (destination.getExternalUrl() == null) {
            throw new NfcCardInactiveException("This link has no active destination");
        }
        return destination.getExternalUrl();
    }
}
