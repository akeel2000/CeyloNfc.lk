-- Users, roles, permissions and their relations.

CREATE TABLE users (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                    VARCHAR(36)      NOT NULL,
    email                   VARCHAR(255)  NOT NULL,
    phone                   VARCHAR(32)   NULL,
    password_hash           VARCHAR(255)  NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts   INT           NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP     NULL,
    must_change_password    BOOLEAN       NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMP     NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP     NULL,
    CONSTRAINT uq_users_uuid UNIQUE (uuid),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE roles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid         VARCHAR(36)      NOT NULL,
    code         VARCHAR(50)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(255)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_uuid UNIQUE (uuid),
    CONSTRAINT uq_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(100)  NOT NULL,
    description  VARCHAR(255)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Per-admin permission grants beyond their role defaults (SUPER_ADMIN needs none: implicit all).
CREATE TABLE admin_permission_overrides (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT  NOT NULL,
    permission_id  BIGINT  NOT NULL,
    granted        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_admin_permission_overrides UNIQUE (user_id, permission_id),
    CONSTRAINT fk_apo_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_apo_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                      VARCHAR(36)      NOT NULL,
    user_id                   BIGINT        NOT NULL,
    token_hash                VARCHAR(64)      NOT NULL,
    issued_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at                TIMESTAMP     NOT NULL,
    revoked_at                TIMESTAMP     NULL,
    replaced_by_token_hash    VARCHAR(64)      NULL,
    ip_address                VARCHAR(64)   NULL,
    user_agent                VARCHAR(255)  NULL,
    CONSTRAINT uq_refresh_tokens_uuid UNIQUE (uuid),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    token_hash   VARCHAR(64)    NOT NULL,
    expires_at   TIMESTAMP   NOT NULL,
    used_at      TIMESTAMP   NULL,
    ip_address   VARCHAR(64) NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
