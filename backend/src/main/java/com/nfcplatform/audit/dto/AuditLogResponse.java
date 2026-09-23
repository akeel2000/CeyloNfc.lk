package com.nfcplatform.audit.dto;

import com.nfcplatform.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String entityType,
        String entityUuid,
        String ipAddress,
        String metadataJson,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log, String actorEmail) {
        return new AuditLogResponse(log.getId(), actorEmail, log.getAction(), log.getEntityType(),
                log.getEntityUuid(), log.getIpAddress(), log.getMetadataJson(), log.getCreatedAt());
    }
}
