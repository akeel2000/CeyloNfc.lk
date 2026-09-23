package com.nfcplatform.profile.entity;

import com.nfcplatform.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "individual_profiles")
public class IndividualProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "client_id", nullable = false, unique = true)
    private Long clientId;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "template_id")
    private Long templateId;

    @Column(length = 1000)
    private String bio;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "cover_image")
    private String coverImage;

    private String phone;
    private String whatsapp;
    private String email;
    private String website;
    private String address;
    private String city;
    private String country;

    @Column(nullable = false)
    private boolean published = false;
}
