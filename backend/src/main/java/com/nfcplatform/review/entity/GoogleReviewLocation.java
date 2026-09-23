package com.nfcplatform.review.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "google_review_locations")
public class GoogleReviewLocation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "location_name")
    private String locationName;

    private String address;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @Column(name = "google_review_url", nullable = false)
    private String googleReviewUrl;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(nullable = false)
    private boolean active = true;
}
