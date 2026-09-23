package com.nfcplatform.profile.service;

import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.service.AnalyticsService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientType;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.common.util.Slugify;
import com.nfcplatform.config.AppProperties;
import com.nfcplatform.profile.dto.*;
import com.nfcplatform.profile.entity.BusinessHour;
import com.nfcplatform.profile.entity.CompanyProfile;
import com.nfcplatform.profile.entity.IndividualProfile;
import com.nfcplatform.profile.entity.SocialLink;
import com.nfcplatform.profile.entity.SocialPlatform;
import com.nfcplatform.profile.repository.BusinessHourRepository;
import com.nfcplatform.profile.repository.CompanyProfileRepository;
import com.nfcplatform.profile.repository.IndividualProfileRepository;
import com.nfcplatform.profile.repository.SocialLinkRepository;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.template.entity.Template;
import com.nfcplatform.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ClientRepository clientRepository;
    private final IndividualProfileRepository individualProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final SocialLinkRepository socialLinkRepository;
    private final BusinessHourRepository businessHourRepository;
    private final TemplateRepository templateRepository;
    private final AppProperties appProperties;
    private final AnalyticsService analyticsService;

    @Transactional
    public ProfileResponse getOrCreateOwnProfile(UserPrincipal actor) {
        return getOrCreateProfile(requireOwnClient(actor));
    }

    /** Admin equivalent of {@link #getOrCreateOwnProfile} - PROFILE_MANAGE, any client by uuid. */
    @Transactional
    public ProfileResponse getOrCreateProfileForClient(String clientUuid) {
        return getOrCreateProfile(requireClient(clientUuid));
    }

    private ProfileResponse getOrCreateProfile(Client client) {
        return client.getType() == ClientType.INDIVIDUAL
                ? toResponse(getOrCreateIndividual(client))
                : toResponse(getOrCreateCompany(client));
    }

    @Transactional
    public ProfileResponse updateOwnProfile(UserPrincipal actor, ProfileUpdateRequest request) {
        return updateProfile(requireOwnClient(actor), request);
    }

    /** Admin equivalent of {@link #updateOwnProfile} - PROFILE_MANAGE, any client by uuid. */
    @Transactional
    public ProfileResponse updateProfileForClient(String clientUuid, ProfileUpdateRequest request) {
        return updateProfile(requireClient(clientUuid), request);
    }

    private ProfileResponse updateProfile(Client client, ProfileUpdateRequest request) {
        if (client.getType() == ClientType.INDIVIDUAL) {
            IndividualProfile profile = getOrCreateIndividual(client);
            ensureSlugAvailable(request.slug(), profile.getClientId(), true);

            if (request.fullName() == null || request.fullName().isBlank()) {
                throw new ValidationException("Full name is required");
            }

            profile.setSlug(request.slug());
            profile.setFullName(request.fullName());
            profile.setJobTitle(request.jobTitle());
            profile.setCompanyName(request.companyName());
            applySharedFields(profile::setBio, profile::setProfileImage, profile::setCoverImage,
                    profile::setPhone, profile::setWhatsapp, profile::setEmail, profile::setWebsite,
                    profile::setAddress, profile::setCity, profile::setCountry, request);
            return toResponse(individualProfileRepository.save(profile));
        } else {
            CompanyProfile profile = getOrCreateCompany(client);
            ensureSlugAvailable(request.slug(), profile.getClientId(), false);

            if (request.companyName() == null || request.companyName().isBlank()) {
                throw new ValidationException("Company name is required");
            }

            profile.setSlug(request.slug());
            profile.setCompanyName(request.companyName());
            profile.setIndustry(request.industry());
            profile.setDescription(request.bio());
            profile.setLogo(request.logo());
            profile.setCoverImage(request.coverImage());
            profile.setPhone(request.phone());
            profile.setWhatsapp(request.whatsapp());
            profile.setEmail(request.email());
            profile.setWebsite(request.website());
            profile.setAddress(request.address());
            profile.setCity(request.city());
            profile.setCountry(request.country());
            profile.setGoogleMapsUrl(request.googleMapsUrl());
            profile.setRegistrationNumber(request.registrationNumber());
            return toResponse(companyProfileRepository.save(profile));
        }
    }

    @Transactional
    public ProfileResponse setPublished(UserPrincipal actor, boolean published) {
        return setPublished(requireOwnClient(actor), published);
    }

    /** Admin equivalent of {@link #setPublished(UserPrincipal, boolean)} - any client by uuid. */
    @Transactional
    public ProfileResponse setPublishedForClient(String clientUuid, boolean published) {
        return setPublished(requireClient(clientUuid), published);
    }

    private ProfileResponse setPublished(Client client, boolean published) {
        if (client.getType() == ClientType.INDIVIDUAL) {
            IndividualProfile profile = getOrCreateIndividual(client);
            profile.setPublished(published);
            return toResponse(individualProfileRepository.save(profile));
        } else {
            CompanyProfile profile = getOrCreateCompany(client);
            profile.setPublished(published);
            return toResponse(companyProfileRepository.save(profile));
        }
    }

    @Transactional
    public List<SocialLinkDto> replaceSocialLinks(UserPrincipal actor, List<SocialLinkDto> links) {
        return replaceSocialLinks(requireOwnClient(actor), links);
    }

    /** Admin equivalent of {@link #replaceSocialLinks(UserPrincipal, List)} - any client by uuid. */
    @Transactional
    public List<SocialLinkDto> replaceSocialLinksForClient(String clientUuid, List<SocialLinkDto> links) {
        return replaceSocialLinks(requireClient(clientUuid), links);
    }

    private List<SocialLinkDto> replaceSocialLinks(Client client, List<SocialLinkDto> links) {
        socialLinkRepository.deleteAllByClientId(client.getId());

        List<SocialLink> saved = links.stream().map(dto -> {
            SocialLink link = new SocialLink();
            link.setClientId(client.getId());
            link.setPlatform(parsePlatform(dto.platform()));
            link.setUrl(dto.url());
            link.setDisplayOrder(dto.displayOrder());
            link.setEnabled(dto.enabled());
            return socialLinkRepository.save(link);
        }).toList();

        return saved.stream().map(SocialLinkDto::from).toList();
    }

    @Transactional
    public List<BusinessHourDto> replaceBusinessHours(UserPrincipal actor, List<BusinessHourDto> hours) {
        return replaceBusinessHours(requireOwnClient(actor), hours);
    }

    /** Admin equivalent of {@link #replaceBusinessHours(UserPrincipal, List)} - any client by uuid. */
    @Transactional
    public List<BusinessHourDto> replaceBusinessHoursForClient(String clientUuid, List<BusinessHourDto> hours) {
        return replaceBusinessHours(requireClient(clientUuid), hours);
    }

    private List<BusinessHourDto> replaceBusinessHours(Client client, List<BusinessHourDto> hours) {
        if (client.getType() != ClientType.BUSINESS) {
            throw new ValidationException("Opening hours only apply to business/company profiles");
        }
        CompanyProfile profile = getOrCreateCompany(client);
        // Hibernate flushes inserts before deletes by default within one transaction - without
        // an explicit flush here, the loop's saves below would hit the (company_profile_id,
        // day_of_week) unique constraint against the not-yet-deleted old rows.
        businessHourRepository.deleteAllByCompanyProfileId(profile.getId());
        businessHourRepository.flush();

        List<BusinessHour> saved = hours.stream().map(dto -> {
            if (!dto.closed() && (dto.opensAt() == null || dto.closesAt() == null)) {
                throw new ValidationException("Opening and closing time are required unless the day is marked closed");
            }
            BusinessHour hour = new BusinessHour();
            hour.setCompanyProfileId(profile.getId());
            hour.setDayOfWeek(dto.dayOfWeek());
            hour.setClosed(dto.closed());
            hour.setOpensAt(dto.closed() ? null : dto.opensAtTime());
            hour.setClosesAt(dto.closed() ? null : dto.closesAtTime());
            return businessHourRepository.save(hour);
        }).toList();

        return saved.stream().map(BusinessHourDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicIndividualProfile(String slug) {
        IndividualProfile profile = individualProfileRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Profile was not found"));
        List<SocialLinkDto> links = socialLinkRepository.findAllByClientIdAndEnabledTrueOrderByDisplayOrderAsc(profile.getClientId())
                .stream().map(SocialLinkDto::from).toList();
        Template template = resolveTemplate(profile.getTemplateId());
        return new PublicProfileResponse("INDIVIDUAL", profile.getSlug(), profile.getFullName(), profile.getJobTitle(),
                profile.getCompanyName(), null, profile.getBio(), profile.getProfileImage(), profile.getCoverImage(), null,
                profile.getPhone(), profile.getWhatsapp(), profile.getEmail(), profile.getWebsite(), profile.getAddress(),
                profile.getCity(), profile.getCountry(), null,
                template == null ? null : template.getPrimaryColor(),
                template == null ? null : template.getLayout().name(),
                links, null, null, null);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicCompanyProfile(String slug) {
        CompanyProfile profile = companyProfileRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile was not found"));
        List<SocialLinkDto> links = socialLinkRepository.findAllByClientIdAndEnabledTrueOrderByDisplayOrderAsc(profile.getClientId())
                .stream().map(SocialLinkDto::from).toList();
        Template template = resolveTemplate(profile.getTemplateId());
        List<BusinessHour> hours = businessHourRepository.findAllByCompanyProfileIdOrderByDayOfWeekAsc(profile.getId());
        List<BusinessHourDto> hourDtos = hours.stream().map(BusinessHourDto::from).toList();
        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);
        return new PublicProfileResponse("BUSINESS", profile.getSlug(), null, null, profile.getCompanyName(),
                profile.getIndustry(), profile.getDescription(), null, profile.getCoverImage(), profile.getLogo(),
                profile.getPhone(), profile.getWhatsapp(), profile.getEmail(), profile.getWebsite(), profile.getAddress(),
                profile.getCity(), profile.getCountry(), profile.getGoogleMapsUrl(),
                template == null ? null : template.getPrimaryColor(),
                template == null ? null : template.getLayout().name(),
                links, hourDtos,
                status == null ? null : status.open(),
                status == null ? null : status.label());
    }

    /**
     * Called from a client-side beacon on the public profile page, not during the page's own
     * SSR fetch - that's deliberate. The public profile page is a plain Server Component
     * fetch() today, which forwards Next's own request headers rather than the visitor's real
     * browser User-Agent, so recording the view there would misattribute every device/browser
     * breakdown. A same-origin client-side POST carries the visitor's actual User-Agent on the
     * wire with no forwarding needed. Silently no-ops on an unknown/unpublished slug - this
     * fires on every page load and must never surface an error to the visitor.
     */
    @Transactional
    public void recordIndividualProfileView(String slug, String userAgent, String referrer) {
        individualProfileRepository.findBySlugAndPublishedTrue(slug)
                .ifPresent(profile -> analyticsService.recordEvent(profile.getClientId(), null, null, null,
                        AnalyticsEventType.PROFILE_VIEW, userAgent, referrer));
    }

    @Transactional
    public void recordCompanyProfileView(String slug, String userAgent, String referrer) {
        companyProfileRepository.findBySlugAndPublishedTrue(slug)
                .ifPresent(profile -> analyticsService.recordEvent(profile.getClientId(), null, null, null,
                        AnalyticsEventType.PROFILE_VIEW, userAgent, referrer));
    }

    /** Used by DestinationService/NfcCardService to check a client has a profile before/at redirect time. */
    @Transactional(readOnly = true)
    public boolean hasPublishedProfile(Long clientId, ClientType type) {
        return type == ClientType.INDIVIDUAL
                ? individualProfileRepository.findByClientId(clientId).map(IndividualProfile::isPublished).orElse(false)
                : companyProfileRepository.findByClientId(clientId).map(CompanyProfile::isPublished).orElse(false);
    }

    /** Resolves the public profile URL for a client, for the NFC/QR redirect hot path. */
    @Transactional(readOnly = true)
    public String resolvePublicUrl(Long clientId, ClientType type) {
        if (type == ClientType.INDIVIDUAL) {
            IndividualProfile profile = individualProfileRepository.findByClientId(clientId)
                    .filter(IndividualProfile::isPublished)
                    .orElseThrow(() -> new ResourceNotFoundException("No published profile"));
            return appProperties.getFrontendUrl() + "/p/" + profile.getSlug();
        }
        CompanyProfile profile = companyProfileRepository.findByClientId(clientId)
                .filter(CompanyProfile::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("No published profile"));
        return appProperties.getFrontendUrl() + "/company/" + profile.getSlug();
    }

    /** Resolves a published profile's downloadable vCard for the NFC/QR redirect hot path. */
    @Transactional(readOnly = true)
    public String resolveVCardUrl(Long clientId, ClientType type) {
        if (type == ClientType.INDIVIDUAL) {
            IndividualProfile profile = individualProfileRepository.findByClientId(clientId)
                    .filter(IndividualProfile::isPublished)
                    .orElseThrow(() -> new ResourceNotFoundException("No published profile"));
            return appProperties.getBackendUrl() + "/api/v1/public/profile/" + profile.getSlug() + "/vcard";
        }
        CompanyProfile profile = companyProfileRepository.findByClientId(clientId)
                .filter(CompanyProfile::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("No published profile"));
        return appProperties.getBackendUrl() + "/api/v1/public/company/" + profile.getSlug() + "/vcard";
    }

    /** Also used by TemplateService.applyToOwnProfile - a client may apply a template before ever loading/saving their own profile page. */
    public IndividualProfile getOrCreateIndividual(Client client) {
        return individualProfileRepository.findByClientId(client.getId()).orElseGet(() -> {
            IndividualProfile profile = new IndividualProfile();
            profile.setClientId(client.getId());
            profile.setFullName(client.getDisplayName());
            profile.setEmail(client.getEmail());
            profile.setPhone(client.getPhone());
            profile.setSlug(uniqueSlug(client.getDisplayName(), true, client.getId()));
            return individualProfileRepository.save(profile);
        });
    }

    /** Also used by TemplateService.applyToOwnProfile - see {@link #getOrCreateIndividual}. */
    public CompanyProfile getOrCreateCompany(Client client) {
        return companyProfileRepository.findByClientId(client.getId()).orElseGet(() -> {
            CompanyProfile profile = new CompanyProfile();
            profile.setClientId(client.getId());
            profile.setCompanyName(client.getDisplayName());
            profile.setEmail(client.getEmail());
            profile.setPhone(client.getPhone());
            profile.setSlug(uniqueSlug(client.getDisplayName(), false, client.getId()));
            return companyProfileRepository.save(profile);
        });
    }

    private String uniqueSlug(String base, boolean individual, Long clientId) {
        String candidate = com.nfcplatform.common.util.Slugify.of(base);
        String result = candidate;
        int suffix = 1;
        while (individual ? individualProfileRepository.existsBySlug(result) : companyProfileRepository.existsBySlug(result)) {
            result = candidate + "-" + (++suffix);
        }
        return result;
    }

    private void ensureSlugAvailable(String slug, Long clientId, boolean individual) {
        boolean taken = individual
                ? individualProfileRepository.existsBySlugAndClientIdNot(slug, clientId)
                : companyProfileRepository.existsBySlugAndClientIdNot(slug, clientId);
        if (taken) {
            throw new ConflictException("This URL slug is already taken - choose another");
        }
    }

    private Client requireOwnClient(UserPrincipal actor) {
        return clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
    }

    private Client requireClient(String clientUuid) {
        return clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
    }

    private SocialPlatform parsePlatform(String value) {
        try {
            return SocialPlatform.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid social platform: " + value);
        }
    }

    private ProfileResponse toResponse(IndividualProfile profile) {
        List<SocialLinkDto> links = socialLinkRepository.findAllByClientIdOrderByDisplayOrderAsc(profile.getClientId())
                .stream().map(SocialLinkDto::from).toList();
        Template template = resolveTemplate(profile.getTemplateId());
        return new ProfileResponse("INDIVIDUAL", profile.getUuid(), profile.getSlug(), profile.isPublished(),
                appProperties.getFrontendUrl() + "/p/" + profile.getSlug(),
                profile.getFullName(), profile.getJobTitle(),
                null, null, null, null, null,
                profile.getBio(), profile.getProfileImage(), profile.getCoverImage(),
                profile.getPhone(), profile.getWhatsapp(), profile.getEmail(), profile.getWebsite(),
                profile.getAddress(), profile.getCity(), profile.getCountry(),
                template == null ? null : template.getUuid(), links, List.of());
    }

    private ProfileResponse toResponse(CompanyProfile profile) {
        List<SocialLinkDto> links = socialLinkRepository.findAllByClientIdOrderByDisplayOrderAsc(profile.getClientId())
                .stream().map(SocialLinkDto::from).toList();
        Template template = resolveTemplate(profile.getTemplateId());
        List<BusinessHourDto> hours = businessHourRepository.findAllByCompanyProfileIdOrderByDayOfWeekAsc(profile.getId())
                .stream().map(BusinessHourDto::from).toList();
        return new ProfileResponse("BUSINESS", profile.getUuid(), profile.getSlug(), profile.isPublished(),
                appProperties.getFrontendUrl() + "/company/" + profile.getSlug(),
                null, null,
                profile.getCompanyName(), profile.getIndustry(), profile.getRegistrationNumber(),
                profile.getGoogleMapsUrl(), profile.getLogo(),
                profile.getDescription(), null, profile.getCoverImage(),
                profile.getPhone(), profile.getWhatsapp(), profile.getEmail(), profile.getWebsite(),
                profile.getAddress(), profile.getCity(), profile.getCountry(),
                template == null ? null : template.getUuid(), links, hours);
    }

    private Template resolveTemplate(Long templateId) {
        return templateId == null ? null : templateRepository.findById(templateId).orElse(null);
    }

    @FunctionalInterface
    private interface StrSetter {
        void set(String value);
    }

    private void applySharedFields(StrSetter bio, StrSetter profileImage, StrSetter coverImage,
                                    StrSetter phone, StrSetter whatsapp, StrSetter email, StrSetter website,
                                    StrSetter address, StrSetter city, StrSetter country,
                                    ProfileUpdateRequest request) {
        bio.set(request.bio());
        profileImage.set(request.profileImage());
        coverImage.set(request.coverImage());
        phone.set(request.phone());
        whatsapp.set(request.whatsapp());
        email.set(request.email());
        website.set(request.website());
        address.set(request.address());
        city.set(request.city());
        country.set(request.country());
    }
}
