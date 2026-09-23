package com.nfcplatform.profile.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "company_profiles")
public class CompanyProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false, unique = true)
    private Long clientId;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String industry;

    @Column(name = "template_id")
    private Long templateId;

    @Column(length = 1000)
    private String description;

    private String logo;

    @Column(name = "cover_image")
    private String coverImage;

    private String phone;
    private String whatsapp;
    private String email;
    private String website;
    private String address;
    private String city;
    private String country;

    @Column(name = "google_maps_url")
    private String googleMapsUrl;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(nullable = false)
    private boolean published = false;
}
