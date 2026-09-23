package com.nfcplatform.destination.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "destinations")
public class Destination extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DestinationType type;

    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    @Column(name = "google_review_location_id")
    private Long googleReviewLocationId;

    @Column(nullable = false)
    private boolean active = true;
}
