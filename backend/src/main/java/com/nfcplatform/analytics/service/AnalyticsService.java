package com.nfcplatform.analytics.service;

import com.nfcplatform.analytics.dto.AnalyticsSummaryResponse;
import com.nfcplatform.analytics.entity.AnalyticsEvent;
import com.nfcplatform.analytics.entity.AnalyticsEventType;
import com.nfcplatform.analytics.repository.AnalyticsEventRepository;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final ClientRepository clientRepository;

    /**
     * Fired from the NFC/QR redirect hot path. @Async so a slow analytics insert never
     * delays the visitor's redirect (docs/NFC_FLOW.md commitment). Takes primitive/String
     * args rather than entities so no lazy-loading surprises cross the thread boundary.
     */
    @Async
    @Transactional
    public void recordEvent(Long clientId, Long nfcCardId, Long qrCodeId, Long destinationId,
                             AnalyticsEventType type, String userAgent, String referrer) {
        UserAgentParser.ParsedUserAgent parsed = UserAgentParser.parse(userAgent);

        AnalyticsEvent event = new AnalyticsEvent();
        event.setClientId(clientId);
        event.setNfcCardId(nfcCardId);
        event.setQrCodeId(qrCodeId);
        event.setDestinationId(destinationId);
        event.setEventType(type);
        event.setDeviceType(parsed.deviceType());
        event.setBrowser(parsed.browser());
        event.setOs(parsed.os());
        event.setReferrer(referrer);
        analyticsEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summaryForOwnClient(UserPrincipal actor, int days) {
        Client client = clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
        return summaryFor(client.getId(), days);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summaryForClient(String clientUuid, int days) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(clientUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));
        return summaryFor(client.getId(), days);
    }

    private AnalyticsSummaryResponse summaryFor(Long clientId, int days) {
        int rangeDays = days <= 0 ? 7 : Math.min(days, 90);
        Instant since = Instant.now().minus(rangeDays, ChronoUnit.DAYS);

        long totalTaps = analyticsEventRepository.countByClientIdAndEventTypeAndCreatedAtAfter(
                clientId, AnalyticsEventType.NFC_TAP, since);
        long totalScans = analyticsEventRepository.countByClientIdAndEventTypeAndCreatedAtAfter(
                clientId, AnalyticsEventType.QR_SCAN, since);
        long totalProfileViews = analyticsEventRepository.countByClientIdAndEventTypeAndCreatedAtAfter(
                clientId, AnalyticsEventType.PROFILE_VIEW, since);

        Map<String, long[]> byDay = new LinkedHashMap<>();
        for (int i = rangeDays - 1; i >= 0; i--) {
            String key = Instant.now().minus(i, ChronoUnit.DAYS).toString().substring(0, 10);
            byDay.put(key, new long[2]);
        }
        for (AnalyticsEventRepository.DailyCount row : analyticsEventRepository.findDailyCounts(clientId, since)) {
            String key = row.getDay().toString();
            long[] counts = byDay.get(key);
            if (counts == null) continue;
            if ("NFC_TAP".equals(row.getEventType())) counts[0] += row.getTotal();
            if ("QR_SCAN".equals(row.getEventType())) counts[1] += row.getTotal();
        }
        List<AnalyticsSummaryResponse.DailyPoint> dailySeries = byDay.entrySet().stream()
                .map(e -> new AnalyticsSummaryResponse.DailyPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        List<AnalyticsSummaryResponse.DeviceStat> deviceBreakdown = analyticsEventRepository
                .findDeviceBreakdown(clientId, since).stream()
                .map(row -> new AnalyticsSummaryResponse.DeviceStat(row.getDevice(), row.getTotal()))
                .toList();

        List<AnalyticsSummaryResponse.TopItem> topCards = analyticsEventRepository
                .findTopCards(clientId, since).stream()
                .map(row -> new AnalyticsSummaryResponse.TopItem(row.getLabel(), row.getTotal()))
                .toList();

        return new AnalyticsSummaryResponse(rangeDays, totalTaps, totalScans, totalProfileViews,
                dailySeries, deviceBreakdown, topCards);
    }
}
