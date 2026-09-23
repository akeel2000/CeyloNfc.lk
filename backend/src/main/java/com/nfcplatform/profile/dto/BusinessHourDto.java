package com.nfcplatform.profile.dto;

import com.nfcplatform.profile.entity.BusinessHour;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** opensAt/closesAt as "HH:mm" strings - simplest shape for a plain HTML time input on the
 *  frontend. Both are null when closed is true. */
public record BusinessHourDto(
        @Min(1) @Max(7) int dayOfWeek,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use HH:mm") String opensAt,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Use HH:mm") String closesAt,
        boolean closed
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static BusinessHourDto from(BusinessHour hour) {
        return new BusinessHourDto(
                hour.getDayOfWeek(),
                hour.getOpensAt() == null ? null : hour.getOpensAt().format(FORMAT),
                hour.getClosesAt() == null ? null : hour.getClosesAt().format(FORMAT),
                hour.isClosed()
        );
    }

    public LocalTime opensAtTime() {
        return opensAt == null ? null : LocalTime.parse(opensAt, FORMAT);
    }

    public LocalTime closesAtTime() {
        return closesAt == null ? null : LocalTime.parse(closesAt, FORMAT);
    }
}
