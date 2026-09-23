package com.nfcplatform.profile.service;

import com.nfcplatform.profile.entity.BusinessHour;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes "open now" purely from a business's own weekly hours, always in the business's own
 * timezone (hardcoded to Asia/Colombo - this platform has no other market yet, matching how
 * currency/locale are hardcoded elsewhere), never the visitor's device time. Handles overnight
 * ranges (e.g. 18:00-02:00) by treating closesAt <= opensAt as "closes the next day".
 */
public final class OpeningHoursCalculator {

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private OpeningHoursCalculator() {
    }

    public record OpenStatus(boolean open, String label) {
    }

    public static OpenStatus compute(List<BusinessHour> hours) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        Map<Integer, BusinessHour> byDay = hours.stream()
                .collect(Collectors.toMap(BusinessHour::getDayOfWeek, h -> h, (a, b) -> a));
        ZonedDateTime now = ZonedDateTime.now(ZONE);

        // A shift starting yesterday can still be open now (overnight range crossing midnight).
        BusinessHour yesterday = byDay.get(previousDay(now.getDayOfWeek()).getValue());
        if (yesterday != null && !yesterday.isClosed() && isOvernight(yesterday) && now.toLocalTime().isBefore(yesterday.getClosesAt())) {
            return new OpenStatus(true, "Open now · closes " + formatTime(yesterday.getClosesAt()));
        }

        BusinessHour today = byDay.get(now.getDayOfWeek().getValue());
        if (today != null && !today.isClosed()) {
            LocalTime nowTime = now.toLocalTime();
            boolean overnight = isOvernight(today);
            boolean openNow = overnight
                    ? !nowTime.isBefore(today.getOpensAt())
                    : !nowTime.isBefore(today.getOpensAt()) && nowTime.isBefore(today.getClosesAt());
            if (openNow) {
                return new OpenStatus(true, "Open now · closes " + formatTime(today.getClosesAt()) + (overnight ? " tomorrow" : ""));
            }
            if (nowTime.isBefore(today.getOpensAt())) {
                return new OpenStatus(false, "Closed · opens " + formatTime(today.getOpensAt()));
            }
        }

        for (int offset = 1; offset <= 7; offset++) {
            DayOfWeek candidateDay = now.getDayOfWeek().plus(offset);
            BusinessHour candidate = byDay.get(candidateDay.getValue());
            if (candidate != null && !candidate.isClosed()) {
                String dayLabel = offset == 1 ? "tomorrow" : candidateDay.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                return new OpenStatus(false, "Closed · opens " + dayLabel + " " + formatTime(candidate.getOpensAt()));
            }
        }
        return new OpenStatus(false, "Closed");
    }

    private static boolean isOvernight(BusinessHour hour) {
        return !hour.getClosesAt().isAfter(hour.getOpensAt());
    }

    private static DayOfWeek previousDay(DayOfWeek day) {
        return day.minus(1);
    }

    private static String formatTime(LocalTime time) {
        return time.format(TIME_LABEL);
    }
}
