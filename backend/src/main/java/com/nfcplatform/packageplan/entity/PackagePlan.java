package com.nfcplatform.packageplan.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "package_plans")
public class PackagePlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod = BillingPeriod.MONTHLY;

    /** Null = unlimited. */
    @Column(name = "card_limit")
    private Integer cardLimit;

    @Column(name = "profile_limit")
    private Integer profileLimit;

    @Column(name = "review_location_limit")
    private Integer reviewLocationLimit;

    @Column(name = "menu_limit")
    private Integer menuLimit;

    @Column(name = "premium_templates", nullable = false)
    private boolean premiumTemplates = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
