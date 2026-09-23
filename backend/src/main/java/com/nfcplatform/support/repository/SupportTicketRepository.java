package com.nfcplatform.support.repository;

import com.nfcplatform.support.entity.SupportTicket;
import com.nfcplatform.support.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByUuid(String uuid);

    Optional<SupportTicket> findByUuidAndClientId(String uuid, Long clientId);

    List<SupportTicket> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT t FROM SupportTicket t WHERE (:status IS NULL OR t.status = :status) " +
            "AND (:search IS NULL OR LOWER(t.subject) LIKE :search)")
    Page<SupportTicket> search(@Param("status") TicketStatus status, @Param("search") String search, Pageable pageable);
}
