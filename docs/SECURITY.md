# Security Architecture

## Authentication

- Passwords hashed with BCrypt (Spring Security `BCryptPasswordEncoder`, strength 12).
- Access JWT: 15 min expiry, signed HS256 with a 256-bit secret from `JWT_SECRET` env var.
  Claims limited to `sub` (user UUID), `role`, `sid` (session id) — no PII.
- Refresh token: opaque random value, 30 day expiry, rotated on every use, hashed
  (SHA-256) before storage in `refresh_tokens`. Old token is marked revoked and linked to
  its replacement so reuse of a revoked token invalidates the entire chain (theft detection).
- Both tokens delivered as `HttpOnly; Secure (prod); SameSite=Lax` cookies. Never placed in
  response JSON bodies or localStorage.
- Password change / logout-all-devices revokes every refresh token row for that user.
- Failed login lockout: configurable threshold (default 5) with a temporary lockout window
  (default 15 min), tracked on `users.failed_login_attempts` / `locked_until`.

## Authorization (RBAC)

- Roles: `SUPER_ADMIN`, `ADMIN`, `CLIENT` (future: `STAFF`, `CLIENT_STAFF`).
- Permissions are granular string codes (see `ROLE_PERMISSION_MATRIX.md`). `SUPER_ADMIN`
  implicitly has every permission; `ADMIN` permissions are explicit grants stored in
  `admin_permission_overrides`.
- Enforced with method security (`@PreAuthorize`) at the service layer, never only in the
  controller and never only in the frontend.

## Tenant isolation (critical)

Every client-owned entity carries `client_id`. All client-scoped repository queries are
written as `findByUuidAndClientId(...)`, never `findByUuid(...)` followed by an
after-the-fact check. The current client's id is resolved server-side from the authenticated
principal — it is never accepted as a client-supplied parameter for CLIENT-role requests.
This is the #1 tested invariant in the test suite (see "Critical security test" below).

## NFC / QR token security

- Generated with `SecureRandom`, ≥128 bits entropy, base62-encoded (no ambiguous chars).
- Only `SHA-256(token + SERVER_PEPPER)` is persisted (`token_hash`); pepper lives in env,
  never in the database.
- Redirect endpoints validate token format (length/charset) before doing any DB lookup, to
  cheaply reject junk without hitting the hot path unnecessarily.
- Destination URLs are validated: scheme must be `http`/`https` only; `javascript:`, `data:`,
  and malformed hosts are rejected outright. Internal destination types (PROFILE, MENU, etc.)
  resolve to platform-controlled URLs, so open-redirect risk is confined to the `CUSTOM_URL`
  destination type, which is validated the same way.

## Transport & headers

- CSRF: SameSite=Lax cookies + double-submit CSRF token (custom header) required on all
  state-changing (`POST/PUT/PATCH/DELETE`) requests from the browser app. `login` and
  `refresh` are exempted: `login` runs before any session exists (no CSRF token to present
  yet), and `refresh` is protected instead by its HttpOnly, unreadable-by-JS refresh cookie —
  a forged cross-site refresh call cannot exfiltrate the resulting tokens due to
  same-origin policy, so CSRF adds no protection there while breaking silent token renewal.
- CORS: explicit origin allow-list (`APP_FRONTEND_URL`), `credentials: true`, never `*`.
- Response headers: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: strict-origin-when-cross-origin`, `Strict-Transport-Security` (prod only),
  `Permissions-Policy`, `X-Frame-Options: DENY`.

## Input validation & injection

- Jakarta Bean Validation on every request DTO; Zod mirrors the same rules client-side as a
  UX layer only.
- Spring Data JPA / parameterized queries exclusively; no string-concatenated SQL.
- File uploads: MIME + extension + magic-byte signature check, server-generated filenames
  (never trust the original), size caps, SVGs rejected unless sanitized.

## Rate limiting

Redis-backed fixed-window counter, correct across multiple backend instances, on: login,
forgot-password, reset-password, public lead form, support ticket creation, and the public
redirect endpoints (`/t/{token}`, `/q/{token}`) to blunt token-guessing/enumeration. Falls back
to an in-memory Bucket4j bucket-per-key limiter whenever the Redis call itself fails (a 500ms
timeout keeps that fast, not a multi-second stall per request) - local dev keeps working without
a running Redis container, and a Redis outage degrades to per-instance limiting rather than
blocking every request these endpoints receive.

## Logging

SLF4J structured logs for auth events, permission denials, failed uploads, failed redirects.
Never logged: passwords, JWTs, refresh tokens, raw NFC/QR tokens, DB credentials.

## Critical security test (must exist and pass)

Client A authenticated, requesting any Client-B-owned resource by UUID
(`/api/v1/client/nfc-cards/{clientBCardUuid}`, and the same for profile, QR, menu, review
location, analytics, destination) must receive `403`/`404`, never Client B's data. See
`backend/src/test/.../TenantIsolationTest.java` (added once the client/nfc modules land).
