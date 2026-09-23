package com.nfcplatform.settings.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.settings.dto.PlatformSettingsResponse;
import com.nfcplatform.settings.dto.PlatformSettingsUpdateRequest;
import com.nfcplatform.settings.entity.PlatformSettings;
import com.nfcplatform.settings.repository.PlatformSettingsRepository;
import com.nfcplatform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private final PlatformSettingsRepository platformSettingsRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PlatformSettingsResponse get() {
        return PlatformSettingsResponse.from(getOrCreate());
    }

    @Transactional
    public PlatformSettingsResponse update(PlatformSettingsUpdateRequest request, UserPrincipal actor,
                                            HttpServletRequest httpRequest) {
        PlatformSettings settings = getOrCreate();
        settings.setSiteName(request.siteName());
        settings.setSupportEmail(request.supportEmail());
        settings.setTagline(request.tagline());
        settings = platformSettingsRepository.save(settings);

        auditService.record(actor.getId(), "SETTINGS_UPDATE", "PlatformSettings", null,
                clientIp(httpRequest), null);

        return PlatformSettingsResponse.from(settings);
    }

    /** The seed migration already inserts the singleton row - this fallback only matters if that row is ever deleted by hand. */
    private PlatformSettings getOrCreate() {
        return platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID).orElseGet(() -> {
            PlatformSettings settings = new PlatformSettings();
            settings.setSiteName("CeyloNfc");
            settings.setSupportEmail("hello@ceylonfc.com");
            return platformSettingsRepository.save(settings);
        });
    }

    private String clientIp(HttpServletRequest request) {
        return com.nfcplatform.common.web.ClientIpResolver.resolve(request);
    }
}
