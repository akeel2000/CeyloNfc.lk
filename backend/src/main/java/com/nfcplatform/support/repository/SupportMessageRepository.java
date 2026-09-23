package com.nfcplatform.support.repository;

import com.nfcplatform.support.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
