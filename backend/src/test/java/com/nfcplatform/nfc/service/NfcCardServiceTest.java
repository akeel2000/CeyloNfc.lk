package com.nfcplatform.nfc.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.NfcCardInactiveException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.destination.entity.Destination;
import com.nfcplatform.destination.repository.DestinationRepository;
import com.nfcplatform.destination.service.DestinationResolverService;
import com.nfcplatform.nfc.dto.NfcCardAssignRequest;
import com.nfcplatform.nfc.dto.NfcCardRegisterRequest;
import com.nfcplatform.nfc.dto.NfcCardRegisterResponse;
import com.nfcplatform.nfc.dto.NfcCardReplaceRequest;
import com.nfcplatform.nfc.dto.NfcCardResponse;
import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.entity.NfcCardStatus;
import com.nfcplatform.nfc.repository.NfcCardRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.service.SubscriptionService;
import com.nfcplatform.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the redirect hot path (resolveRedirectTarget) and the card-limit gating
 * around assign/replace - see docs/SECURITY.md "NFC / QR token security" and
 * NfcCardService.replace's javadoc for why replace deliberately skips the limit check that
 * assign enforces. Pure Mockito, no Spring context/DB.
 */
class NfcCardServiceTest {

    @Mock
    private NfcCardRepository nfcCardRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DestinationRepository destinationRepository;
    @Mock
    private NfcTokenService nfcTokenService;
    @Mock
    private AuditService auditService;
    @Mock
    private DestinationResolverService destinationResolverService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private NfcCardService nfcCardService;

    private static final long CLIENT_ID = 7L;
    private static final long DESTINATION_ID = 9L;
    private static final long ACTOR_USER_ID = 100L;
    private static final String RAW_TOKEN = "AAAAAAAAAAAAAAAAAAAA";
    private static final String TOKEN_HASH = "hashed-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl("https://ceylonfc.com");
        nfcCardService = new NfcCardService(nfcCardRepository, clientRepository, destinationRepository,
                nfcTokenService, auditService, appProperties, destinationResolverService, analyticsService,
                subscriptionService);
        when(nfcCardRepository.save(any(NfcCard.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- resolveRedirectTarget: the redirect hot path -----------------------------------

    @Test
    void resolveRedirectTargetRejectsAnObviouslyMalformedTokenBeforeHittingTheDatabase() {
        when(nfcTokenService.isValidFormat("not-a-real-token")).thenReturn(false);

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget("not-a-real-token", "agent", "ref"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(nfcCardRepository, never()).findByTokenHash(any());
    }

    @Test
    void resolveRedirectTargetThrowsForAnUnknownTokenHash() {
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveRedirectTargetRejectsANonActiveCard() {
        NfcCard suspendedCard = activeCard();
        suspendedCard.setStatus(NfcCardStatus.SUSPENDED);
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(suspendedCard));

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsACardWhoseClientWasSoftDeleted() {
        NfcCard card = activeCard();
        Client deletedClient = new Client();
        deletedClient.setId(CLIENT_ID);
        deletedClient.setStatus(ClientStatus.ACTIVE);
        deletedClient.setDeletedAt(Instant.now());
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(card));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(deletedClient));

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsACardWhoseClientIsSuspended() {
        NfcCard card = activeCard();
        Client suspendedClient = new Client();
        suspendedClient.setId(CLIENT_ID);
        suspendedClient.setStatus(ClientStatus.SUSPENDED);
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(card));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(suspendedClient));

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetRejectsACardWithNoResolvableDestination() {
        NfcCard card = activeCard();
        Client activeClient = activeClient();
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(card));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(destinationRepository.findById(DESTINATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> nfcCardService.resolveRedirectTarget(RAW_TOKEN, "agent", "ref"))
                .isInstanceOf(NfcCardInactiveException.class);
    }

    @Test
    void resolveRedirectTargetIncrementsTapsAndRecordsAnalyticsOnTheHappyPath() {
        NfcCard card = activeCard();
        card.setTotalTaps(4);
        Client activeClient = activeClient();
        Destination destination = new Destination();
        destination.setId(DESTINATION_ID);
        when(nfcTokenService.isValidFormat(RAW_TOKEN)).thenReturn(true);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(nfcCardRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(card));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(destinationRepository.findById(DESTINATION_ID)).thenReturn(Optional.of(destination));
        when(destinationResolverService.resolve(destination, activeClient)).thenReturn("https://target.example/page");

        String target = nfcCardService.resolveRedirectTarget(RAW_TOKEN, "some-agent", "https://ref.example");

        assertThat(target).isEqualTo("https://target.example/page");
        assertThat(card.getTotalTaps()).isEqualTo(5);
        assertThat(card.getLastTappedAt()).isNotNull();
        verify(analyticsService).recordEvent(eq(CLIENT_ID), eq(card.getId()), eq(null), eq(DESTINATION_ID),
                eq(AnalyticsEventType.NFC_TAP), eq("some-agent"), eq("https://ref.example"));
    }

    // --- assign: card-limit gating only on a genuinely new client assignment ------------

    @Test
    void assigningAnUnassignedCardToAClientChecksTheCardLimit() {
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setId(1L);
        unassignedCard.setClientId(null);
        Client client = clientWithUuid("client-uuid");
        Destination destination = destinationWithUuid("dest-uuid", CLIENT_ID);

        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(unassignedCard));
        when(clientRepository.findByUuidAndDeletedAtIsNull("client-uuid")).thenReturn(Optional.of(client));
        when(destinationRepository.findByUuidAndClientId("dest-uuid", CLIENT_ID)).thenReturn(Optional.of(destination));

        nfcCardService.assign("card-uuid", new NfcCardAssignRequest("client-uuid", "dest-uuid"),
                principal(), httpServletRequest);

        verify(subscriptionService).assertCanAssignCard(CLIENT_ID);
        assertThat(unassignedCard.getStatus()).isEqualTo(NfcCardStatus.ACTIVE);
    }

    @Test
    void reassigningACardAlreadyBelongingToTheSameClientSkipsTheCardLimitCheck() {
        NfcCard alreadyAssignedCard = new NfcCard();
        alreadyAssignedCard.setId(1L);
        alreadyAssignedCard.setClientId(CLIENT_ID);
        Client client = clientWithUuid("client-uuid");
        Destination newDestination = destinationWithUuid("new-dest-uuid", CLIENT_ID);

        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(alreadyAssignedCard));
        when(clientRepository.findByUuidAndDeletedAtIsNull("client-uuid")).thenReturn(Optional.of(client));
        when(destinationRepository.findByUuidAndClientId("new-dest-uuid", CLIENT_ID)).thenReturn(Optional.of(newDestination));

        nfcCardService.assign("card-uuid", new NfcCardAssignRequest("client-uuid", "new-dest-uuid"),
                principal(), httpServletRequest);

        verify(subscriptionService, never()).assertCanAssignCard(anyLong());
    }

    @Test
    void assigningACardToADifferentClientThanItCurrentlyBelongsToChecksTheCardLimit() {
        NfcCard cardBelongingToAnotherClient = new NfcCard();
        cardBelongingToAnotherClient.setId(1L);
        cardBelongingToAnotherClient.setClientId(999L);
        Client client = clientWithUuid("client-uuid");
        Destination destination = destinationWithUuid("dest-uuid", CLIENT_ID);

        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(cardBelongingToAnotherClient));
        when(clientRepository.findByUuidAndDeletedAtIsNull("client-uuid")).thenReturn(Optional.of(client));
        when(destinationRepository.findByUuidAndClientId("dest-uuid", CLIENT_ID)).thenReturn(Optional.of(destination));

        nfcCardService.assign("card-uuid", new NfcCardAssignRequest("client-uuid", "dest-uuid"),
                principal(), httpServletRequest);

        verify(subscriptionService).assertCanAssignCard(CLIENT_ID);
    }

    // --- replace: like-for-like swap deliberately skips the card-limit check ------------

    @Test
    void replaceRejectsAnOldCardWithNoExistingAssignment() {
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setClientId(null);
        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(unassignedCard));

        assertThatThrownBy(() -> nfcCardService.replace("card-uuid",
                new NfcCardReplaceRequest("NEW-SERIAL", null), principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void replaceRejectsADuplicateNewSerialNumber() {
        NfcCard oldCard = activeCard();
        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(oldCard));
        when(nfcCardRepository.existsBySerialNumber("DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> nfcCardService.replace("card-uuid",
                new NfcCardReplaceRequest("DUPLICATE", null), principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void replaceNeverConsultsTheSubscriptionCardLimit() {
        NfcCard oldCard = activeCard();
        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(oldCard));
        when(nfcCardRepository.existsBySerialNumber("NEW-SERIAL")).thenReturn(false);
        when(nfcTokenService.generateRawToken()).thenReturn(RAW_TOKEN);
        when(nfcTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);

        NfcCardRegisterResponse response = nfcCardService.replace("card-uuid",
                new NfcCardReplaceRequest("NEW-SERIAL", null), principal(), httpServletRequest);

        verify(subscriptionService, never()).assertCanAssignCard(anyLong());
        assertThat(oldCard.getStatus()).isEqualTo(NfcCardStatus.REPLACED);
        assertThat(response.rawToken()).isEqualTo(RAW_TOKEN);
    }

    // --- setStatus: activation requires a full assignment --------------------------------

    @Test
    void activatingAnUnassignedCardIsRejected() {
        NfcCard unassignedCard = new NfcCard();
        unassignedCard.setClientId(null);
        unassignedCard.setDestinationId(null);
        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(unassignedCard));

        assertThatThrownBy(() -> nfcCardService.setStatus("card-uuid", "ACTIVE", principal(), httpServletRequest))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void suspendingAFullyAssignedCardIsAllowed() {
        NfcCard assignedCard = activeCard();
        when(nfcCardRepository.findByUuid("card-uuid")).thenReturn(Optional.of(assignedCard));

        NfcCardResponse response = nfcCardService.setStatus("card-uuid", "SUSPENDED", principal(), httpServletRequest);

        assertThat(response.status()).isEqualTo("SUSPENDED");
    }

    // --- register: serial number must be unique ------------------------------------------

    @Test
    void registerRejectsADuplicateSerialNumber() {
        when(nfcCardRepository.existsBySerialNumber("EXISTING")).thenReturn(true);

        assertThatThrownBy(() -> nfcCardService.register(new NfcCardRegisterRequest("EXISTING", null),
                principal(), httpServletRequest))
                .isInstanceOf(ConflictException.class);
    }

    private NfcCard activeCard() {
        NfcCard card = new NfcCard();
        card.setId(1L);
        card.setClientId(CLIENT_ID);
        card.setDestinationId(DESTINATION_ID);
        card.setStatus(NfcCardStatus.ACTIVE);
        return card;
    }

    private Client activeClient() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setStatus(ClientStatus.ACTIVE);
        return client;
    }

    private Client clientWithUuid(String uuid) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(uuid);
        client.setStatus(ClientStatus.ACTIVE);
        return client;
    }

    private Destination destinationWithUuid(String uuid, long clientId) {
        Destination destination = new Destination();
        destination.setId(DESTINATION_ID);
        destination.setUuid(uuid);
        destination.setClientId(clientId);
        return destination;
    }

    private UserPrincipal principal() {
        Role role = new Role();
        role.setCode(RoleCode.SUPER_ADMIN);
        role.setName(RoleCode.SUPER_ADMIN.name());

        User user = new User();
        user.setId(ACTOR_USER_ID);
        user.setEmail("admin@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
