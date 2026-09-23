# Database Design

MySQL 8, InnoDB, `utf8mb4`. All tables use `BIGINT AUTO_INCREMENT` primary keys plus a
`CHAR(36)` `uuid` column (unique, indexed) as the only externally-exposed identifier.
Timestamps are `TIMESTAMP` UTC, managed via JPA auditing (`created_at`, `updated_at`,
`created_by`, `updated_by`). Soft-delete (`deleted_at NULL`) is used for clients, profiles,
products and templates; audit logs are append-only and never soft-deleted.

## Phase 1 schema (this migration set)

### users
`id, uuid, email (unique), phone, password_hash, status(ACTIVE|LOCKED|DISABLED),
failed_login_attempts, locked_until, must_change_password, last_login_at,
created_at, updated_at, deleted_at`

### roles
`id, uuid, code(SUPER_ADMIN|ADMIN|CLIENT|STAFF|CLIENT_STAFF unique), name, description`

### permissions
`id, code(unique, e.g. CLIENT_VIEW), description`

### role_permissions
`role_id FK, permission_id FK` (composite PK)

### user_roles
`user_id FK, role_id FK` (composite PK) — a user may hold multiple roles, though the
seed data assigns exactly one (SUPER_ADMIN/ADMIN/CLIENT) per account.

### admin_permission_overrides
`id, user_id FK, permission_id FK, granted(boolean)` — lets Super Admin grant/revoke
individual permissions per Admin beyond their role defaults.

### refresh_tokens
`id, uuid, user_id FK, token_hash (unique), issued_at, expires_at, revoked_at,
replaced_by_token_hash, ip_address, user_agent`
Indexed on `(user_id)` and `(token_hash)`. Rotation: each refresh issues a new row and
sets `revoked_at`+`replaced_by_token_hash` on the old one.

### password_reset_tokens
`id, user_id FK, token_hash (unique), expires_at, used_at, ip_address`

### audit_logs
`id, actor_user_id FK NULL, action, entity_type, entity_uuid, ip_address, metadata_json,
created_at` — insert-only, no update/delete grants at the application layer.

## Indexing strategy (applies platform-wide as later modules land)

- Every `uuid` column: unique index.
- Every FK column (`client_id`, `destination_id`, etc.): index.
- `email` on users: unique index.
- `token_hash` on nfc_cards / qr_codes / refresh_tokens: unique index (hot lookup path).
- `slug` on profiles/menus: unique index.
- `analytics_events(client_id, created_at)`: composite index for dashboard range queries.
- `orders(order_number)`, `nfc_cards(serial_number)`: unique index.

## Later modules (tracked in PROJECT_PROGRESS.md, not yet migrated)

`clients, client_members, individual_profiles, company_profiles, social_links, nfc_cards,
destinations, qr_codes, google_review_locations, menus, menu_categories,
menu_items, analytics_events, package_plans, subscriptions, orders, order_items, products,
leads, support_tickets, support_messages, notifications, media, settings` — see
`ARCHITECTURE.md` for the entity field lists already specified for each; migrations will be
added as `V2__create_client_tables.sql`, `V3__create_profile_tables.sql`,
`V4__create_nfc_tables.sql`, etc., one focused file per module, never one giant migration.
