-- Google Review locations: a client can have multiple (e.g. one per branch), each with
-- its own NFC cards/QR codes and analytics. destinations.google_review_location_id lets a
-- GOOGLE_REVIEW destination point at a specific one (see docs/TECHNICAL_DECISIONS.md for
-- why this is a typed FK column rather than the original spec's generic target_reference).

CREATE TABLE google_review_locations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)   NOT NULL,
    client_id           BIGINT        NOT NULL,
    business_name       VARCHAR(255)  NOT NULL,
    location_name       VARCHAR(255)  NULL,
    address             VARCHAR(500)  NULL,
    google_maps_url     VARCHAR(2048) NULL,
    google_review_url   VARCHAR(2048) NOT NULL,
    google_place_id     VARCHAR(255)  NULL,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_grl_uuid UNIQUE (uuid),
    CONSTRAINT fk_grl_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_grl_client ON google_review_locations (client_id);

ALTER TABLE destinations
    ADD COLUMN google_review_location_id BIGINT NULL AFTER external_url,
    ADD CONSTRAINT fk_destinations_review_location FOREIGN KEY (google_review_location_id) REFERENCES google_review_locations (id);

-- QR codes follow the exact same secure-token pattern as nfc_cards (token_hash only,
-- raw token shown once) so the redirect pipeline can be shared - see DestinationResolverService.
CREATE TABLE qr_codes (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid              VARCHAR(36)   NOT NULL,
    client_id         BIGINT        NOT NULL,
    destination_id    BIGINT        NOT NULL,
    token_hash        VARCHAR(64)   NOT NULL,
    name              VARCHAR(255)  NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    total_scans       BIGINT        NOT NULL DEFAULT 0,
    last_scanned_at   TIMESTAMP     NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_qr_codes_uuid UNIQUE (uuid),
    CONSTRAINT uq_qr_codes_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_qr_codes_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_qr_codes_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT chk_qr_codes_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_qr_codes_client ON qr_codes (client_id);
