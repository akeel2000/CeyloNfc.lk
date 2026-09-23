package com.nfcplatform.review.repository;

import com.nfcplatform.review.entity.GoogleReviewLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleReviewLocationRepository extends JpaRepository<GoogleReviewLocation, Long> {

    Optional<GoogleReviewLocation> findByUuidAndClientId(String uuid, Long clientId);

    Optional<GoogleReviewLocation> findByUuid(String uuid);

    List<GoogleReviewLocation> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
}
