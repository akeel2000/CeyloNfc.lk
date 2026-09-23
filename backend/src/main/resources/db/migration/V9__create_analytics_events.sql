-- Privacy-friendly analytics events (docs/SECURITY.md, docs/ARCHITECTURE.md "analytics
-- performance"). No PII: device/browser/OS family only, parsed server-side from
-- User-Agent, never the raw header. Composite index matches the documented dashboard
-- range-query access pattern (client_id, created_at).

CREATE TABLE analytics_events (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid             VARCHAR(36)  NOT NULL,
    client_id        BIGINT       NOT NULL,
    nfc_card_id      BIGINT       NULL,
    qr_code_id       BIGINT       NULL,
    destination_id   BIGINT       NULL,
    event_type       VARCHAR(30)  NOT NULL,
    device_type      VARCHAR(20)  NULL,
    browser          VARCHAR(50)  NULL,
    os               VARCHAR(50)  NULL,
    referrer         VARCHAR(500) NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_analytics_events_uuid UNIQUE (uuid),
    CONSTRAINT fk_analytics_events_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_analytics_events_client_created ON analytics_events (client_id, created_at);
CREATE INDEX idx_analytics_events_nfc_card ON analytics_events (nfc_card_id);
CREATE INDEX idx_analytics_events_qr_code ON analytics_events (qr_code_id);
