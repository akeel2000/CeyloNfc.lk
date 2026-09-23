package com.nfcplatform.profile.repository;

import com.nfcplatform.profile.entity.CompanyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile> findByClientId(Long clientId);

    Optional<CompanyProfile> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndClientIdNot(String slug, Long clientId);
}
