-- One client has at most one individual_profile OR one company_profile, matching their
-- client.type (enforced in the service layer, not the schema, since MySQL can't easily
-- express "exactly one of two tables" as a constraint). template_id is omitted - the
-- Template module (Phase 3 polish) doesn't exist yet; see docs/TECHNICAL_DECISIONS.md.

CREATE TABLE individual_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            VARCHAR(36)   NOT NULL,
    client_id       BIGINT        NOT NULL,
    slug            VARCHAR(100)  NOT NULL,
    full_name       VARCHAR(255)  NOT NULL,
    job_title       VARCHAR(255)  NULL,
    company_name    VARCHAR(255)  NULL,
    bio             VARCHAR(1000) NULL,
    profile_image   VARCHAR(2048) NULL,
    cover_image     VARCHAR(2048) NULL,
    phone           VARCHAR(32)   NULL,
    whatsapp        VARCHAR(32)   NULL,
    email           VARCHAR(255)  NULL,
    website         VARCHAR(2048) NULL,
    address         VARCHAR(500)  NULL,
    city            VARCHAR(100)  NULL,
    country         VARCHAR(100)  NULL,
    published       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_individual_profiles_uuid UNIQUE (uuid),
    CONSTRAINT uq_individual_profiles_client UNIQUE (client_id),
    CONSTRAINT uq_individual_profiles_slug UNIQUE (slug),
    CONSTRAINT fk_individual_profiles_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE company_profiles (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                  VARCHAR(36)   NOT NULL,
    client_id             BIGINT        NOT NULL,
    slug                  VARCHAR(100)  NOT NULL,
    company_name          VARCHAR(255)  NOT NULL,
    industry              VARCHAR(255)  NULL,
    description           VARCHAR(1000) NULL,
    logo                  VARCHAR(2048) NULL,
    cover_image           VARCHAR(2048) NULL,
    phone                 VARCHAR(32)   NULL,
    whatsapp              VARCHAR(32)   NULL,
    email                 VARCHAR(255)  NULL,
    website               VARCHAR(2048) NULL,
    address               VARCHAR(500)  NULL,
    city                  VARCHAR(100)  NULL,
    country               VARCHAR(100)  NULL,
    google_maps_url       VARCHAR(2048) NULL,
    registration_number   VARCHAR(100)  NULL,
    published             BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_company_profiles_uuid UNIQUE (uuid),
    CONSTRAINT uq_company_profiles_client UNIQUE (client_id),
    CONSTRAINT uq_company_profiles_slug UNIQUE (slug),
    CONSTRAINT fk_company_profiles_client FOREIGN KEY (client_id) REFERENCES clients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE social_links (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id       BIGINT        NOT NULL,
    platform        VARCHAR(20)   NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    display_order   INT           NOT NULL DEFAULT 0,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_social_links_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_social_links_client ON social_links (client_id);
