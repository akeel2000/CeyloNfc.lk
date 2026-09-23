package com.nfcplatform.subscription.repository;

import com.nfcplatform.subscription.entity.Subscription;
import com.nfcplatform.subscription.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByClientId(Long clientId);

    @Query("SELECT s FROM Subscription s WHERE (:status IS NULL OR s.status = :status) " +
            "AND (:packagePlanId IS NULL OR s.packagePlanId = :packagePlanId)")
    Page<Subscription> search(@Param("status") SubscriptionStatus status,
                               @Param("packagePlanId") Long packagePlanId,
                               Pageable pageable);
}
