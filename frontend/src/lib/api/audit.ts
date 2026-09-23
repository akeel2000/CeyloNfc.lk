import { apiClient } from "@/lib/api/client";
import type { AuditLog, AuditLogFilters } from "@/lib/types/audit";
import type { PageResponse } from "@/lib/types/client";

export interface AuditLogListParams {
  page?: number;
  size?: number;
  action?: string;
  entityType?: string;
  search?: string;
  from?: string;
  to?: string;
}

function buildQuery(params: AuditLogListParams): string {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  if (params.action) query.set("action", params.action);
  if (params.entityType) query.set("entityType", params.entityType);
  if (params.search) query.set("search", params.search);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const auditLogsApi = {
  list: (params: AuditLogListParams = {}) =>
    apiClient.get<PageResponse<AuditLog>>(`/admin/audit-logs${buildQuery(params)}`),
  filters: () => apiClient.get<AuditLogFilters>("/admin/audit-logs/filters"),
};
