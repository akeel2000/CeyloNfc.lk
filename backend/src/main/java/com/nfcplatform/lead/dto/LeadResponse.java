package com.nfcplatform.lead.dto;

import com.nfcplatform.lead.entity.Lead;

import java.time.Instant;

public record LeadResponse(
        String uuid,
        String name,
        String email,
        String phone,
        String company,
        String message,
        String source,
        String status,
        String notes,
        Instant createdAt
) {
    public static LeadResponse from(Lead lead) {
        return new LeadResponse(lead.getUuid(), lead.getName(), lead.getEmail(), lead.getPhone(),
                lead.getCompany(), lead.getMessage(), lead.getSource(), lead.getStatus().name(), lead.getNotes(),
                lead.getCreatedAt());
    }
}
