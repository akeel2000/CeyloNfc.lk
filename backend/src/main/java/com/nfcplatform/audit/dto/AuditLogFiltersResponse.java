package com.nfcplatform.audit.dto;

import java.util.List;

public record AuditLogFiltersResponse(List<String> actions, List<String> entityTypes) {
}
