package com.nfcplatform.audit.repository;

import com.nfcplatform.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:actorUserId IS NULL OR a.actorUserId = :actorUserId) " +
            "AND (:from IS NULL OR a.createdAt >= :from) " +
            "AND (:to IS NULL OR a.createdAt <= :to) " +
            "AND (:search IS NULL OR LOWER(a.entityUuid) LIKE :search OR LOWER(a.action) LIKE :search)")
    Page<AuditLog> search(@Param("action") String action, @Param("entityType") String entityType,
                           @Param("actorUserId") Long actorUserId, @Param("from") Instant from,
                           @Param("to") Instant to, @Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a ORDER BY a.entityType")
    List<String> findDistinctEntityTypes();
}
