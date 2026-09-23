package com.nfcplatform.profile.service;

import com.nfcplatform.profile.entity.BusinessHour;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the "open now" computation always resolves in Asia/Colombo regardless of the JVM's
 * default zone, and handles overnight ranges (closesAt <= opensAt means "closes the next day") -
 * see docs/PROJECT_PROGRESS.md for why this exists (business profile "Open Now" badge).
 */
class OpeningHoursCalculatorTest {

    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

    private BusinessHour hour(int dayOfWeek, LocalTime opens, LocalTime closes, boolean closed) {
        BusinessHour h = new BusinessHour();
        h.setDayOfWeek(dayOfWeek);
        h.setOpensAt(opens);
        h.setClosesAt(closes);
        h.setClosed(closed);
        return h;
    }

    @Test
    void returnsNullWhenNoHoursAreConfiguredAtAll() {
        assertThat(OpeningHoursCalculator.compute(List.of())).isNull();
        assertThat(OpeningHoursCalculator.compute(null)).isNull();
    }

    @Test
    void reportsOpenDuringNormalDaytimeHours() {
        int today = ZonedDateTime.now(COLOMBO).getDayOfWeek().getValue();
        List<BusinessHour> hours = List.of(hour(today, LocalTime.of(0, 0), LocalTime.of(23, 59), false));

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);

        assertThat(status.open()).isTrue();
        assertThat(status.label()).startsWith("Open now");
    }

    @Test
    void reportsClosedWhenTodayIsMarkedClosed() {
        int today = ZonedDateTime.now(COLOMBO).getDayOfWeek().getValue();
        List<BusinessHour> hours = List.of(hour(today, null, null, true));

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);

        assertThat(status.open()).isFalse();
    }

    @Test
    void reportsClosedBeforeTodaysOpeningTimeWithAnOpensAtLabel() {
        int today = ZonedDateTime.now(COLOMBO).getDayOfWeek().getValue();
        LocalTime farFuture = LocalTime.now(COLOMBO).plusHours(2).isBefore(LocalTime.of(23, 0))
                ? LocalTime.now(COLOMBO).plusHours(2)
                : LocalTime.of(23, 30);
        List<BusinessHour> hours = List.of(hour(today, farFuture, LocalTime.of(23, 59), false));

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);

        assertThat(status.open()).isFalse();
        assertThat(status.label()).startsWith("Closed · opens");
    }

    @Test
    void reportsClosedAfterTodaysClosingTimeAndFindsTheNextOpenDay() {
        int today = ZonedDateTime.now(COLOMBO).getDayOfWeek().getValue();
        int tomorrow = today % 7 + 1;
        LocalTime justPassed = LocalTime.now(COLOMBO).minusMinutes(1);
        List<BusinessHour> hours = List.of(
                hour(today, LocalTime.of(0, 0), justPassed.isAfter(LocalTime.of(0, 5)) ? justPassed : LocalTime.of(0, 5), false),
                hour(tomorrow, LocalTime.of(9, 0), LocalTime.of(18, 0), false)
        );

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);

        assertThat(status.open()).isFalse();
        assertThat(status.label()).contains("opens");
    }

    @Test
    void treatsAnOvernightShiftStartingYesterdayAsStillOpenIfClosingTimeIsAheadOfNow() {
        int today = ZonedDateTime.now(COLOMBO).getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime nowPlusOneMinute = LocalTime.now(COLOMBO).plusMinutes(1);
        // Yesterday's shift: opens 18:00, closes a minute from now (today) - closesAt <= opensAt
        // marks it overnight, and "now" still falls before that closing time.
        List<BusinessHour> overnightFromYesterday = List.of(
                hour(yesterday, LocalTime.of(18, 0), nowPlusOneMinute, false)
        );

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(overnightFromYesterday);

        assertThat(status.open()).isTrue();
        assertThat(status.label()).startsWith("Open now");
    }

    @Test
    void allDayClosedEveryDayReportsClosedWithNoNextOpening() {
        List<BusinessHour> hours = List.of(
                hour(1, null, null, true), hour(2, null, null, true), hour(3, null, null, true),
                hour(4, null, null, true), hour(5, null, null, true), hour(6, null, null, true),
                hour(7, null, null, true)
        );

        OpeningHoursCalculator.OpenStatus status = OpeningHoursCalculator.compute(hours);

        assertThat(status.open()).isFalse();
        assertThat(status.label()).isEqualTo("Closed");
    }
}
