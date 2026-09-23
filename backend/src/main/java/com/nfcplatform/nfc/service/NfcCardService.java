package com.nfcplatform.nfc.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.NfcCardInactiveException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.destination.service.DestinationResolverService;
import com.nfcplatform.nfc.dto.*;
import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.entity.NfcCardStatus;
import com.nfcplatform.nfc.repository.NfcCardRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NfcCardService {

    private final NfcCardRepository nfcCardRepository;
    private final ClientRepository clientRepository;
    private final DestinationRepository destinationRepository;
    private final NfcTokenService nfcTokenService;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final DestinationResolverService destinationResolverService;
    private final AnalyticsService analyticsService;
    private final SubscriptionService subscriptionService;

    @Transactional
    public NfcCardRegisterResponse register(NfcCardRegisterRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        if (nfcCardRepository.existsBySerialNumber(request.serialNumber())) {
            throw new ConflictException("A card with this serial number is already registered");
        }

        String rawToken = nfcTokenService.generateRawToken();

        NfcCard card = new NfcCard();
        card.setSerialNumber(request.serialNumber());
        card.setTokenHash(nfcTokenService.hash(rawToken));
        card.setNotes(request.notes());
        card.setStatus(NfcCardStatus.UNASSIGNED);
        card = nfcCardRepository.save(card);

        auditService.record(actor.getId(), "NFC_CREATE", "NfcCard", card.getUuid(),
                clientIp(httpRequest), Map.of("serialNumber", card.getSerialNumber()));

        String publicUrl = appProperties.getFrontendUrl() + "/t/" + rawToken;
        return new NfcCardRegisterResponse(toResponse(card), rawToken, publicUrl);
    }

    @Transactional(readOnly = true)
    public PageResponse<NfcCardResponse> list(String status, String clientUuid, String search, Pageable pageable) {
        NfcCardStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        Long clientId = null;
        if (clientUuid != null && !clientUuid.isBlank()) {
            clientId = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Client was not found"))
                    .getId();
        }
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<NfcCard> page = nfcCardRepository.search(statusFilter, clientId, searchPattern, pageable);
        return enrich(page);
    }

    @Transactional(readOnly = true)
    public NfcCardResponse get(String uuid) {
        NfcCard card = nfcCardRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));
        return toResponse(card);
    }

    @Transactional
    public NfcCardResponse assign(String uuid, NfcCardAssignRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        NfcCard card = nfcCardRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));

        Client client = clientRepository.findByUuidAndDeletedAtIsNull(request.clientUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        Destination destination = destinationRepository.findByUuidAndClientId(request.destinationUuid(), client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination was not found for this client"));

        boolean isNewAssignmentForClient = card.getClientId() == null || !card.getClientId().equals(client.getId());
        if (isNewAssignmentForClient) {
            subscriptionService.assertCanAssignCard(client.getId());
        }

        card.setClientId(client.getId());
        card.setDestinationId(destination.getId());
        card.setStatus(NfcCardStatus.ACTIVE);
        if (card.getActivatedAt() == null) {
            card.setActivatedAt(Instant.now());
        }
        card = nfcCardRepository.save(card);

        auditService.record(actor.getId(), "NFC_ASSIGN", "NfcCard", card.getUuid(), clientIp(httpRequest),
                Map.of("clientUuid", client.getUuid(), "destinationUuid", destination.getUuid()));

        return toResponse(card);
    }

    /**
     * Replaces a lost/damaged physical card without disturbing the client's destination -
     * the new card inherits the old card's clientId/destinationId directly (not through
     * {@link #assign}, so this deliberately does not re-run
     * {@link SubscriptionService#assertCanAssignCard} - a like-for-like swap isn't a new
     * assignment against the plan's card limit). The old card is marked REPLACED rather than
     * deleted so its tap history and audit trail survive; {@link #resolveRedirectTarget}
     * already treats any non-ACTIVE card as inactive, so the old token stops resolving the
     * moment this runs, with no separate "deactivate" step needed.
     */
    @Transactional
    public NfcCardRegisterResponse replace(String uuid, NfcCardReplaceRequest request, UserPrincipal actor, HttpServletRequest httpRequest) {
        NfcCard oldCard = nfcCardRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));
        if (oldCard.getClientId() == null || oldCard.getDestinationId() == null) {
            throw new ValidationException("This card has no client/destination assignment to carry over - register a new card instead");
        }
        if (nfcCardRepository.existsBySerialNumber(request.newSerialNumber())) {
            throw new ConflictException("A card with this serial number is already registered");
        }

        String rawToken = nfcTokenService.generateRawToken();

        NfcCard newCard = new NfcCard();
        newCard.setSerialNumber(request.newSerialNumber());
        newCard.setTokenHash(nfcTokenService.hash(rawToken));
        newCard.setNotes(request.notes());
        newCard.setClientId(oldCard.getClientId());
        newCard.setDestinationId(oldCard.getDestinationId());
        newCard.setStatus(NfcCardStatus.ACTIVE);
        newCard.setActivatedAt(Instant.now());
        newCard = nfcCardRepository.save(newCard);

        oldCard.setStatus(NfcCardStatus.REPLACED);
        nfcCardRepository.save(oldCard);

        auditService.record(actor.getId(), "NFC_REPLACE", "NfcCard", oldCard.getUuid(), clientIp(httpRequest),
                Map.of("newCardUuid", newCard.getUuid(), "newSerialNumber", newCard.getSerialNumber()));

        String publicUrl = appProperties.getFrontendUrl() + "/t/" + rawToken;
        return new NfcCardRegisterResponse(toResponse(newCard), rawToken, publicUrl);
    }

    @Transactional
    public NfcCardResponse setStatus(String uuid, String newStatus, UserPrincipal actor, HttpServletRequest httpRequest) {
        NfcCard card = nfcCardRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));
        NfcCardStatus status = parseStatus(newStatus);

        if (status == NfcCardStatus.ACTIVE && (card.getClientId() == null || card.getDestinationId() == null)) {
            throw new ValidationException("Card must be assigned to a client and destination before activation");
        }

        card.setStatus(status);
        card = nfcCardRepository.save(card);

        auditService.record(actor.getId(), "NFC_STATUS_CHANGE", "NfcCard", card.getUuid(),
                clientIp(httpRequest), Map.of("status", status.name()));

        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public List<NfcCardResponse> listForOwnClient(UserPrincipal actor) {
        Client client = clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
        return nfcCardRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NfcCardResponse getForOwnClient(String uuid, UserPrincipal actor) {
        Client client = clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
        // Tenant-safe: findByUuidAndClientId, never findByUuid followed by an after-the-fact check.
        NfcCard card = nfcCardRepository.findByUuidAndClientId(uuid, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));
        return toResponse(card);
    }

    /**
     * The redirect hot path. Loads only what's needed to resolve a target URL - never the
     * full Client/Profile aggregate (see docs/NFC_FLOW.md). The per-card counter is updated
     * synchronously (cheap, single-row); the detailed analytics event (device/browser/OS,
     * for the dashboards) is recorded via AnalyticsService.recordEvent, which is @Async so
     * it never delays the redirect response.
     */
    @Transactional
    public String resolveRedirectTarget(String rawToken, String userAgent, String referrer) {
        if (!nfcTokenService.isValidFormat(rawToken)) {
            throw new ResourceNotFoundException("NFC card was not found");
        }

        NfcCard card = nfcCardRepository.findByTokenHash(nfcTokenService.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("NFC card was not found"));

        if (card.getStatus() != NfcCardStatus.ACTIVE) {
            throw new NfcCardInactiveException("This NFC card is not currently active");
        }

        Client client = clientRepository.findById(card.getClientId())
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new NfcCardInactiveException("This NFC card is not currently active"));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new NfcCardInactiveException("This NFC card is not currently active");
        }

        Destination destination = destinationRepository.findById(card.getDestinationId())
                .orElseThrow(() -> new NfcCardInactiveException("This NFC card has no active destination"));

        String targetUrl = destinationResolverService.resolve(destination, client);

        card.setTotalTaps(card.getTotalTaps() + 1);
        card.setLastTappedAt(Instant.now());
        nfcCardRepository.save(card);

        analyticsService.recordEvent(client.getId(), card.getId(), null, destination.getId(),
                AnalyticsEventType.NFC_TAP, userAgent, referrer);

        return targetUrl;
    }

    private PageResponse<NfcCardResponse> enrich(Page<NfcCard> page) {
        List<Long> clientIds = page.getContent().stream().map(NfcCard::getClientId).filter(java.util.Objects::nonNull).toList();
        List<Long> destinationIds = page.getContent().stream().map(NfcCard::getDestinationId).filter(java.util.Objects::nonNull).toList();

        Map<Long, Client> clientsById = clientIds.isEmpty() ? Map.of() :
                clientRepository.findAllById(clientIds).stream().collect(Collectors.toMap(Client::getId, c -> c));
        Map<Long, Destination> destinationsById = destinationIds.isEmpty() ? Map.of() :
                destinationRepository.findAllById(destinationIds).stream().collect(Collectors.toMap(Destination::getId, d -> d));

        return PageResponse.of(page, card -> {
            Client client = card.getClientId() == null ? null : clientsById.get(card.getClientId());
            Destination destination = card.getDestinationId() == null ? null : destinationsById.get(card.getDestinationId());
            return NfcCardResponse.from(card,
                    client == null ? null : client.getDisplayName(),
                    client == null ? null : client.getUuid(),
                    destination == null ? null : destination.getName(),
                    destination == null ? null : destination.getUuid());
        });
    }

    private NfcCardResponse toResponse(NfcCard card) {
        Client client = card.getClientId() == null ? null : clientRepository.findById(card.getClientId()).orElse(null);
        Destination destination = card.getDestinationId() == null ? null : destinationRepository.findById(card.getDestinationId()).orElse(null);
        return NfcCardResponse.from(card,
                client == null ? null : client.getDisplayName(),
                client == null ? null : client.getUuid(),
                destination == null ? null : destination.getName(),
                destination == null ? null : destination.getUuid());
    }

    private NfcCardStatus parseStatus(String value) {
        try {
            return NfcCardStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid NFC card status: " + value);
        }
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
