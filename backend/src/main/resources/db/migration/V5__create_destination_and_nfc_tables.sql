-- Destinations: what an NFC card/QR code resolves to. Kept generic so a card's physical
-- URL never has to change when a client updates where it points (see docs/NFC_FLOW.md).
--
-- Phase 4 implements only the URL-holding destination types (WEBSITE, WHATSAPP, SOCIAL,
-- CUSTOM_URL). PROFILE/COMPANY_PROFILE/MENU/GOOGLE_REVIEW/VCARD are reserved values the
-- column already supports, but resolving them requires modules not yet built (Phase 3/5/6);
-- the service layer rejects creating a destination of those types for now rather than
-- faking a redirect. See docs/TECHNICAL_DECISIONS.md.

CREATE TABLE destinations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            VARCHAR(36)   NOT NULL,
    client_id       BIGINT        NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    external_url    VARCHAR(2048) NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_destinations_uuid UNIQUE (uuid),
    CONSTRAINT fk_destinations_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_destinations_client ON destinations (client_id);

-- NFC cards. token_hash is SHA-256(rawToken + server pepper); the raw token is only ever
-- returned once, at registration time, and is not recoverable afterwards (see SECURITY.md).
CREATE TABLE nfc_cards (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid              VARCHAR(36)   NOT NULL,
    serial_number     VARCHAR(100)  NOT NULL,
    token_hash        VARCHAR(64)   NOT NULL,
    client_id         BIGINT        NULL,
    destination_id    BIGINT        NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'UNASSIGNED',
    notes             VARCHAR(500)  NULL,
    activated_at      TIMESTAMP     NULL,
    last_tapped_at    TIMESTAMP     NULL,
    total_taps        BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_nfc_cards_uuid UNIQUE (uuid),
    CONSTRAINT uq_nfc_cards_serial UNIQUE (serial_number),
    CONSTRAINT uq_nfc_cards_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_nfc_cards_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_nfc_cards_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT chk_nfc_cards_status CHECK (status IN ('UNASSIGNED', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOST', 'EXPIRED', 'REPLACED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_nfc_cards_client ON nfc_cards (client_id);
CREATE INDEX idx_nfc_cards_status ON nfc_cards (status);
