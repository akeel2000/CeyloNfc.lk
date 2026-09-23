package com.nfcplatform.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "nfc_card_id")
    private Long nfcCardId;

    @Column(name = "qr_code_id")
    private Long qrCodeId;

    @Column(name = "destination_id")
    private Long destinationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AnalyticsEventType eventType;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(length = 50)
    private String browser;

    @Column(length = 50)
    private String os;

    @Column(length = 500)
    private String referrer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
