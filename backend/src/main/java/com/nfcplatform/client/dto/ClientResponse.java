package com.nfcplatform.client.dto;

import com.nfcplatform.client.entity.Client;

import java.time.Instant;

public record ClientResponse(
        String uuid,
        String type,
        String status,
        String displayName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getUuid(),
                client.getType().name(),
                client.getStatus().name(),
                client.getDisplayName(),
                client.getEmail(),
                client.getPhone(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
