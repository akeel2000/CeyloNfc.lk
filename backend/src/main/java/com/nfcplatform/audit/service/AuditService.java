package com.nfcplatform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfcplatform.audit.entity.AuditLog;
import com.nfcplatform.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Append-only audit trail writer. Never exposes update/delete operations —
 * audit_logs rows are immutable once written.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(Long actorUserId, String action, String entityType, String entityUuid,
                        String ipAddress, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(actorUserId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityUuid(entityUuid);
        entry.setIpAddress(ipAddress);
        if (metadata != null && !metadata.isEmpty()) {
            try {
                entry.setMetadataJson(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("Failed to serialize audit metadata for action {}", action, e);
            }
        }
        auditLogRepository.save(entry);
    }
}
