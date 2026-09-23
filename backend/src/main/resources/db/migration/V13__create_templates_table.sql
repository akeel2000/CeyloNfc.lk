-- Template gallery (Phase 3 remainder - see docs/PROJECT_PROGRESS.md). Admin authors the
-- gallery (create/update/deactivate, no delete - matches package_plans's active-flag
-- convention), clients select/apply one to their own profile (TEMPLATE_MANAGE: "select/apply
-- only" per docs/ROLE_PERMISSION_MATRIX.md). ON DELETE SET NULL on the profile FKs so a
-- template row can still be removed by hand without leaving a dangling reference - profiles
-- just revert to the default look.

CREATE TABLE templates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            VARCHAR(36)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    description     VARCHAR(1000) NULL,
    preview_image   VARCHAR(2048) NULL,
    primary_color   VARCHAR(7)    NOT NULL DEFAULT '#4338ca',
    layout          VARCHAR(20)   NOT NULL DEFAULT 'CLASSIC',
    premium         BOOLEAN       NOT NULL DEFAULT FALSE,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_templates_uuid UNIQUE (uuid),
    CONSTRAINT chk_templates_layout CHECK (layout IN ('CLASSIC', 'MINIMAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE individual_profiles
    ADD COLUMN template_id BIGINT NULL AFTER company_name,
    ADD CONSTRAINT fk_individual_profiles_template FOREIGN KEY (template_id) REFERENCES templates (id) ON DELETE SET NULL;

ALTER TABLE company_profiles
    ADD COLUMN template_id BIGINT NULL AFTER industry,
    ADD CONSTRAINT fk_company_profiles_template FOREIGN KEY (template_id) REFERENCES templates (id) ON DELETE SET NULL;
