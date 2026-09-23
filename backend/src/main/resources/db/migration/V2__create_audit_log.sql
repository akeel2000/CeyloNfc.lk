-- Append-only audit trail. No UPDATE/DELETE grants are used against this table
-- from the application layer (service layer only ever INSERTs).

CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id   BIGINT       NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_uuid     VARCHAR(36)     NULL,
    ip_address      VARCHAR(64)  NULL,
    metadata_json   JSON         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_uuid);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id);
