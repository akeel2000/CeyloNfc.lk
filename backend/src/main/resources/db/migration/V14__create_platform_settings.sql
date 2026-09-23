-- Single-row settings table (Phase 9 Settings module - see docs/PROJECT_PROGRESS.md and the
-- forward-looking comment in frontend/src/lib/config/brand.ts naming exactly these three
-- fields). Deliberately a fixed-schema single row, not a generic key/value store - only three
-- concrete values are actually consumed anywhere in the app today, and a generic store would
-- need its own validation layer for no real benefit at this size. Row id is always 1, enforced
-- in PlatformSettingsService rather than the schema (MySQL has no clean "exactly one row"
-- constraint short of a trigger).

CREATE TABLE platform_settings (
    id              BIGINT        NOT NULL PRIMARY KEY,
    site_name       VARCHAR(100)  NOT NULL DEFAULT 'CeyloNfc',
    support_email   VARCHAR(255)  NOT NULL DEFAULT 'hello@ceylonfc.com',
    tagline         VARCHAR(255)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO platform_settings (id, site_name, support_email, tagline)
VALUES (1, 'CeyloNfc', 'hello@ceylonfc.com', 'One Tap. One Connection. Unlimited Possibilities.');
