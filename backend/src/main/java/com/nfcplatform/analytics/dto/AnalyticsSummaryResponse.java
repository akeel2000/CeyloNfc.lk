package com.nfcplatform.analytics.dto;

import java.util.List;

public record AnalyticsSummaryResponse(
        int rangeDays,
        long totalNfcTaps,
        long totalQrScans,
        long totalProfileViews,
        List<DailyPoint> dailySeries,
        List<DeviceStat> deviceBreakdown,
        List<TopItem> topCards
) {
    public record DailyPoint(String date, long nfcTaps, long qrScans) {
    }

    public record DeviceStat(String device, long count) {
    }

    public record TopItem(String label, long count) {
    }
}
