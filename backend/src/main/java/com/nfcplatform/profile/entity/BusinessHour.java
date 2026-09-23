package com.nfcplatform.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/** One weekday's hours for a CompanyProfile. dayOfWeek matches java.time.DayOfWeek.getValue()
 *  (1=MONDAY .. 7=SUNDAY). A closed day has null opensAt/closesAt. */
@Getter
@Setter
@Entity
@Table(name = "business_hours")
public class BusinessHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_profile_id", nullable = false)
    private Long companyProfileId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = true;
}
