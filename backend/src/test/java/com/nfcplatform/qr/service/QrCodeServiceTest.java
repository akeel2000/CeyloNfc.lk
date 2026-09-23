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
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the QR scan redirect hot path - mirrors NfcCardServiceTest.
 * resolveRedirectTarget's rejection chain, since QrCodeService.resolveRedirectTarget was
 * deliberately written to mirror NfcCardService.resolveRedirectTarget (see that method's
 * javadoc). Pure Mockito, no Spring context/DB.
 */
class QrCodeServiceTest {

    @Mock
    private QrCodeRepository qrCodeRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private DestinationService destinationService;
    @Mock
    private DestinationResolverService destinationResolverService;
    @Mock
    private NfcTokenService tokenService;
    @Mock
    private AuditService auditService;
    @Mock
    private AnalyticsService analyticsService;

    private QrCodeService qrCodeService;

    private static final long CLIENT_ID = 7L;
    private static final long DESTINATION_ID = 9L;
    private static final String RAW_TOKEN = "AAAAAAAAAAAAAAAAAAAA";
    private static final String TOKEN_HASH = "hashed-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        qrCodeService = new QrCodeService(qrCodeRepository, clientRepository, destinationRepository,
                destinationService, destinationResolverService, tokenService, auditService, appProperties,
                analyticsService);
        when(qrCodeRepository.save(any(QrCode.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- resolveRedirectTarget: mirrors NfcCardService's redirect hot path ------------------

    @Test
    void resolveRedirectTargetRejectsAMalformedTokenBeforeHittingTheDatabase() {
        when(tokenService.isValidFormat("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget("bad-token", "agent", "ref"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(qrCodeRepository, never()).findByTokenHash(any());
    }

    @Test
    void resolveRedirectTargetThrowsForAnUnknownTokenHash() {
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveRedirectTargetRejectsASuspendedQrCode() {
        QrCode suspended = activeQrCode();
        suspended.setStatus(QrCodeStatus.SUSPENDED);
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsAQrCodeWhoseClientWasSoftDeleted() {
        QrCode qr = activeQrCode();
        Client deletedClient = new Client();
        deletedClient.setId(CLIENT_ID);
        deletedClient.setStatus(ClientStatus.ACTIVE);
        deletedClient.setDeletedAt(Instant.now());
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(qr));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(deletedClient));

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsAQrCodeWhoseClientIsSuspended() {
        QrCode qr = activeQrCode();
        Client suspendedClient = new Client();
        suspendedClient.setId(CLIENT_ID);
        suspendedClient.setStatus(ClientStatus.SUSPENDED);
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(qr));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(suspendedClient));

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsAQrCodeWithNoResolvableDestination() {
        QrCode qr = activeQrCode();
        Client activeClient = activeClient();
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(qr));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(destinationRepository.findById(DESTINATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCodeService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetIncrementsScansAndRecordsAnalyticsOnTheHappyPath() {
        QrCode qr = activeQrCode();
        qr.setTotalScans(3);
        Client activeClient = activeClient();
        Destination destination = new Destination();
        destination.setId(DESTINATION_ID);
        when(tokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(qrCodeRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(qr));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(destinationRepository.findById(DESTINATION_ID)).thenReturn(Optional.of(destination));
        when(destinationResolverService.resolve(destination, activeClient)).thenReturn("https://target.example/page");

        String target = qrCodeService.resolveRedirectTarget(RAW_TOKEN, "some-agent", "https://ref.example");

        assertThat(target).isEqualTo("https://target.example/page");
        assertThat(qr.getTotalScans()).isEqualTo(4);
        assertThat(qr.getLastScannedAt()).isNotNull();
        verify(analyticsService).recordEvent(eq(CLIENT_ID), eq(null), eq(qr.getId()), eq(DESTINATION_ID),
                eq(AnalyticsEventType.QR_SCAN), eq("some-agent"), eq("https://ref.example"));
    }

    // --- setStatus: tenant-scoped for clients, global for admin -----------------------------

    @Test
    void settingStatusForOwnClientIsScopedToThatClientsQrCodesOnly() {
        UserPrincipal actor = principal();
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setOwnerUserId(1L);
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(client));
        when(qrCodeRepository.findByUuidAndClientId("qr-uuid", CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCodeService.setStatusForOwnClient(actor, "qr-uuid", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void settingStatusToInactiveSuspendsTheQrCode() {
        QrCode qr = activeQrCode();
        when(qrCodeRepository.findByUuid("qr-uuid")).thenReturn(Optional.of(qr));

        QrCodeResponse response = qrCodeService.setStatusForAdmin("qr-uuid", false);

        assertThat(response.status()).isEqualTo("SUSPENDED");
    }

    @Test
    void settingStatusToActiveReactivatesTheQrCode() {
        QrCode qr = activeQrCode();
        qr.setStatus(QrCodeStatus.SUSPENDED);
        when(qrCodeRepository.findByUuid("qr-uuid")).thenReturn(Optional.of(qr));

        QrCodeResponse response = qrCodeService.setStatusForAdmin("qr-uuid", true);

        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    // --- create: always creates a fresh destination first ------------------------------------

    @Test
    void creatingAQrCodeAlwaysCreatesItsDestinationFirst() {
        UserPrincipal actor = principal();
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid("client-uuid");
        client.setOwnerUserId(1L);
        Destination destinationEntity = new Destination();
        destinationEntity.setId(DESTINATION_ID);
        destinationEntity.setUuid("dest-uuid");
        DestinationResponse destinationResponse =
                new DestinationResponse("dest-uuid", "Website", "WEBSITE", "https://example.com", true);
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(client));
        when(destinationService.create(any())).thenReturn(destinationResponse);
        when(destinationRepository.findByUuid("dest-uuid")).thenReturn(Optional.of(destinationEntity));
        when(tokenService.generateRawToken()).thenReturn(RAW_TOKEN);
        when(tokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);

        QrCodeCreateResponse response = qrCodeService.createForOwnClient(actor,
                new QrCodeCreateRequest("Website", "WEBSITE", "https://example.com", null));

        verify(destinationService).create(any());
        assertThat(response.publicUrl()).isEqualTo("https://ceylonfc.com/q/" + RAW_TOKEN);
    }

    private QrCode activeQrCode() {
        QrCode qr = new QrCode();
        qr.setId(1L);
        qr.setClientId(CLIENT_ID);
        qr.setDestinationId(DESTINATION_ID);
        qr.setStatus(QrCodeStatus.ACTIVE);
        return qr;
    }

    private Client activeClient() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setStatus(ClientStatus.ACTIVE);
        return client;
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(1L);
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
