-- Clients (tenants). Every client is owned by exactly one user account (login
-- credentials); ClientMember (multi-user-per-client) is deferred to a later phase.
--
-- display_name is a deliberate, documented addition beyond the original Client
-- field list in docs/DATABASE.md: it lets the admin UI show a usable client list
-- before the Profile module (individual_profiles/company_profiles) exists. See
-- docs/TECHNICAL_DECISIONS.md.

CREATE TABLE clients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            VARCHAR(36)   NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    display_name    VARCHAR(255)  NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    phone           VARCHAR(32)   NULL,
    owner_user_id   BIGINT        NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP     NULL,
    CONSTRAINT uq_clients_uuid UNIQUE (uuid),
    CONSTRAINT uq_clients_owner_user UNIQUE (owner_user_id),
    CONSTRAINT fk_clients_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT chk_clients_type CHECK (type IN ('INDIVIDUAL', 'BUSINESS')),
    CONSTRAINT chk_clients_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_clients_status ON clients (status);
CREATE INDEX idx_clients_email ON clients (email);
CREATE INDEX idx_clients_display_name ON clients (display_name);
