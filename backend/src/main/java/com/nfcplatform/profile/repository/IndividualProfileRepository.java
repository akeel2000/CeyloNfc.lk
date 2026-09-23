package com.nfcplatform.profile.repository;

import com.nfcplatform.profile.entity.IndividualProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndividualProfileRepository extends JpaRepository<IndividualProfile, Long> {

    Optional<IndividualProfile> findByClientId(Long clientId);

    Optional<IndividualProfile> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndClientIdNot(String slug, Long clientId);
}
