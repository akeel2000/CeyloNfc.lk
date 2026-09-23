# Technical Decisions

Log of significant architectural decisions and why they were made. Newest first.

## Notifications: one row per recipient, one endpoint set for both roles

**Decision:** `NotificationService.notifyUsers` writes a separate `Notification` row per
recipient rather than one row referencing a role/audience, and `NotificationController` has
no admin/client split - it is scoped entirely to `@AuthenticationPrincipal`, unlike every
other module in this codebase which has parallel `Admin*Controller`/`Client*Controller`
classes.
**Why:** A notification is addressed to a *user*, not a *tenant* - admins and clients read
the exact same shape of data (their own unread count and list), so splitting the controller
would just duplicate the same three methods twice for no isolation benefit (there's no
cross-user data to leak). Fan-out on write (one row per recipient) keeps "unread count" a
single indexed `COUNT(*) WHERE user_id = ? AND read_at IS NULL` instead of needing a
separate per-user read-tracking join table for a broadcast row - simpler at the cost of
some write amplification when notifying many admins at once, an acceptable trade for a
volume this low (new leads/tickets, not a firehose).
**How to apply:** Any future notification-worthy event (subscription expiring, order
shipped, etc.) should call `notify`/`notifyUsers` the same way `LeadService` and
`SupportService` do - resolve recipients, fire the notification, don't build bespoke
per-feature notification plumbing.

## Support ticket status auto-transitions on reply

**Decision:** `SupportService` flips a ticket's status automatically when a message is
added: an admin reply on an `OPEN` ticket moves it to `IN_PROGRESS`; a client reply on a
`RESOLVED` or `CLOSED` ticket reopens it to `OPEN`. Both are side effects of `addMessage*`,
not a separate action the caller has to remember to take.
**Why:** Without this, a support queue would need admins to manually re-open tickets
whenever a customer replies after a ticket was marked resolved - an easy way for a
customer's follow-up to silently sit unnoticed on a "closed" ticket. Making the reopen
automatic on the read path that matters (a client message arriving) means the status field
can be trusted as "does this need attention" without an admin manually maintaining it.
**How to apply:** `fromSupportStaff` on a message is derived at read time (sender's user id
compared against the ticket's client owner id) rather than stored as a column, so it can
never drift out of sync with who actually holds the client account - the same
derive-don't-duplicate principle should apply to any future flag on `SupportMessage`.

## Subscription card-limit enforcement lives in the service layer, not the controller

**Decision:** `SubscriptionService.assertCanAssignCard(clientId)` is called from inside
`NfcCardService.assign()`, immediately before the card's `clientId` is set - not as a
`@PreAuthorize`/filter-level check, and not duplicated in the frontend beyond a normal error
toast.
**Why:** Card limits are a business rule about *state* (how many cards a client already has
relative to their plan), not an access-control rule about *identity* - `@PreAuthorize`/SpEL
can't express "count existing rows and compare to a value looked up on another entity"
without contorting the security expression language for something a plain Java method
already does clearly. Keeping it in the service also means it's enforced for every future
caller of `assign()` (admin UI today, any bulk-assign tool later) without each caller having
to remember to re-check.
**How to apply:** Only re-checked on *new* client assignment (`card.getClientId() == null ||
!card.getClientId().equals(client.getId())`), not on idempotent re-assignment - re-assigning
a card to the client it's already on must never fail just because that client happens to be
at or over its limit already (e.g. after an admin lowers a plan's limit retroactively). A
missing subscription record is treated as unmanaged/unlimited, not zero - it represents a
client an admin hasn't put on a plan yet, not a client entitled to nothing.

## Zod `.transform()` fields kept out of RHF form-value types

**Decision:** The package plan form's "blank = unlimited" numeric fields (`cardLimit`,
`profileLimit`, etc.) are typed as plain `number | undefined` in the Zod schema (and
therefore in `useForm`'s generic) with **no** `.transform()` in the schema itself. The
NaN/undefined -> `null` conversion for the API payload happens in a separate
`toPackagePlanPayload()` helper called right before the mutation fires.
**Why:** `zodResolver`'s `Resolver<TFieldValues>` type is generated from the schema's
*output* type, but RHF's `useForm<TFieldValues>` - register, defaultValues, watch - all
operate on the *input* type. When a schema uses `.transform()`, those two types diverge
(e.g. input `number | undefined` vs. output `number | null`), and declaring `useForm` with
either type alone produces a genuine `tsc` type error against the other side, not a
suppressible lint warning. This was hit and fixed while building Phase 8's package plan form.
**How to apply:** Any future form field needing a schema-level transform (trimming,
NaN-to-null, string-to-Date, etc.) should keep the RHF-facing schema transform-free and do
the transform in a plain function between `handleSubmit`'s callback and the API call, the
same way `toPackagePlanPayload()` does - not by reaching for `z.input`/`z.output` gymnastics
in the component.

## Explicit header forwarding in the `/t/[token]` and `/q/[token]` route handlers

**Decision:** Both route handlers now explicitly copy `User-Agent`, `Referer`, and
`X-Forwarded-For` from the incoming request onto the server-side `fetch()` call to the
backend.
**Why:** `fetch()` called from inside a Next.js Route Handler does not automatically forward
the original request's headers - discovered while building Phase 7 analytics, when every
event would otherwise have recorded the Next.js server's own User-Agent instead of the real
visitor's device, and the backend's per-IP rate limiter would have seen only the Next.js
server's IP for every visitor (one shared bucket instead of one per real visitor). Caught by
testing the actual frontend path end-to-end rather than only curling the backend directly -
a curl-only test can't surface this class of bug since curl talks to the backend directly
and has no intermediate server-side fetch hop to lose headers at.
**How to apply:** Any future route handler that proxies a request server-side (not just
these two) needs the same explicit header-forwarding treatment if the backend cares about
the original client's identity (IP, UA, or any other request-scoped header).

## Analytics events recorded async, off the redirect hot path

**Decision:** `AnalyticsService.recordEvent` is `@Async` (Spring's `@EnableAsync`, already
on from Phase 1) and called after the redirect target is already resolved - a slow/failed
analytics insert can never delay or break a visitor's tap/scan redirect.
**Why:** NFC_FLOW.md commits to this explicitly ("record tap asynchronously... does not
block redirect"); the redirect endpoint is the single highest-traffic path in the system.
**Trade-off:** Event counts can very rarely lag or (on a JVM crash mid-write) drop an event;
the per-card/per-QR counter (`total_taps`/`total_scans`) is still updated synchronously in
the same transaction as the redirect, so the primary-use-case numbers (shown on the card/QR
detail pages) are never at risk - only the secondary analytics-event detail (device/browser
breakdown) has this small async window.

## `/t/[token]` resolved server-side in the Next.js route handler, not blind-forwarded

**Decision:** The frontend's `GET /t/{token}` route calls the backend redirect endpoint
server-side with `redirect: "manual"`, reads the `Location` header itself, and only then
redirects the visitor's browser - either straight to the destination (on `302`) or to
`/card-unavailable?reason=...` (on `410`/`429`/other). It does not simply 307-forward the
browser to the backend URL.
**Why:** A blind forward would leave the browser showing raw backend JSON error bodies for
suspended/unknown cards, and would expose the backend's own URL/port to visitors. Reading
the response server-side lets a proper branded error page render instead, at the cost of one
extra server-to-server hop per tap.
**Note:** In production, Nginx can route `/t/*` straight to the backend and skip the Next.js
hop entirely (see DEPLOYMENT.md) - this passthrough exists for environments without that
reverse-proxy layer.

## In-memory Bucket4j rate limiting on the public redirect endpoint

**Decision:** `RateLimiterService` is a plain `ConcurrentHashMap<String, Bucket>` (Bucket4j),
applied per-IP to `GET /api/v1/public/t/{token}` (30 requests/minute/IP).
**Why:** SECURITY.md commits to rate-limiting the redirect endpoints specifically because
they're exposed to token-guessing/enumeration attempts, and this was a real gap from Phase 1.
**Trade-off:** In-memory means limits are per-instance, not global, the moment there's more
than one backend replica. Swap `RateLimiterService`'s bucket store for a Redis-backed Bucket4j
proxy manager before running multiple instances in production - this was called out as
acceptable for the dev/single-instance stage in SECURITY.md's own rate-limiting section.

## Color system: indigo brand accent over pure black/white

**Decision:** Introduced a vivid indigo (`#4338ca` light / `#818cf8` dark) as `--primary`,
plus dedicated `--success`/`--warning`/`--info` tokens and a 5-color chart palette, layered
on top of the existing neutral black/white/gray structural colors (backgrounds, cards,
borders, body text stay neutral).
**Why:** User feedback ("add attractive colors for user") - a pure black/white UI read as
flat for day-to-day dashboard use. Kept the neutral foundation (cards/borders/text) so the
product doesn't lose its premium feel, and confined color to CTAs, active nav states, status
badges and chart series where it adds real signal (e.g. status badges: ACTIVE=green,
SUSPENDED=red, PENDING=amber).

## Per-admin permissions merged via PrincipalFactory

**Decision:** `UserPrincipal` now carries role-derived permissions **and** per-admin
overrides (`admin_permission_overrides`), merged in one place: `PrincipalFactory.build(User)`.
Every authentication path (login, refresh, the per-request JWT filter, `CustomUserDetailsService`)
goes through this factory instead of `new UserPrincipal(user)` directly. Permission codes are
also exposed as Spring Security authorities (alongside `ROLE_*`), so controllers use plain
`@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority(T(...PermissionCodes).CLIENT_VIEW)")`
rather than a custom permission-evaluator bean.
**Why:** `ROLE_PERMISSION_MATRIX.md` always specified per-admin granular permissions, but
Phase 1 only wired role-level checks. Centralizing the merge avoids the bug class where one
auth path (e.g. login) has the override and another (e.g. the JWT filter) doesn't.
**Deferred:** A Super-Admin UI screen to grant/revoke `admin_permission_overrides` per admin
is not built yet - only `SUPER_ADMIN` is seeded/usable today. Grant overrides via SQL/seed
until that screen lands.

## Client.displayName added beyond the original schema doc

**Decision:** `clients.display_name` (`VARCHAR(255) NOT NULL`) was added to the `Client`
entity even though `DATABASE.md`'s original field list for Client didn't include it.
**Why:** `IndividualProfile`/`CompanyProfile` (Phase 3, not yet built) are where a client's
real name/company name belong per the architecture doc, but the admin Client list needs
*some* human-readable label before Profile exists. Capturing it at client-creation time keeps
the admin UI usable now instead of shipping a client list that only shows UUIDs/emails.
**Revisit when:** Phase 3 profile module lands - display_name should stay as a fallback label
even after profiles exist (e.g. for clients who haven't published a profile yet).

## Java 25 runtime

**Decision:** Target Java 25, the latest LTS release, for the backend runtime.
**Why:** Java 25 is supported by the current Spring Boot 3.3 application and provides the
current long-term-supported JDK baseline for development, CI, and deployment. The upgrade
requires only the Maven target and runtime image changes; application source compatibility is
preserved.
**Revisit when:** A newer Java LTS release becomes the supported deployment baseline.

## Stack: Spring Boot + MySQL (not Next.js full-stack + Postgres/Prisma)

**Decision:** Backend is a standalone Spring Boot REST API with MySQL 8 + Flyway.
Next.js is a pure frontend/API-consumer, never touching the database directly.
**Why:** The user was given two conflicting master prompts in one session (one Java/Spring/MySQL,
one Next.js-only/Postgres/Prisma) and explicitly chose the Spring Boot + MySQL stack when asked
to disambiguate.
**Revisit when:** Only if the user explicitly asks to migrate.

## JWT delivered via HttpOnly, Secure, SameSite cookies (not localStorage)

**Decision:** Access token (short-lived, ~15 min) and refresh token (long-lived, rotated)
are both set as HttpOnly cookies by the backend. The frontend never reads the raw JWT.
**Why:** Eliminates XSS-based token theft (the dominant risk with localStorage JWTs).
Refresh token rotation + server-side metadata table allows revocation, which pure stateless
JWT-in-localStorage cannot do cleanly.
**Trade-off:** Requires CSRF protection since cookies are sent automatically. Mitigated with
`SameSite=Lax` cookies plus a double-submit CSRF token header on state-changing requests, and
strict CORS with `Access-Control-Allow-Credentials: true` limited to the known frontend origin.

## UUID public identifiers, BIGINT internal primary keys

**Decision:** Every entity has an internal `BIGINT AUTO_INCREMENT` primary key for joins/indexes,
plus a `BINARY(16)`/`CHAR(36)` `uuid` column that is the only identifier ever exposed via the API.
**Why:** Sequential IDs leak business volume (competitors could infer client/card counts) and
enable enumeration/IDOR attempts. UUIDs as external identifiers close that off while keeping
BIGINT join performance internally.

## NFC/QR token storage: hash, not plaintext

**Decision:** NFC and QR tokens are generated with `SecureRandom` (128+ bits of entropy, base62
encoded), and only `SHA-256(token + server_pepper)` is stored in `token_hash`. The raw token is
shown once at creation/write time and is otherwise unrecoverable from the database.
**Why:** A database leak must not hand an attacker working NFC/QR redirect URLs. Hashing with a
server-side pepper (env var, not in DB) means stolen rows alone are insufficient to forge valid
tokens the way a bcrypt-hashed password protects against credential reuse.

## Redirect hot path avoids loading full aggregates

**Decision:** `GET /api/v1/public/t/{token}` only loads a narrow projection (card status, client
status, destination target) — never the full Client/Profile/Menu graph.
**Why:** This endpoint is the highest-traffic, latency-sensitive path in the system (every
physical tap). Analytics event writes are dispatched asynchronously so they never block the
redirect response.

## ddl-auto=validate, Flyway owns schema

**Decision:** Hibernate `ddl-auto` is `validate` in all profiles; all schema changes go through
versioned Flyway migrations (`V1__...sql`, `V2__...sql`, ...), one focused migration per module.
**Why:** Auto-DDL is unsafe for a commercial product — silent, unreviewed schema drift. Flyway
gives an auditable, reproducible, rollback-aware schema history suitable for CI/CD.
