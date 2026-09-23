package com.nfcplatform.audit.service;

import com.nfcplatform.audit.dto.AuditLogFiltersResponse;
import com.nfcplatform.audit.dto.AuditLogResponse;
import com.nfcplatform.audit.entity.AuditLog;
import com.nfcplatform.audit.repository.AuditLogRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.user.entity.User;
import com.nfcplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only viewer for the append-only audit trail written by {@link AuditService}. Kept as
 * a separate service (rather than adding query methods to AuditService itself) since writing
 * and reading the trail are different concerns with different callers - every feature module
 * writes, only the admin audit log page reads.
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action, String entityType, String actorUuid, String search,
                                                   Instant from, Instant to, Pageable pageable) {
        Long actorUserId = null;
        if (actorUuid != null && !actorUuid.isBlank()) {
            actorUserId = userRepository.findByUuidAndDeletedAtIsNull(actorUuid).map(User::getId).orElse(-1L);
        }
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<AuditLog> page = auditLogRepository.search(blankToNull(action), blankToNull(entityType), actorUserId,
                from, to, searchPattern, pageable);

        List<Long> actorIds = page.getContent().stream()
                .map(AuditLog::getActorUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> emailsById = new HashMap<>();
        if (!actorIds.isEmpty()) {
            userRepository.findAllById(actorIds).forEach(u -> emailsById.put(u.getId(), u.getEmail()));
        }

        return PageResponse.of(page, log -> AuditLogResponse.from(log,
                log.getActorUserId() == null ? "System" : emailsById.getOrDefault(log.getActorUserId(), "Unknown")));
    }

    @Transactional(readOnly = true)
    public AuditLogFiltersResponse filters() {
        return new AuditLogFiltersResponse(auditLogRepository.findDistinctActions(),
                auditLogRepository.findDistinctEntityTypes());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
