package com.nfcplatform.qr.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.NfcCardInactiveException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.destination.dto.DestinationCreateRequest;
import com.nfcplatform.destination.dto.DestinationResponse;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.destination.service.DestinationResolverService;
import com.nfcplatform.destination.service.DestinationService;
import com.nfcplatform.nfc.service.NfcTokenService;
import com.nfcplatform.qr.dto.QrCodeCreateRequest;
import com.nfcplatform.qr.dto.QrCodeCreateResponse;
import com.nfcplatform.qr.dto.QrCodeResponse;
import com.nfcplatform.qr.entity.QrCode;
import com.nfcplatform.qr.entity.QrCodeStatus;
import com.nfcplatform.qr.repository.QrCodeRepository;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final ClientRepository clientRepository;
    private final DestinationRepository destinationRepository;
    private final DestinationService destinationService;
    private final DestinationResolverService destinationResolverService;
    private final NfcTokenService tokenService;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final AnalyticsService analyticsService;

    @Transactional
    public QrCodeCreateResponse createForOwnClient(UserPrincipal actor, QrCodeCreateRequest request) {
        return create(requireOwnClient(actor), request, actor);
    }

    @Transactional
    public QrCodeCreateResponse createForClient(String clientUuid, QrCodeCreateRequest request, UserPrincipal actor) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return create(client, request, actor);
    }

    private QrCodeCreateResponse create(Client client, QrCodeCreateRequest request, UserPrincipal actor) {
        DestinationResponse destination = destinationService.create(new DestinationCreateRequest(
                client.getUuid(), request.name(), request.destinationType(),
                request.externalUrl(), request.googleReviewLocationUuid()));
        Destination destinationEntity = destinationRepository.findByUuid(destination.uuid())
                .orElseThrow(() -> new IllegalStateException("Destination was just created but could not be found"));

        String rawToken = tokenService.generateRawToken();

        QrCode qr = new QrCode();
        qr.setClientId(client.getId());
        qr.setDestinationId(destinationEntity.getId());
        qr.setTokenHash(tokenService.hash(rawToken));
        qr.setName(request.name());
        qr = qrCodeRepository.save(qr);

        auditService.record(actor.getId(), "QR_CREATE", "QrCode", qr.getUuid(), null,
                Map.of("destinationUuid", destination.uuid()));

        String publicUrl = appProperties.getFrontendUrl() + "/q/" + rawToken;
        return new QrCodeCreateResponse(QrCodeResponse.from(qr, destination.name(), destination.uuid()), publicUrl);
    }

    @Transactional(readOnly = true)
    public List<QrCodeResponse> listForOwnClient(UserPrincipal actor) {
        Client client = requireOwnClient(actor);
        return enrich(qrCodeRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()));
    }

    @Transactional
    public QrCodeResponse setStatusForOwnClient(UserPrincipal actor, String uuid, boolean active) {
        Client client = requireOwnClient(actor);
        QrCode qr = qrCodeRepository.findByUuidAndClientId(uuid, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("QR code was not found"));
        return setStatus(qr, active);
    }

    @Transactional
    public QrCodeResponse setStatusForAdmin(String uuid, boolean active) {
        QrCode qr = qrCodeRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("QR code was not found"));
        return setStatus(qr, active);
    }

    private QrCodeResponse setStatus(QrCode qr, boolean active) {
        qr.setStatus(active ? QrCodeStatus.ACTIVE : QrCodeStatus.SUSPENDED);
        qr = qrCodeRepository.save(qr);

        Destination destination = destinationRepository.findById(qr.getDestinationId()).orElse(null);
        return QrCodeResponse.from(qr, destination == null ? null : destination.getName(),
                destination == null ? null : destination.getUuid());
    }

    @Transactional(readOnly = true)
    public List<QrCodeResponse> listForClient(String clientUuid) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return enrich(qrCodeRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()));
    }

    /** Platform-wide total for the admin dashboard KPI card - not scoped to any one client. */
    @Transactional(readOnly = true)
    public long countAll() {
        return qrCodeRepository.count();
    }

    /** QR scan redirect hot path - mirrors NfcCardService.resolveRedirectTarget. */
    @Transactional
    public String resolveRedirectTarget(String rawToken, String userAgent, String referrer) {
        if (!tokenService.isValidFormat(rawToken)) {
            throw new ResourceNotFoundException("QR code was not found");
        }

        QrCode qr = qrCodeRepository.findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("QR code was not found"));

        if (qr.getStatus() != QrCodeStatus.ACTIVE) {
            throw new NfcCardInactiveException("This QR code is not currently active");
        }

        Client client = clientRepository.findById(qr.getClientId())
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new NfcCardInactiveException("This QR code is not currently active"));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new NfcCardInactiveException("This QR code is not currently active");
        }

        Destination destination = destinationRepository.findById(qr.getDestinationId())
                .orElseThrow(() -> new NfcCardInactiveException("This QR code has no active destination"));

        String targetUrl = destinationResolverService.resolve(destination, client);

        qr.setTotalScans(qr.getTotalScans() + 1);
        qr.setLastScannedAt(Instant.now());
        qrCodeRepository.save(qr);

        analyticsService.recordEvent(client.getId(), null, qr.getId(), destination.getId(),
                AnalyticsEventType.QR_SCAN, userAgent, referrer);

        return targetUrl;
    }

    private List<QrCodeResponse> enrich(List<QrCode> codes) {
        List<Long> destinationIds = codes.stream().map(QrCode::getDestinationId).distinct().toList();
        Map<Long, Destination> destinationsById = destinationIds.isEmpty() ? Map.of() :
                destinationRepository.findAllById(destinationIds).stream()
                        .collect(java.util.stream.Collectors.toMap(Destination::getId, d -> d));

        return codes.stream().map(qr -> {
            Destination destination = destinationsById.get(qr.getDestinationId());
            return QrCodeResponse.from(qr, destination == null ? null : destination.getName(),
                    destination == null ? null : destination.getUuid());
        }).toList();
    }

    private Client requireOwnClient(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }
}
