package com.nfcplatform.subscription.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.entity.NfcCardStatus;
import com.nfcplatform.nfc.repository.NfcCardRepository;
import com.nfcplatform.packageplan.dto.PackagePlanResponse;
import com.nfcplatform.packageplan.entity.PackagePlan;
import com.nfcplatform.packageplan.repository.PackagePlanRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.dto.SubscriptionAssignRequest;
import com.nfcplatform.subscription.dto.SubscriptionResponse;
import com.nfcplatform.subscription.entity.Subscription;
import com.nfcplatform.subscription.entity.SubscriptionStatus;
import com.nfcplatform.subscription.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PackagePlanRepository packagePlanRepository;
    private final ClientRepository clientRepository;
    private final NfcCardRepository nfcCardRepository;
    private final AuditService auditService;

    @Transactional
    public SubscriptionResponse assignForClient(String clientUuid, SubscriptionAssignRequest request,
                                                 UserPrincipal actor, HttpServletRequest httpRequest) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        PackagePlan plan = packagePlanRepository.findByUuid(request.packagePlanUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Package plan was not found"));

        Subscription subscription = subscriptionRepository.findByClientId(client.getId())
                .orElseGet(Subscription::new);
        subscription.setClientId(client.getId());
        subscription.setPackagePlanId(plan.getId());
        subscription.setStatus(request.status() == null ? SubscriptionStatus.ACTIVE : parseStatus(request.status()));
        if (request.endDate() != null) subscription.setEndDate(request.endDate());
        if (request.renewalDate() != null) subscription.setRenewalDate(request.renewalDate());
        subscription.setNotes(request.notes());
        subscription = subscriptionRepository.save(subscription);

        auditService.record(actor.getId(), "SUBSCRIPTION_ASSIGN", "Subscription", client.getUuid(),
                clientIp(httpRequest), Map.of("packagePlan", plan.getName(), "status", subscription.getStatus().name()));

        return toResponse(subscription, client, plan);
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getForOwnClient(UserPrincipal actor) {
        Client client = clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
        return getForClient(client);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getForClient(String clientUuid) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return getForClient(client);
    }

    private SubscriptionResponse getForClient(Client client) {
        Subscription subscription = subscriptionRepository.findByClientId(client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("This client has no subscription yet"));
        PackagePlan plan = packagePlanRepository.findById(subscription.getPackagePlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Package plan was not found"));
        return toResponse(subscription, client, plan);
    }

    /**
     * Platform-wide "which clients are on plan X" view - separate from the per-client
     * {@link #getForClient(String)} used on the client detail page, since that path is a
     * single lookup with no pagination/filtering concerns.
     */
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> list(String status, String packagePlanUuid, Pageable pageable) {
        SubscriptionStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        Long packagePlanId = null;
        if (packagePlanUuid != null && !packagePlanUuid.isBlank()) {
            packagePlanId = packagePlanRepository.findByUuid(packagePlanUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Package plan was not found"))
                    .getId();
        }
        Page<Subscription> page = subscriptionRepository.search(statusFilter, packagePlanId, pageable);
        return enrich(page);
    }

    private PageResponse<SubscriptionResponse> enrich(Page<Subscription> page) {
        List<Subscription> subscriptions = page.getContent();

        List<Long> clientIds = subscriptions.stream().map(Subscription::getClientId).distinct().toList();
        Map<Long, Client> clientsById = new HashMap<>();
        if (!clientIds.isEmpty()) {
            clientRepository.findAllById(clientIds).forEach(c -> clientsById.put(c.getId(), c));
        }

        List<Long> planIds = subscriptions.stream().map(Subscription::getPackagePlanId).distinct().toList();
        Map<Long, PackagePlan> plansById = new HashMap<>();
        if (!planIds.isEmpty()) {
            packagePlanRepository.findAllById(planIds).forEach(p -> plansById.put(p.getId(), p));
        }

        Map<Long, Long> cardCountByClientId = new HashMap<>();
        if (!clientIds.isEmpty()) {
            for (NfcCard card : nfcCardRepository.findAllByClientIdIn(clientIds)) {
                if (card.getStatus() != NfcCardStatus.REPLACED) {
                    cardCountByClientId.merge(card.getClientId(), 1L, Long::sum);
                }
            }
        }

        return PageResponse.of(page, subscription -> {
            Client client = clientsById.get(subscription.getClientId());
            PackagePlan plan = plansById.get(subscription.getPackagePlanId());
            int cardsUsed = Math.toIntExact(cardCountByClientId.getOrDefault(subscription.getClientId(), 0L));
            return SubscriptionResponse.from(subscription,
                    client == null ? null : client.getUuid(),
                    client == null ? null : client.getDisplayName(),
                    plan == null ? null : PackagePlanResponse.from(plan),
                    cardsUsed);
        });
    }

    /**
     * Enforced from NfcCardService.assign - the actual backend gate, not just a hidden
     * frontend button (see docs/SECURITY.md "feature limits"). No subscription record at
     * all is treated as unmanaged/unlimited (a client an admin hasn't put on a plan yet);
     * an existing but expired/suspended/cancelled subscription blocks assignment outright.
     */
    @Transactional(readOnly = true)
    public void assertCanAssignCard(Long clientId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByClientId(clientId);
        if (subscriptionOpt.isEmpty()) {
            return;
        }
        Subscription subscription = subscriptionOpt.get();
        if (!subscription.isUsable()) {
            throw new com.nfcplatform.common.exception.SubscriptionExpiredException(
                    "This client's subscription is " + subscription.getStatus().name().toLowerCase()
                            + " - renew or change plan before assigning more cards");
        }
        PackagePlan plan = packagePlanRepository.findById(subscription.getPackagePlanId()).orElse(null);
        if (plan == null || plan.getCardLimit() == null) {
            return;
        }
        long currentCards = countUsableCards(nfcCardRepository.findAllByClientIdOrderByCreatedAtDesc(clientId));
        if (currentCards >= plan.getCardLimit()) {
            throw new ValidationException(
                    "This client's plan (" + plan.getName() + ") allows up to " + plan.getCardLimit()
                            + " card(s) - upgrade the plan to assign more");
        }
    }

    /**
     * Same "no subscription record = unmanaged/unlimited" precedent as
     * {@link #assertCanAssignCard} - a client the admin hasn't put on a plan yet isn't being
     * enforced against any plan's capabilities either, premium templates included.
     */
    @Transactional(readOnly = true)
    public boolean clientHasPremiumTemplateAccess(Long clientId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByClientId(clientId);
        if (subscriptionOpt.isEmpty()) {
            return true;
        }
        Subscription subscription = subscriptionOpt.get();
        if (!subscription.isUsable()) {
            return false;
        }
        PackagePlan plan = packagePlanRepository.findById(subscription.getPackagePlanId()).orElse(null);
        return plan != null && plan.isPremiumTemplates();
    }

    private SubscriptionResponse toResponse(Subscription subscription, Client client, PackagePlan plan) {
        int cardsUsed = countUsableCards(nfcCardRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()));
        return SubscriptionResponse.from(subscription, client.getUuid(), client.getDisplayName(),
                PackagePlanResponse.from(plan), cardsUsed);
    }

    /**
     * A REPLACED card is a decommissioned physical card whose replacement already has its
     * own row for the same client - counting both would double-count against the plan's
     * card limit and misreport "cards used" for a client who has replaced a lost/damaged
     * card. Every other status (including SUSPENDED) still represents a provisioned card.
     */
    private int countUsableCards(List<NfcCard> cards) {
        return (int) cards.stream().filter(c -> c.getStatus() != NfcCardStatus.REPLACED).count();
    }

    private SubscriptionStatus parseStatus(String value) {
        try {
            return SubscriptionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid subscription status: " + value);
        }
    }
}
