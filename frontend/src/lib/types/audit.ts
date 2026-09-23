export interface AuditLog {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityUuid: string | null;
  ipAddress: string | null;
  metadataJson: string | null;
  createdAt: string;
}

export interface AuditLogFilters {
  actions: string[];
  entityTypes: string[];
}
