package com.nfcplatform.analytics.service;

import com.nfcplatform.analytics.dto.AnalyticsSummaryResponse;
import com.nfcplatform.analytics.repository.AnalyticsEventRepository;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the analytics summary's day-range clamping and daily-series bucketing -
 * every day in the requested range is pre-seeded with zero counts (so a quiet day shows as 0,
 * not a gap in the chart), and any row the query returns for a day outside that pre-seeded
 * range is silently dropped rather than crashing. Pure Mockito, no Spring context/DB.
 */
class AnalyticsServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;
    @Mock
    private ClientRepository clientRepository;

    private AnalyticsService analyticsService;

    private static final long CLIENT_ID = 7L;
    private static final long OWNER_USER_ID = 100L;
    private static final String CLIENT_UUID = "client-uuid";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsService(analyticsEventRepository, clientRepository);
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client()));
        when(analyticsEventRepository.countByClientIdAndEventTypeAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(analyticsEventRepository.findDailyCounts(any(), any())).thenReturn(List.of());
        when(analyticsEventRepository.findDeviceBreakdown(any(), any())).thenReturn(List.of());
        when(analyticsEventRepository.findTopCards(any(), any())).thenReturn(List.of());
    }

    @Test
    void summaryForClientThrowsForAnUnknownClient() {
        when(clientRepository.findByUuidAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.summaryForClient("unknown", 7))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryForOwnClientThrowsWhenNoClientIsLinkedToTheActor() {
        when(clientRepository.findByOwnerUserIdAndDeletedAtIsNull(OWNER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.summaryForOwnClient(principal(), 7))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void zeroOrNegativeDaysDefaultsToASevenDayRange() {
        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 0);

        assertThat(response.rangeDays()).isEqualTo(7);
        assertThat(response.dailySeries()).hasSize(7);
    }

    @Test
    void aRequestForMoreThanNinetyDaysIsClampedToNinety() {
        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 365);

        assertThat(response.rangeDays()).isEqualTo(90);
        assertThat(response.dailySeries()).hasSize(90);
    }

    @Test
    void aValidDaysValueIsPassedThroughUnchanged() {
        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 14);

        assertThat(response.rangeDays()).isEqualTo(14);
        assertThat(response.dailySeries()).hasSize(14);
    }

    @Test
    void everyDayInRangeAppearsInTheSeriesEvenWithZeroEvents() {
        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 7);

        assertThat(response.dailySeries()).allMatch(point -> point.nfcTaps() == 0 && point.qrScans() == 0);
    }

    @Test
    void aDailyCountRowOutsideThePreSeededRangeIsSilentlyDroppedRatherThanCrashing() {
        AnalyticsEventRepository.DailyCount farOutOfRange = mockDailyCount(Date.valueOf(LocalDate.of(2000, 1, 1)), "NFC_TAP", 5L);
        when(analyticsEventRepository.findDailyCounts(any(), any())).thenReturn(List.of(farOutOfRange));

        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 7);

        assertThat(response.dailySeries()).allMatch(point -> point.nfcTaps() == 0);
    }

    @Test
    void nfcTapAndQrScanRowsForTheSameDayAreAggregatedIntoTheCorrectDailyPointColumn() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AnalyticsEventRepository.DailyCount tapRow = mockDailyCount(Date.valueOf(today), "NFC_TAP", 3L);
        AnalyticsEventRepository.DailyCount scanRow = mockDailyCount(Date.valueOf(today), "QR_SCAN", 2L);
        when(analyticsEventRepository.findDailyCounts(any(), any())).thenReturn(List.of(tapRow, scanRow));

        AnalyticsSummaryResponse response = analyticsService.summaryForClient(CLIENT_UUID, 7);

        AnalyticsSummaryResponse.DailyPoint todayPoint = response.dailySeries().stream()
                .filter(p -> p.date().equals(today.toString()))
                .findFirst()
                .orElseThrow();
        assertThat(todayPoint.nfcTaps()).isEqualTo(3);
        assertThat(todayPoint.qrScans()).isEqualTo(2);
    }

    private AnalyticsEventRepository.DailyCount mockDailyCount(Date day, String eventType, Long total) {
        AnalyticsEventRepository.DailyCount row = org.mockito.Mockito.mock(AnalyticsEventRepository.DailyCount.class);
        when(row.getDay()).thenReturn(day);
        when(row.getEventType()).thenReturn(eventType);
        when(row.getTotal()).thenReturn(total);
        return row;
    }

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setUuid(CLIENT_UUID);
        client.setOwnerUserId(OWNER_USER_ID);
        return client;
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(OWNER_USER_ID);
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
