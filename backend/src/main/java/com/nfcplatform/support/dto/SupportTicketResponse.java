package com.nfcplatform.support.dto;

import com.nfcplatform.support.entity.SupportTicket;

import java.time.Instant;
import java.util.List;

public record SupportTicketResponse(
        String uuid,
        String clientUuid,
        String clientDisplayName,
        String subject,
        String status,
        String priority,
        List<SupportMessageDto> messages,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupportTicketResponse from(SupportTicket ticket, String clientUuid, String clientDisplayName,
                                              List<SupportMessageDto> messages) {
        return new SupportTicketResponse(ticket.getUuid(), clientUuid, clientDisplayName, ticket.getSubject(),
                ticket.getStatus().name(), ticket.getPriority().name(), messages, ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
