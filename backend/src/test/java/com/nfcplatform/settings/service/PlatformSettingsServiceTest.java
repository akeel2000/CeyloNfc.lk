package com.nfcplatform.settings.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.settings.dto.PlatformSettingsResponse;
import com.nfcplatform.settings.dto.PlatformSettingsUpdateRequest;
import com.nfcplatform.settings.entity.PlatformSettings;
import com.nfcplatform.settings.repository.PlatformSettingsRepository;
import com.nfcplatform.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the singleton platform settings row - the get-or-create fallback only
 * matters if the seeded row is ever deleted by hand, so it's tested explicitly rather than
 * assumed to never run. Pure Mockito, no Spring context/DB.
 */
class PlatformSettingsServiceTest {

    @Mock
    private PlatformSettingsRepository platformSettingsRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private PlatformSettingsService platformSettingsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        platformSettingsService = new PlatformSettingsService(platformSettingsRepository, auditService);
        when(platformSettingsRepository.save(any(PlatformSettings.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getReturnsTheExistingSingletonRowWhenOnePresent() {
        PlatformSettings settings = new PlatformSettings();
        settings.setSiteName("CeyloNfc");
        settings.setSupportEmail("hello@ceylonfc.com");
        when(platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));

        PlatformSettingsResponse response = platformSettingsService.get();

        assertThat(response.siteName()).isEqualTo("CeyloNfc");
    }

    @Test
    void getCreatesADefaultRowIfTheSingletonWasSomehowDeleted() {
        when(platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID)).thenReturn(Optional.empty());

        PlatformSettingsResponse response = platformSettingsService.get();

        assertThat(response.siteName()).isNotBlank();
        assertThat(response.supportEmail()).isNotBlank();
        verify(platformSettingsRepository).save(any(PlatformSettings.class));
    }

    @Test
    void updateSavesTheNewValuesAndRecordsAnAuditEntry() {
        PlatformSettings existing = new PlatformSettings();
        existing.setSiteName("Old Name");
        existing.setSupportEmail("old@ceylonfc.com");
        when(platformSettingsRepository.findById(PlatformSettings.SINGLETON_ID)).thenReturn(Optional.of(existing));

        PlatformSettingsResponse response = platformSettingsService.update(
                new PlatformSettingsUpdateRequest("New Name", "new@ceylonfc.com", "A new tagline"),
                principal(), httpServletRequest);

        assertThat(response.siteName()).isEqualTo("New Name");
        assertThat(response.supportEmail()).isEqualTo("new@ceylonfc.com");
        assertThat(response.tagline()).isEqualTo("A new tagline");
        verify(auditService).record(any(), org.mockito.ArgumentMatchers.eq("SETTINGS_UPDATE"),
                org.mockito.ArgumentMatchers.eq("PlatformSettings"), any(), any(), any());
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@test.local");
        user.setPasswordHash("hash");
        Role role = new Role();
        role.setCode(RoleCode.SUPER_ADMIN);
        role.setName(RoleCode.SUPER_ADMIN.name());
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
