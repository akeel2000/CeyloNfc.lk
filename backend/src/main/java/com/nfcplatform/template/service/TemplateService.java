package com.nfcplatform.template.service;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.profile.entity.CompanyProfile;
import com.nfcplatform.profile.entity.IndividualProfile;
import com.nfcplatform.profile.repository.CompanyProfileRepository;
import com.nfcplatform.profile.repository.IndividualProfileRepository;
import com.nfcplatform.profile.service.ProfileService;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.subscription.service.SubscriptionService;
import com.nfcplatform.template.dto.TemplateGalleryItem;
import com.nfcplatform.template.dto.TemplateRequest;
import com.nfcplatform.template.dto.TemplateResponse;
import com.nfcplatform.template.entity.Template;
import com.nfcplatform.template.entity.TemplateLayout;
import com.nfcplatform.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final ClientRepository clientRepository;
    private final IndividualProfileRepository individualProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ProfileService profileService;
    private final SubscriptionService subscriptionService;

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        Template template = new Template();
        apply(template, request);
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse update(String uuid, TemplateRequest request) {
        Template template = templateRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template was not found"));
        apply(template, request);
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listAllForAdmin() {
        return templateRepository.findAllByOrderBySortOrderAsc().stream()
                .map(TemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateGalleryItem> listGalleryForOwnClient(UserPrincipal actor) {
        Client client = requireOwnClient(actor);
        boolean hasPremiumAccess = subscriptionService.clientHasPremiumTemplateAccess(client.getId());
        Long currentTemplateId = currentTemplateId(client);
        return templateRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(template -> new TemplateGalleryItem(TemplateResponse.from(template),
                        template.isPremium() && !hasPremiumAccess,
                        template.getId().equals(currentTemplateId)))
                .toList();
    }

    /** TEMPLATE_MANAGE: "select/apply only" for clients (docs/ROLE_PERMISSION_MATRIX.md) - never authors gallery content. */
    @Transactional
    public void applyToOwnProfile(UserPrincipal actor, String templateUuid) {
        Client client = requireOwnClient(actor);
        Template template = templateRepository.findByUuid(templateUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Template was not found"));
        if (!template.isActive()) {
            throw new ValidationException("This template is no longer available");
        }
        if (template.isPremium() && !subscriptionService.clientHasPremiumTemplateAccess(client.getId())) {
            throw new ValidationException(
                    "This is a premium template - upgrade your plan to unlock it");
        }

        if (client.getType() == ClientType.INDIVIDUAL) {
            IndividualProfile profile = profileService.getOrCreateIndividual(client);
            profile.setTemplateId(template.getId());
            individualProfileRepository.save(profile);
        } else {
            CompanyProfile profile = profileService.getOrCreateCompany(client);
            profile.setTemplateId(template.getId());
            companyProfileRepository.save(profile);
        }
    }

    private Long currentTemplateId(Client client) {
        if (client.getType() == ClientType.INDIVIDUAL) {
            return individualProfileRepository.findByClientId(client.getId())
                    .map(IndividualProfile::getTemplateId).orElse(null);
        }
        return companyProfileRepository.findByClientId(client.getId())
                .map(CompanyProfile::getTemplateId).orElse(null);
    }

    private void apply(Template template, TemplateRequest request) {
        template.setName(request.name());
        template.setDescription(request.description());
        template.setPreviewImage(request.previewImage());
        template.setPrimaryColor(request.primaryColor());
        template.setLayout(parseLayout(request.layout()));
        template.setPremium(Boolean.TRUE.equals(request.premium()));
        template.setActive(request.active() == null || request.active());
        if (request.sortOrder() != null) template.setSortOrder(request.sortOrder());
    }

    private TemplateLayout parseLayout(String value) {
        try {
            return TemplateLayout.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid template layout: " + value);
        }
    }

    private Client requireOwnClient(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }
}
