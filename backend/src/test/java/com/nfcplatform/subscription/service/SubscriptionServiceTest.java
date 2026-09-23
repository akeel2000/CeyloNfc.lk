package com.nfcplatform.subscription.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.SubscriptionExpiredException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.repository.NfcCardRepository;
import com.nfcplatform.packageplan.entity.PackagePlan;
import com.nfcplatform.packageplan.repository.PackagePlanRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.dto.SubscriptionAssignRequest;
import com.nfcplatform.subscription.dto.SubscriptionResponse;
import com.nfcplatform.subscription.entity.Subscription;
import com.nfcplatform.subscription.entity.SubscriptionStatus;
import com.nfcplatform.subscription.repository.SubscriptionRepository;
import com.nfcplatform.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the card-limit enforcement described in
 * docs/TECHNICAL_DECISIONS.md#subscription-card-limit-enforcement-lives-in-the-service-layer.
 * Pure Mockito, no Spring context/DB - runs without Testcontainers.
 */
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PackagePlanRepository packagePlanRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private NfcCardRepository nfcCardRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private SubscriptionService subscriptionService;

    private static final long CLIENT_ID = 42L;
    private static final String CLIENT_UUID = "client-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subscriptionService = new SubscriptionService(subscriptionRepository, packagePlanRepository,
                clientRepository, nfcCardRepository, auditService);
    }

    @Test
    void noSubscriptionMeansUnmanagedAndUnlimited() {
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> subscriptionService.assertCanAssignCard(CLIENT_ID)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionStatus.class, names = {"EXPIRED", "SUSPENDED", "CANCELLED"})
    void unusableSubscriptionStatusBlocksAssignmentRegardlessOfCardLimit(SubscriptionStatus status) {
        Subscription subscription = subscription(status, 1L);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.assertCanAssignCard(CLIENT_ID))
                .isInstanceOf(SubscriptionExpiredException.class)
                .hasMessageContaining(status.name().toLowerCase());
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionStatus.class, names = {"ACTIVE", "TRIAL"})
    void usableSubscriptionWithUnlimitedPlanNeverBlocks(SubscriptionStatus status) {
        Subscription subscription = subscription(status, 1L);
        PackagePlan unlimitedPlan = plan(null);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(1L)).thenReturn(Optional.of(unlimitedPlan));

        assertThatCode(() -> subscriptionService.assertCanAssignCard(CLIENT_ID)).doesNotThrowAnyException();
    }

    @Test
    void blocksAssignmentOnceCardCountReachesThePlanLimit() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 1L);
        PackagePlan oneCardPlan = plan(1);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(1L)).thenReturn(Optional.of(oneCardPlan));
        when(nfcCardRepository.findAllByClientIdOrderByCreatedAtDesc(CLIENT_ID)).thenReturn(cardsOfSize(1));

        assertThatThrownBy(() -> subscriptionService.assertCanAssignCard(CLIENT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Starter")
                .hasMessageContaining("1 card");
    }

    @Test
    void allowsAssignmentWhenUnderThePlanLimit() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 1L);
        PackagePlan threeCardPlan = plan(3);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(1L)).thenReturn(Optional.of(threeCardPlan));
        when(nfcCardRepository.findAllByClientIdOrderByCreatedAtDesc(CLIENT_ID)).thenReturn(cardsOfSize(2));

        assertThatCode(() -> subscriptionService.assertCanAssignCard(CLIENT_ID)).doesNotThrowAnyException();
    }

    @Test
    void missingPlanRecordIsTreatedAsUnlimitedRatherThanBlocking() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 999L);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatCode(() -> subscriptionService.assertCanAssignCard(CLIENT_ID))
                .as("a dangling package_plan_id must not hard-fail card assignment")
                .doesNotThrowAnyException();
    }

    @Test
    void noSubscriptionMeansUnrestrictedPremiumTemplateAccess() {
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        assertThat(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionStatus.class, names = {"EXPIRED", "SUSPENDED", "CANCELLED"})
    void unusableSubscriptionStatusDeniesPremiumTemplateAccessRegardlessOfPlan(SubscriptionStatus status) {
        Subscription subscription = subscription(status, 1L);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));

        assertThat(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).isFalse();
    }

    @Test
    void usableSubscriptionOnAPlanWithoutPremiumTemplatesDeniesAccess() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 1L);
        PackagePlan planWithoutPremium = plan(null);
        planWithoutPremium.setPremiumTemplates(false);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(1L)).thenReturn(Optional.of(planWithoutPremium));

        assertThat(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).isFalse();
    }

    @Test
    void usableSubscriptionOnAPlanWithPremiumTemplatesGrantsAccess() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 1L);
        PackagePlan planWithPremium = plan(null);
        planWithPremium.setPremiumTemplates(true);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(1L)).thenReturn(Optional.of(planWithPremium));

        assertThat(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).isTrue();
    }

    @Test
    void danglingPackagePlanIdDeniesPremiumTemplateAccessRatherThanThrowing() {
        Subscription subscription = subscription(SubscriptionStatus.ACTIVE, 999L);
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(packagePlanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(subscriptionService.clientHasPremiumTemplateAccess(CLIENT_ID)).isFalse();
    }

    @Test
    void assignForClientRecordsAnAuditEntry() {
        // Regression test for a real bug found by live interactive testing: subscription
        // changes were the only admin mutation in the whole platform with no audit trail at
        // all - every sibling admin service (ClientService, NfcCardService, AdminUserService,
        // MenuService...) already calls AuditService.record on every mutation.
        Client client = client();
        PackagePlan plan = plan(null);
        plan.setName("Pro");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(packagePlanRepository.findByUuid("plan-uuid")).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = subscriptionService.assignForClient(CLIENT_UUID,
                new SubscriptionAssignRequest("plan-uuid", "ACTIVE", null, null, null),
                principal(), httpServletRequest);

        assertThat(response.plan().name()).isEqualTo("Pro");
        verify(auditService).record(any(), eq("SUBSCRIPTION_ASSIGN"), eq("Subscription"), eq(client.getUuid()),
                any(), any());
    }

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setDisplayName("Test Client");
        return client;
    }

    private UserPrincipal principal() {
        Role role = new Role();
        role.setCode(RoleCode.SUPER_ADMIN);
        role.setName(RoleCode.SUPER_ADMIN.name());
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }

    private Subscription subscription(SubscriptionStatus status, Long packagePlanId) {
        Subscription subscription = new Subscription();
        subscription.setStatus(status);
        subscription.setPackagePlanId(packagePlanId);
        return subscription;
    }

    private PackagePlan plan(Integer cardLimit) {
        PackagePlan plan = new PackagePlan();
        plan.setName("Starter");
        plan.setCardLimit(cardLimit);
        return plan;
    }

    private List<NfcCard> cardsOfSize(int size) {
        return IntStream.range(0, size).mapToObj(i -> new NfcCard()).toList();
    }
}
