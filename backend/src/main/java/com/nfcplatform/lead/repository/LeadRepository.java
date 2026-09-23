package com.nfcplatform.lead.repository;

import com.nfcplatform.lead.entity.Lead;
import com.nfcplatform.lead.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByUuid(String uuid);

    @Query("SELECT l FROM Lead l WHERE (:status IS NULL OR l.status = :status) " +
            "AND (:search IS NULL OR LOWER(l.name) LIKE :search OR LOWER(l.email) LIKE :search " +
            "OR LOWER(l.company) LIKE :search)")
    Page<Lead> search(@Param("status") LeadStatus status, @Param("search") String search, Pageable pageable);
}
