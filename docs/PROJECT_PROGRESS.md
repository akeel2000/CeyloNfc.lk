# Project Progress

Stack confirmed with user: **Java 17 (Spring Boot) + MySQL + Next.js**, per `TECHNICAL_DECISIONS.md`.

## Post-roadmap: premium public profile redesign (2026-08-19)

User-requested frontend-only redesign of the public NFC profile preview page (`/p/[slug]` and
`/company/[slug]`) into a "3D person/logo + orbiting social icons" premium mobile-first
experience. Explicitly scoped to UI/UX only - no backend, database, API contract, or
admin/client dashboard changes, and the task's own spec called for reusing 100% of existing data
rather than inventing new fields.

- Rewrote `features/profile/public-profile-card.tsx` (same `PublicProfileCard({ profile,
  vcardPath })` signature, so neither `app/p/[slug]/page.tsx` nor `app/company/[slug]/page.tsx`
  needed to change) plus two new files: `orbiting-social-ring.tsx` (the orbit layout/animation)
  and `social-icons.tsx` (a `SocialPlatform -> {label, Icon, tint}` map - lucide-react dropped
  brand-logo icons for trademark reasons, confirmed by checking the installed package's exports
  directly rather than trusting the stale comment in the old code; used lucide generics that
  read correctly for each platform - `Camera` for Instagram, `PlayCircle` for YouTube, `Music2`
  for TikTok, lucide's own `X` glyph for X - plus two hand-drawn letterform glyphs for
  Facebook/LinkedIn, avoiding a new icon-pack dependency for two icons).
- **Audited exactly what data the public endpoint actually returns before designing anything**:
  `PublicProfileResponse`/`PublicProfile` only ever carries name/title/company/industry/bio,
  profile/cover/logo images, phone/whatsapp/email/website, address/city/country, template
  layout+color, and `socialLinks[]` - there is no services/portfolio/products/gallery/team/
  reviews/opening-hours data on this endpoint (or anywhere in the schema tied to a public
  profile). The redesign only builds sections backed by real fields (hero, About, "Connect With
  Me", Contact, quick actions) and simply omits any section spec'd in the request that has no
  backing data, rather than inventing placeholder content - each section already independently
  no-ops when its field is empty, matching the existing card's established pattern.
  `profile.type` is `INDIVIDUAL | BUSINESS` (not the "company/freelancer/individual/business"
  four-way split the request described) - built exactly those two hero variants.
- Orbiting social ring: pure CSS, one `@keyframes orbit-spin` per icon computed from
  `rotate(angle) translateX(radius) rotate(-angle)` - the trailing counter-rotation exactly
  cancels the leading one every frame, so the icon glyph never visually rotates while its
  position travels the full circle; `animation-play-state: paused` on hover/focus (hover
  naturally bubbles to the ancestor, no extra JS needed). Orbit items are built from
  `profile.socialLinks` (already server-filtered to `enabled` and sorted by `displayOrder`) plus
  `profile.website` as an extra orbit entry, exactly mirroring what quick-actions/contact
  already surface - no new data sources.
  Reused the existing `qrcode`-backed `QrImage` component (`features/qr/qr-image.tsx`, already
  used by the admin/client QR feature) for the new top-bar QR button rather than adding a
  dependency; Share uses the standard Web Share API with a clipboard-copy fallback (`sonner`
  toast, already mounted globally in `app/layout.tsx`) - both pure frontend, no backend change.
  Scroll-driven hero shrink is a rAF-throttled scroll listener applying only `transform`/`opacity`
  (no layout-triggering properties); the effect is skipped entirely up front when
  `prefers-reduced-motion: reduce` matches, and the same media query (already global in
  `globals.css`) zeroes out the orbit/float/particle CSS animations for free.
- Scoped every new visual to the page itself rather than any shared/global styling: added a
  handful of purely-additive, uniquely-named keyframes/utility classes to the bottom of
  `globals.css` (`.orbit-icon`, `.hero-particles`, `.glass-panel`, etc. - referenced nowhere
  else) and wrapped just this component's root in Tailwind's `.dark` class (this app has no
  theme toggle - `.dark` was defined in `globals.css` but never applied anywhere - so scoping it
  to this one subtree cleanly gets every existing UI primitive, Button/Dialog included, resolving
  dark-palette tokens with zero risk to any other page, which stays exactly as before). Confirmed
  via grep that `PublicProfileCard` has exactly two importers (the two page routes above) and that
  the editor's separate live-preview mockup (`phone-preview.tsx`, used inside the admin/client
  dashboard) is a hand-rolled component that never imported this one - so the dashboard is
  provably untouched.
- Verified: `npm run typecheck`/`lint` clean; confirmed via `curl` against real SSR HTML that a
  data-sparse company profile (only phone+email set, no bio/logo/social links) correctly omits
  the About/Connect sections with no phantom empty cards, while a data-rich individual profile
  (bio + website, no social links) correctly renders exactly one orbit icon/Connect entry: then
  visually verified both in a real browser at a mobile viewport - orbit animation confirmed
  actually live (icon position differed between two screenshots taken seconds apart), QR dialog
  renders a real scannable code of the live profile URL, and the business-profile hero correctly
  shows the company logo tile instead of a person avatar with no orbit ring or Connect button
  when the profile has no social/website data.

## Post-roadmap: deep interactive admin audit - subscription audit trail + Orders dropdown fix (2026-08-18)

Follow-up audit pass that went beyond page-load checks to actually click through and exercise
every admin mutation (change plans, update order/payment status, etc.), per explicit user
request to keep auditing more deeply after two shallower passes had already been done. Found and
fixed two real bugs:

- **Subscription changes were the only admin mutation on the whole platform with zero audit
  trail.** Every sibling admin service (`ClientService`, `NfcCardService`, `AdminUserService`,
  `MenuService`, ...) already calls `AuditService.record` on every mutation; `SubscriptionService
  .assignForClient` never did. Fixed by injecting `AuditService` and recording a
  `SUBSCRIPTION_ASSIGN` entry (client uuid, actor, IP, plan name + status) on every plan
  assignment, threading `UserPrincipal actor` and `HttpServletRequest` through
  `AdminSubscriptionController`'s `POST` endpoint. New regression test
  `assignForClientRecordsAnAuditEntry` in `SubscriptionServiceTest`. Verified live: assigning a
  plan now produces a matching row in `audit_logs` with the correct metadata.
- **Orders page's combined order-status/payment-status dropdown rendered pinned to the top-right
  of the viewport**, overlapping the notification bell, instead of anchored under its trigger
  button. Root cause: the shared `DropdownMenuContent` (`components/ui/dropdown-menu.tsx`) was
  missing the standard shadcn/ui height-cap classes (`max-h-(--radix-dropdown-menu-content-
  available-height) overflow-y-auto overflow-x-hidden`). The Orders menu combines 9 order
  statuses + 4 payment statuses in one list (~500px unconstrained height), and without a height
  cap Radix's collision/flip positioning logic couldn't find anywhere sensible to place it in a
  short viewport, so it fell back to a corner. No other menu in the app has enough items to hit
  this, so this was previously invisible everywhere else. Fixed by adding the missing classes to
  the shared primitive (the correct place, not a per-page workaround) - verified live that the
  menu now anchors correctly under the trigger, scrolls internally, and that selecting a status
  still updates the order correctly.

Full backend suite re-verified after both fixes: 243/243 unit tests pass (same 2 pre-existing
Testcontainers/Docker-only failures as every prior run this session, unrelated to this change).

## Post-roadmap: change-password flow + client dashboard analytics fix (2026-08-18)

A full-platform audit (client sidebar mirroring the earlier admin sidebar audit, plus
`docs/PROJECT_PROGRESS.md`/`docs/DEPLOYMENT.md` consistency, public pages, and every other
disabled "Soon" UI element) turned up two real gaps and confirmed everything else - all 11
client sidebar pages, all 6 public pages, and both docs - was already accurate with no stale
claims.

- **No password-change UI existed anywhere on the platform, for either role.** The backend
  endpoint (`POST /auth/change-password`), the frontend API function
  (`authApi.changePassword`), and even the validation schema (`changePasswordSchema` in
  `lib/schemas/auth.ts`) all already existed, fully unused - nothing ever rendered a form that
  called them. This mattered more than a typical missing-page gap: *every* account on the
  platform is created with a forced temporary password (`AdminUserService.createAdminUser` and
  `ClientService.createClient` both set `mustChangePassword: true`), and `mustChangePassword` was
  tracked in `UserMe` but never actually enforced anywhere - so there was no way for any admin or
  client to ever rotate off their initial temporary password. Fixed with one shared component
  rather than a per-role settings page:
  - New `ChangePasswordDialog` (`features/auth/change-password-dialog.tsx`), mounted once in
    `DashboardHeader` - the header shared by both `AdminLayout` and `ClientLayout` - covering
    both roles from a single implementation. A `forced` prop (driven by
    `useAuth().user?.mustChangePassword`) hides the Cancel/X and blocks Escape/outside-click
    dismissal via a new `hideCloseButton` prop added to the shared `DialogContent` primitive;
    the dialog is open whenever `forced` is true, independent of the discoverable "Change
    password" account-menu item added alongside it for voluntary use.
  - On success: revoking every refresh token is already `AuthService.changePassword`'s own
    behavior, so the dialog logs the user out and redirects to `/login` immediately rather than
    leaving a stale session that would fail confusingly on its next silent refresh.
  - Removed the client sidebar's disabled "Settings" placeholder (`app/client/layout.tsx`) -
    its only concretely-scoped content was account password management, which now lives in the
    shared header dropdown available to both roles, so a client-only settings page would have
    been redundant. Mirrors the "Destinations" placeholder removal from the earlier admin
    sidebar audit: remove a stale disabled item once its function is covered elsewhere, rather
    than leaving a permanent "Soon" badge.
- **Client dashboard's "Profile Views" KPI card was hardcoded to `undefined`** - identical bug
  shape to the admin dashboard KPI bug fixed earlier this session. The data was already real and
  reachable (`/client/analytics` already rendered it correctly via
  `AnalyticsService.summaryForOwnClient`), just never wired into the dashboard card. Fixed by
  fetching the same 7-day summary the analytics page already uses.

Verified live end-to-end in a real browser, not just typecheck, using a fresh throwaway client
created via the admin API specifically to reach the forced (`mustChangePassword: true`) path:
logging in landed directly on a non-dismissible "Set a new password" dialog (no X button
present); completing it showed the "Password changed - please sign in again" toast and redirected
to `/login`; logging in again with the *old* password correctly failed and with the *new*
password correctly succeeded, `mustChangePassword: false` confirmed via the login response.
Separately verified the voluntary path on the real admin account: the account-menu "Change
password" item opens the same dialog with Cancel/X both present (correctly dismissible),
cancelled without submitting to leave the working admin session untouched. Also confirmed via
screenshot that the client dashboard's "Profile Views" card now shows a real live number instead
of "No data yet". Test client cleaned up afterward. `npm run typecheck`/`lint` clean.

## Post-roadmap: Settings module (2026-08-18)

Clears the last disabled sidebar placeholder from the admin-sidebar audit earlier in this doc.
Scope was already answered by the codebase itself: `frontend/src/lib/config/brand.ts` carried
its own forward-looking comment - *"Once the Settings module (Phase 9) lands, Super
Admin-configurable values here should move to the backend `settings` table instead of this
file"* - naming exactly three fields (`name`, `supportEmail`, `tagline`). `domain` was
deliberately left out of scope and stays env-var-driven (`NEXT_PUBLIC_SITE_URL`) - it's tied to
actual DNS/deployment, not something a form should be able to silently desync from where the
site is really hosted.

- New `settings` module (backend): `V14__create_platform_settings.sql` - a single fixed-schema
  row (`platform_settings`, id always `1`), not a generic key/value store, since exactly three
  concrete values are consumed anywhere in the app today and a generic store would need its own
  validation layer for no real benefit at this size. `PlatformSettingsService.get()` follows
  this session's established get-or-create idiom (falls back to seeding a default row if the
  singleton was ever deleted by hand, covered by `PlatformSettingsServiceTest`).
  `AdminSettingsController` (`GET`/`PUT /api/v1/admin/settings`, gated on the `SETTINGS_MANAGE`
  permission that was already seeded but had no controller using it) and
  `PublicSettingsController` (`GET /api/v1/public/settings`, unauthenticated) both front the
  same service.
- New `/admin/settings` page - a single settings form (name, support email, tagline), wired the
  sidebar's dormant "Settings" entry to it.
- **The part that makes this a real feature and not a form that saves into a void**:
  `SiteHeader` and `SiteFooter` (the public marketing site's header/footer, previously hardcoded
  to the static `brand.ts` constants) now fetch `/public/settings` live and render those values,
  falling back to `brand.ts` only if the fetch hasn't resolved yet or the backend is
  unreachable - the public site must never render blank while this query is in flight.
  `SiteFooter` was a plain server component with no data needs before this; converted to a
  client component to match `SiteHeader`'s existing pattern rather than inventing a different
  one. Deliberately did *not* rewire `robots.ts`, `sitemap.ts`, or the root layout's
  `generateMetadata` - those are build/request-time SEO surfaces where wiring in a live,
  admin-editable fetch is a materially bigger architectural change for a lower-value payoff
  (an admin-edited tagline propagating into cached SEO metadata isn't expected to happen
  instantly in most Next.js deployments anyway) - out of proportion to what was actually asked.
- New `PlatformSettingsServiceTest` (3 cases) matching this session's blanket policy of a test
  class per new service.

Verified live end-to-end in a real browser, not just typecheck: logged in as admin, changed the
site name on `/admin/settings`, saved, opened a *fresh* tab to the public homepage with no
rebuild/redeploy, and confirmed via `document.querySelector('footer').textContent` that both
the header logo text and the footer's copyright line reflected the new name immediately.
Reverted the test value back to `CeyloNfc` afterward and re-verified via `GET
/public/settings` that the real default was restored. `mvn test`: 242 tests, 240 passing (same
2 pre-existing Testcontainers-only failures, unrelated) - up from 239 tests/237 passing before
this entry. `npm run typecheck`/`lint` clean.

## Post-roadmap: remaining service + utility unit test coverage - every backend service now tested (2026-08-18)

Closes out the Phase 10 test-coverage push entirely - every remaining untested service
(`DestinationService`, `GoogleReviewLocationService`, `AnalyticsService`, `NotificationService`,
`PackagePlanService`, `ProductService`) plus the pure-utility classes (`UserAgentParser`,
`FileValidator`, `VCardGenerator`, `DestinationUrlValidator`) now have dedicated test classes.
Every backend service in the codebase has unit test coverage as of this entry.

- **`DestinationServiceTest`** (12 cases): the type-dispatch branching in `create` -
  `PROFILE`/`COMPANY_PROFILE` destinations are rejected when they don't match the client's own
  type (a business client can't get a `PROFILE` destination, an individual can't get
  `COMPANY_PROFILE`), `MENU` is allowed for either type, `GOOGLE_REVIEW` requires a location
  that actually belongs to the requesting client and stores its id rather than a URL,
  URL-based types (`WEBSITE` etc.) reuse `DestinationUrlValidator`, and `VCARD` - the one type
  that exists in the enum but has no module built yet - is rejected with a "not yet supported"
  message. Also fixed a stale comment on `DestinationType` claiming `PROFILE`/`COMPANY_PROFILE`/
  `GOOGLE_REVIEW`/`MENU` are "rejected until that module lands" - all four have been fully
  supported for phases now; only `VCARD` is still actually unbuilt.
- **`GoogleReviewLocationServiceTest`** (7 cases): the shared `DestinationUrlValidator` reuse
  applies to both create and update (a URL is checked twice in the code, so needed testing
  twice), and the same tenant-scoped-vs-global lookup split `MenuServiceTest` already proved
  for menus (`updateForOwnClient` via `findByUuidAndClientId`, `updateForAdmin` via the
  unscoped `findByUuid`).
- **`AnalyticsServiceTest`** (8 cases): the day-range clamping (`days <= 0` defaults to 7,
  `days > 90` clamps to 90, anything in between passes through unchanged) and the daily-series
  bucketing - every day in the requested range is pre-seeded with zero counts so a quiet day
  renders as 0 rather than a gap in the chart, and a query row for a day *outside* that
  pre-seeded range is silently dropped rather than crashing (confirmed with a row dated year
  2000 against a 7-day range).
- **`NotificationServiceTest`** (6 cases): `notifyUsers` fans out to one `notify()` call per
  recipient, and `markRead` is idempotent - calling it again on an already-read notification
  doesn't re-save or overwrite the original read timestamp (`verify(..., never()).save`).
- **`ProductServiceTest`** (5 cases) alongside a real fix: `update` previously reused `apply()`
  for every field except SKU uniqueness, which `create` does check - so renaming a product's
  SKU to collide with another product's would have hit the database's raw unique-constraint
  error instead of a clean `ConflictException`. Added `existsBySkuAndIdNot` and the check in
  `update`, with a test for both the collision case and that a product keeping its own
  unchanged SKU is unaffected.
- **`PackagePlanServiceTest`** (5 cases): create defaults `active` to `true` and
  `premiumTemplates` to `false` when omitted; update's `sortOrder` is only overwritten when
  explicitly supplied, same "null means don't touch this field" pattern `LeadServiceTest`
  already covered for lead notes.
- **`UserAgentParserTest`** (9 cases) - **found and fixed a real, live production bug**: a real
  iPhone/iPad Safari User-Agent string always includes `"like Mac OS X"` (Apple's own
  compatibility convention, e.g. `"CPU iPhone OS 17_0 like Mac OS X"`), and the parser's OS
  checks tested `windows` → `macos` → `android` → `ios` in that order - meaning `"mac os"`
  matched and returned before the code ever reached its `iphone`/`ipad`/`ios` check. Every real
  iPhone and iPad tap has been recorded as `macos` in the analytics OS breakdown rather than
  `ios` since this code was written, for every client on the platform, silently. This is
  exactly the kind of bug a written-from-imagination test would launder past (it "looks"
  correct - iOS-detection code exists, right below the failing check) but a test built from
  copying a real, unmodified UA string off a real device caught immediately. Fixed by
  reordering the checks so iOS-specific tokens are tested before the macOS token.
- **`FileValidatorTest`** (10 cases) - the magic-byte upload validator from
  `docs/SECURITY.md`'s "File uploads" section, exercised against real byte signatures rather
  than mocks: rejects empty/oversized files, rejects SVG by extension or content-type outright,
  accepts real JPEG/PNG/GIF/WEBP signatures, rejects bytes matching no known signature, and -
  the actual security property this validator exists for - rejects a file whose real magic
  bytes don't match its declared content-type (a PNG's bytes declared as
  `application/octet-stream`, a JPEG's bytes declared as `image/png`).
- **`VCardGeneratorTest`** (5 cases): individual-with-company gets both `FN` and `ORG` lines, a
  business profile's own name never gets duplicated into `ORG`, optional fields are omitted
  entirely (not emitted blank) when unset, and vCard special-character escaping
  (`,`/`;`/`\`) is verified against the spec rather than assumed from reading the escape
  function.
- **`DestinationUrlValidatorTest`** (8 cases): null/blank rejected, non-http(s) schemes
  (`javascript:`, `ftp:`) rejected with the specific "must use http or https" message, a
  `data:` URL rejected via a different code path (illegal characters trip Java's own URI
  parser before the scheme check ever runs - still correctly rejected, just via a different
  message, worth documenting so a future reader isn't confused by the different exception
  text), a URL with no host rejected, and valid http/https URLs (with a port, a query string,
  a subdomain) all accepted.

`mvn test`: 239 tests, 237 passing (same 2 pre-existing Testcontainers-only failures, unrelated)
- up from 162 tests/160 passing before this entry. Backend restarted and confirmed booting
cleanly with every change in this entry (the `ProductRepository` addition, the `UserAgentParser`
fix, the `DestinationType` comment correction).

## Post-roadmap: QrCodeService unit test coverage (2026-08-18)

Continues the Phase 10 test-coverage push - `QrCodeService.resolveRedirectTarget` is the QR
scan hot path, deliberately written to mirror `NfcCardService.resolveRedirectTarget` (its own
javadoc says so), so it gets the same rejection-chain coverage `NfcCardServiceTest` already
established for physical cards: malformed token rejected before the database, unknown token
hash, a suspended QR code, a soft-deleted client, a suspended client, and a QR code with no
resolvable destination - then the happy path (scan count incremented, `lastScannedAt` set,
`QR_SCAN` analytics event recorded with the right client/qr/destination ids). Also covers
`setStatus`'s two lookup paths (tenant-scoped `findByUuidAndClientId` for a client managing
their own code vs. global `findByUuid` for admin) and both directions of the active/suspended
toggle, plus `create` - confirmed a destination is always created fresh via
`DestinationService.create` before the QR row is saved, rather than ever pointing at an
existing one (every QR code owns exactly one destination, 1:1).

New `QrCodeServiceTest` (12 cases, pure Mockito, no Spring context/DB). `mvn test`: 162 tests,
160 passing (same 2 pre-existing Testcontainers-only failures, unrelated) - up from 151 tests/
149 passing before this entry.

## Post-roadmap: AuthService unit test coverage (2026-08-18)

The single highest-value remaining gap in the Phase 10 test-coverage push: the authentication
core itself - account lockout, refresh-token rotation with reuse detection, and the password
reset/change flows - had zero unit tests despite being the most security-critical code in the
platform. (`AuthFlowIntegrationTest` exists but is Testcontainers-based and doesn't run in this
sandbox - see "Known Issues" - so this was genuinely uncovered here, not just uncovered by a
faster test tier.)

New `AuthServiceTest` (18 cases, pure Mockito, no Spring context/DB):

- **Login**: an unknown email and a wrong password both throw the identical
  `InvalidCredentialsException` message ("Invalid email or password") - confirmed as one test,
  not two, specifically because the whole point of that shared message is that a caller can't
  distinguish "no such account" from "wrong password" from the response alone. A locked account
  is rejected *before* the password is even checked (`verify(passwordEncoder, never()).matches`)
  - locked-account timing shouldn't leak whether the password would have been right. A
  non-`ACTIVE` account gets the same generic message rather than a distinct "disabled" error,
  for the same anti-enumeration reason. Failed attempts increment without locking below the
  configured threshold, then lock exactly at it (`AppProperties.Security.maxFailedLoginAttempts`,
  default 5). A successful login resets the counter and clears any lock.
- **Refresh** (the highest-risk logic in the whole service): reusing an already-revoked refresh
  token - the signal of a stolen/replayed token - revokes *every* active session for that user,
  not just the one presented, confirmed by asserting a second, unrelated session's `revokedAt`
  got set too. Separately confirmed that a token that's merely *expired* (never revoked) does
  **not** trigger that same full-session-revocation path - conflating "expired" with "reused"
  would force a full re-login storm on every ordinary session timeout instead of only on actual
  token theft, so keeping these as two distinct code paths (and two distinct tests) matters. A
  successful refresh rotates the token (old one revoked, linked via `replacedByTokenHash`)
  rather than being reusable.
- **Password reset/change**: `forgotPassword` is a true no-op for an unknown email - no token
  row created, no email sent - the anti-enumeration behavior the code comment already states,
  now actually verified rather than just asserted in a comment. Both `resetPassword` and
  `changePassword` revoke every active session on success, so a password compromise (or a
  legitimate password change) can't leave a stolen session still valid.

`mvn test`: 151 tests, 149 passing (same 2 pre-existing Testcontainers-only failures,
unrelated) - up from 134 tests/132 passing before this entry.

## Post-roadmap: LeadService unit test coverage (2026-08-18)

Closes out the last significant untested service from this session's Phase 10 test-coverage
push - lead-to-client conversion, the backend guard behind the "Post-roadmap: Convert to
client" feature earlier in this doc (whose frontend half had its own bug found and fixed live:
`ConvertLeadDialog` hiding its own just-converted result screen).

New `LeadServiceTest` (10 cases, pure Mockito, no Spring context/DB): `update` rejects an
unknown lead uuid and an unrecognized status, and - the one field with non-obvious semantics in
this service - `notes` is only overwritten when the request actually supplies a value
(`if (request.notes() != null)`), confirmed both ways: omitting `notes` on a status-only update
preserves the existing note rather than blanking it, supplying one overwrites it.
`convertToClient` rejects an unknown lead and a lead that's already `CONVERTED`
(`verify(clientService, never()).createClient(...)` confirms the guard short-circuits before
ever attempting a second client creation, not just that it throws); the display name passed to
`ClientService.createClient` prefers the lead's company name and falls back to the lead's
personal name only when no company was captured; and - the case most worth a regression test
for a method that does two side-effecting things in sequence - the lead is marked `CONVERTED`
*only* after `ClientService.createClient` actually succeeds, confirmed by making that call
throw (e.g. the lead's email already belongs to a client) and asserting the lead's status is
untouched and `leadRepository.save` was never even called, not just that the exception
propagates.

`mvn test`: 134 tests, 132 passing (same 2 pre-existing Testcontainers-only failures,
unrelated) - up from 124 tests/122 passing before this entry.

## Post-roadmap: ClientService unit test coverage (2026-08-18)

Continues the Phase 10 remainder's test-coverage gap - `ClientService` is the tenant root every
other client-scoped feature this session depends on, and its soft-delete cascade (owner
disabled + sessions revoked, from the earlier "client delete" post-roadmap entry) had never
been exercised by anything but manual browser verification.

New `ClientServiceTest` (11 cases, pure Mockito, no Spring context/DB): `createClient` rejects
an email already used by *either* a user account or an active client (two separate uniqueness
checks against two different tables, both needed - a user could exist without a client, or vice
versa in edge cases) and an unrecognized client type; the happy path links the new user as
`ownerUserId` and forces a password change. `updateClient` skips its own conflict check
entirely when the email is unchanged (case-insensitively) - the case that would otherwise
false-positive a client against its own existing row - and rejects a genuine collision with
another active client. `deleteClient`'s cascade is tested as three separate assertions rather
than one: the client itself is soft-deleted (`deletedAt` set, status `CLOSED`, never actually
removed), the owner account is disabled, and the owner's active sessions are revoked - plus
confirming an already-deleted client can't be "deleted" again (`findActiveOrThrow` is shared by
every mutating method, so one test here stands in for all of them).

`mvn test`: 124 tests, 122 passing (same 2 pre-existing Testcontainers-only failures,
unrelated) - up from 113 tests/111 passing before this entry.

## Post-roadmap: AdminUserService unit test coverage (2026-08-18)

Continues the Phase 10 remainder's test-coverage gap, picking the highest-stakes untested
service left: platform staff (ADMIN/SUPER_ADMIN) account management and per-admin permission
overrides - a bug here is a privilege-escalation or lockout risk, not just a broken feature.

- New `AdminUserServiceTest` (13 cases, pure Mockito, no Spring context/DB) covers
  `createAdminUser`'s validation (duplicate email, an unrecognized role string, a real role that
  just isn't a staff role like `CLIENT`) and its happy path (`mustChangePassword` forced `true`,
  account-created email sent); `updateStatus`'s self-suspend guard - an admin can reactivate
  their own account but not suspend/disable it, confirmed via both directions rather than just
  the blocked one - plus the session-revocation side effect, confirmed to fire on
  suspend/disable and confirmed *not* to fire on reactivation (a bug that revoked sessions on
  every status change, not just suspensions, would silently log out a just-reactivated admin);
  and the SUPER_ADMIN/permission-override rules from `docs/ROLE_PERMISSION_MATRIX.md` - a
  SUPER_ADMIN shows every permission as granted regardless of the (irrelevant) override table,
  a regular admin only shows the ones actually granted, overrides can't be set on a SUPER_ADMIN
  at all, an unknown permission code is rejected, and updating overrides replaces the full set
  (delete-all then re-save) rather than diffing - confirmed exactly one `save` call for one
  granted permission out of three candidates, not three.

`mvn test`: 113 tests, 111 passing (same 2 pre-existing Testcontainers-only failures,
unrelated) - up from 100 tests/98 passing before this entry.

## Post-roadmap: MenuService unit test coverage (2026-08-18)

Follow-up to the admin sidebar audit entry directly below - that entry refactored every
mutating method on `MenuService` into a private `Client`-based core plus two public entry
points (own client / admin-on-behalf-of-any-client) to add the new `AdminMenuController`, but
shipped with zero tests proving the refactor didn't silently change behavior for either path.
Matches this session's established rule: a same-day refactor of this shape gets its own test
class before moving on, same as `TemplateServiceTest`/`ProfileServiceTest` did for their
originating features.

New `MenuServiceTest` (16 cases, pure Mockito, no Spring context/DB): `getOrCreateOwnMenu`
slugifies the client's display name on first creation; `getOrCreateMenuForClient` resolves by
uuid rather than the authenticated user and never touches
`findByOwnerUserIdAndDeletedAtIsNull` (a wrong resolver here would let an admin action operate
on whichever client happens to be the *caller's own*, not the one requested - confirmed it
doesn't); `updateOwnMenu` rejects a slug already taken by another client; `updateMenuForClient`
saves through the admin path with the same validation; `setPublished`/`setPublishedForClient`
both toggle correctly; category/item create-default-sort-order, not-found, and delete are all
covered through the admin path specifically (the newer, less-exercised half of the duality);
`hasPublishedMenu`/`resolvePublicUrl` treat "no menu row" and "menu row but unpublished" the
same as `ProfileServiceTest` already established for profiles; `recordMenuView` is a true no-op
for an unknown slug.

`mvn test`: 100 tests, 98 passing (same 2 pre-existing Testcontainers-only failures, unrelated) -
up from 84 tests/82 passing before this entry.

## Post-roadmap: admin sidebar audit - dashboard KPIs, Menu management, Destinations cleanup (2026-08-18)

The user asked to verify every admin sidebar item was actually finished and correctly wired,
having doubts about it. Checked all 20 items live in a real browser rather than just reading
the nav config: all 17 linked pages render correctly with real data (confirmed via
`get_page_text` on each, not just that they don't 404). The 3 disabled "Soon" placeholders each
turned out to be a different situation, investigated individually rather than assumed:

- **Destinations**: not actually missing. `AdminDestinationController` has working list/create
  endpoints, but destinations are always created *inline* as part of the QR-code-creation and
  NFC-card-assignment flows (both call destination creation as a step before their own
  create/assign call) - never as a standalone object a user browses. A dedicated "browse all
  destinations" page would be redundant with that, not a missing feature. Removed the stale
  placeholder from both the admin and client sidebars (`admin/layout.tsx`, `client/layout.tsx`)
  rather than building a page nobody needs.
- **Menus**: a real gap - client self-service already existed (`ClientMenuController`) but there
  was no admin equivalent, unlike Profile/QR/Google Reviews which all support "admin edits on a
  client's behalf." Built one, following the exact pattern `AdminProfileController` already
  established:
  - `MenuService` refactored so every one of its 9 mutating methods now has a private
    `Client`-based core plus two public entry points - `xOwnY(actor, ...)` for the client's own
    menu and a new `xYForClient(clientUuid, ...)` for admin - rather than duplicating the logic.
    `ProfileService.getOrCreateIndividual`/`getOrCreateCompany` (made `public` in the template
    system entry above) is the template this followed.
  - New `AdminMenuController` (`/api/v1/admin/menu`, gated on `MENU_MANAGE` - already seeded as
    a permission with "own menus only" scope noted for clients and full manage for admin in
    `docs/ROLE_PERMISSION_MATRIX.md`, just never had a controller before now) mirrors
    `AdminProfileController`'s `@RequestParam clientUuid` shape exactly.
  - `MediaUploadController`'s `@PreAuthorize` broadened for `MENU_MANAGE`, same precedent as the
    `TEMPLATE_MANAGE` addition - an admin uploading a menu item photo on a client's behalf goes
    through the same shared upload endpoint.
  - Frontend: `menu-editor-content.tsx`, `add-category-dialog.tsx`, and `menu-item-dialog.tsx`
    all gained an optional `clientUuid` prop that switches between `menuApi`/`adminMenuApi` and
    the `["client","menu"]`/`["admin","menu",clientUuid]` query key - the exact idiom
    `SocialLinksEditor` already used for the same own-vs-admin split. New
    `AdminMenuPageContent` + `/admin/menu` route mirror `AdminProfilePageContent` +
    `/admin/profile` (`ClientPicker`-gated, empty state until a client is chosen). Wired the
    sidebar's dormant "Menus" entry (`href` was missing) to it.
- **Settings**: confirmed genuinely 0% built - only the `SETTINGS_MANAGE` permission constant
  exists, nothing else. Left as the disabled placeholder since what should live on it hasn't
  been defined yet; asked the user to scope it before building anything speculative.

**Also found and fixed while doing this audit, not part of the original ask**: the admin
dashboard's "QR Codes" and "Google Review Locations" KPI cards were permanently hardcoded to
`undefined` (`values.qr`/`values.reviews` never fetched anything), showing a fake "No data yet"
regardless of how much real data existed - and the "Getting started" card's copy still claimed
"QR codes and Google Review locations land in the next phases of the build," which has been
false since both shipped in earlier phases. Neither had a platform-wide count endpoint (the
existing admin QR/review-location list endpoints are both `listForClient(clientUuid)` only, no
"all clients" variant), so added `QrCodeService.countAll()`/`GoogleReviewLocationService.countAll()`
(both just `repository.count()` - already free via `JpaRepository`) plus a `GET .../count`
endpoint on each admin controller, wired into `admin-dashboard-content.tsx`. Verified live: the
dashboard now shows real counts (10 QR codes, 2 review locations at time of writing) instead of
permanent placeholders.

Verified live in the browser end-to-end, not just typecheck: selected a client on the new
`/admin/menu` page, added a category and an item through the actual dialogs, confirmed both
persisted via the new admin endpoints; separately logged in as a real client and confirmed
`/client/menu` (untouched code path, `clientUuid` undefined) still works identically to before
the refactor - a genuine regression check, not an assumption. `mvn test`: 84 tests, 82 passing
(same 2 pre-existing Testcontainers-only failures, unchanged), `npm run typecheck`/`lint` clean.

## Post-roadmap: NfcCardService unit test coverage (2026-08-18)

Continues the Phase 10 remainder's test-coverage gap, targeting the single highest-stakes
untested method in the codebase: `resolveRedirectTarget`, the actual redirect hot path every
physical card tap runs through. A bug here doesn't just break a feature - it's either a broken
product (a tap that should redirect doesn't) or a security gap (a tap that shouldn't redirect
does), so it was the natural next pick over the several other still-untested services.

- New `NfcCardServiceTest` (16 cases, pure Mockito, no Spring context/DB) covers
  `resolveRedirectTarget`'s full rejection chain in order - malformed token format (rejected
  before ever touching the database, confirmed via `verify(..., never())`), unknown token hash,
  non-`ACTIVE` card status, a soft-deleted client, a `SUSPENDED` client, and a card whose
  destination no longer resolves - plus the happy path: tap count incremented, `lastTappedAt`
  set, and the analytics event recorded with the exact client/card/destination ids. Also covers
  `assign`'s card-limit gating (`SubscriptionService.assertCanAssignCard` is called for a
  genuinely new client assignment and for reassigning to a *different* client, but explicitly
  skipped when re-assigning a destination for the *same* client the card already belongs to -
  that distinction lives in `isNewAssignmentForClient` and had no test proving it actually
  works); `replace`'s validation (no prior assignment, duplicate new serial number) and its
  documented deliberate omission of the same limit check (a like-for-like swap isn't a new
  assignment against the plan); and `setStatus`'s activation guard (can't activate a card with
  no client/destination assigned).

`mvn test`: 84 tests, 82 passing (same 2 pre-existing Testcontainers-only failures, unrelated) -
up from 68 tests/66 passing before this entry.

## Post-roadmap: ProfileService unit test coverage (2026-08-18)

Continues chipping away at the Phase 10 remainder's "most services still have no dedicated unit
tests" gap - `ProfileService` was the most-used untested service in the codebase (backs both
profile editing and every public profile page render) and had the richest untested branching
logic: slug-collision suffixing, lazy get-or-create idempotency, and the template-resolution
code path added in the "Post-roadmap: template system" entry below.

- New `ProfileServiceTest` (16 cases, pure Mockito, no Spring context/DB): `getOrCreateIndividual`
  slugifies a client's display name on first creation, appends `-2`/`-3`/... suffixes on
  collision, and - the case that matters for `TemplateService.applyToOwnProfile`'s lazy-creation
  fix from the template-system entry - returns the existing profile untouched (no slug
  generation, no save) when one already exists; `updateProfile` rejects a blank name (full name
  for individuals, company name for businesses) and a slug already taken by a different client;
  `hasPublishedProfile`/`resolvePublicUrl` correctly treat "no profile row" and "profile row but
  unpublished" as the same not-available outcome; the two `getPublic*Profile` methods include a
  template's color/layout in the response only when one is actually applied, confirmed by
  verifying `templateRepository.findById` is never even called otherwise; the view-recording
  methods are a true no-op (verified via `verifyNoInteractions`) for an unknown slug, since that
  method fires on every public page load and must never surface an error to the visitor.
- One test bug caught and fixed while writing these, not a product bug: an early draft of the
  slug-conflict test used a bare `new IndividualProfile()` fixture with no `clientId` set, so
  `ensureSlugAvailable`'s `existsBySlugAndClientIdNot(slug, profile.getClientId())` call was
  checking against `null` instead of the stubbed client id and silently passed instead of
  throwing - caught immediately by actually running the test (it failed) rather than assuming a
  reasonable-looking fixture was correct.

`mvn test`: 68 tests, 66 passing (same 2 pre-existing Testcontainers-only failures, unrelated) -
up from 52 tests/50 passing before this entry.

## Post-roadmap: Playwright journeys - order creation, package/product CRUD, QR code creation (2026-08-18)

Clears the three named Playwright journeys from the Phase 10 remainder (5 specs/7 tests already
existed: admin login success/failure, client creation, NFC card ownership isolation, a full
support-ticket round trip, and the public pricing/lead form - these three were the ones still
missing). All three follow the same self-contained pattern the existing specs already
established (`admin-create-client.spec.ts`): create their own fresh data with a
`Date.now()`-based unique name rather than depending on pre-seeded dev-database state, so they
don't go stale or collide with each other across runs.

- New `e2e/helpers.ts` - a tiny `loginAsAdmin(page)` shared by all three new specs (the first
  real duplication across specs; the existing 5 didn't share enough setup to justify one).
- `admin-package-product-crud.spec.ts`: create a package plan, create a product, both verified
  to actually appear in their respective admin list tables afterward (not just a success toast).
- `admin-qr-code-creation.spec.ts`: create a client, select them via the QR page's client
  picker, create a QR code against it, verify the created-QR confirmation screen and that the
  code appears in that client's list.
- `admin-order-creation.spec.ts`: create a product and a client independently, then create an
  order tying them together from the Orders page's client+product selects, verifying the order
  appears in the table under the right client name.

**A real, non-obvious Playwright gotcha hit while writing these, not a product bug**: every
`Select`'s trigger in this design system (`ClientPicker`, the order dialog's client/product
selects) renders as `role="combobox"` with its placeholder text as a plain child `<span>`, not
via `aria-label`/`aria-labelledby` - so `getByRole("combobox", { name: "Select a client" })`
timed out finding zero matches, because Chromium's accessible-name computation doesn't count
that child text toward the combobox's own name the way visible-text intuition suggests. Fixed
by matching on visible text instead of accessible name: `getByRole("combobox").filter({
hasText: "Select a client" })`. Documented here since every future spec touching one of this
codebase's `Select` components will hit the exact same trap.

Verified live: all three new specs pass individually and the full suite (11 tests across 8
spec files, up from 5 specs/7 tests) passes together in one run with no cross-test
interference, run against the real local dev stack (`localhost:8080`/`:3000`), not mocked.

## Post-roadmap: template + subscription unit test coverage (2026-08-18)

Small follow-up to the template system entry directly below - that entry shipped the feature
verified live (real browser screenshots, real subscription upgrades) but with zero unit tests,
which is exactly the Phase 10 remainder gap ("most services still have no dedicated unit
tests"). Added the two test classes the new code actually needed:

- New `TemplateServiceTest` (8 cases, pure Mockito, no Spring context/DB): free templates never
  even call the subscription check; a premium template is rejected server-side when the client
  lacks access and succeeds when they have it; a deactivated template is rejected regardless of
  premium status; an unknown template uuid throws `ResourceNotFoundException`; the gallery's
  `locked` flag matches subscription access and its `selected` flag matches the profile's actual
  `template_id`. One test is a direct regression test for the lazy-profile-creation bug found
  during live verification (applying to a client with zero prior profile activity must still
  succeed by going through `ProfileService.getOrCreateIndividual`, not throw).
- Extended the existing `SubscriptionServiceTest` with 4 cases for the new
  `clientHasPremiumTemplateAccess` method, mirroring the exact structure already used for
  `assertCanAssignCard`'s status/plan-limit tests: no subscription grants access (the documented
  "unmanaged = unrestricted" precedent), an unusable subscription status denies it regardless of
  plan, and - the one place this method's semantics deliberately diverge from
  `assertCanAssignCard` - a dangling `package_plan_id` denies access rather than granting it
  (card limits treat "no plan found" as "no limit", but premium access is opt-in: no plan found
  means no `premiumTemplates: true` was ever confirmed, so it must deny, not default-allow).

`mvn test`: 52 tests, 50 passing (same 2 pre-existing Testcontainers-only failures every other
entry this session has hit, unrelated to this change) - up from 37 tests/35 passing before this
entry.

## Post-roadmap: template system (2026-08-18)

Clears the Phase 3 remainder - a `template` module, `template_id` on profiles, and a
premium/free flag were always the plan (`docs/ARCHITECTURE.md`'s module list and
`docs/DATABASE.md`'s Phase-1 planning notes both already named it; `V6__create_profile_tables.sql`
even left a comment explaining why `template_id` was deliberately omitted at the time - "the
Template module doesn't exist yet"). `PackagePlan.premiumTemplates` and the `TEMPLATE_MANAGE`
permission were already seeded/wired from earlier phases as forward-looking stubs, unused until
now.

- New `template` module: `Template` entity (`name`, `description`, `previewImage`,
  `primaryColor` hex, `layout` enum `CLASSIC`/`MINIMAL`, `premium`, `active`, `sortOrder`),
  `V13__create_templates_table.sql` adds the table plus a nullable `template_id` FK
  (`ON DELETE SET NULL`) on both `individual_profiles` and `company_profiles`. Admin authors
  the gallery (`AdminTemplateController`: list/create/update, no delete - deactivate instead,
  matching `package_plans`'s existing `active`-flag convention rather than inventing a new one).
  Clients select/apply from the gallery to their own profile only
  (`ClientTemplateController` + a new endpoint on `ClientProfileController`) - exactly
  `docs/ROLE_PERMISSION_MATRIX.md`'s pre-existing "TEMPLATE_MANAGE: select/apply only" scope
  note for the Client role, enforced the same ownership-scoped way as every other client
  endpoint (no platform permission check for clients, just `hasRole('CLIENT')` + `client_id`
  ownership in the service layer).
- Premium gating reuses the exact "no subscription record = unmanaged/unlimited" precedent
  `SubscriptionService.assertCanAssignCard` already established for card limits - a client the
  admin hasn't put on a plan yet isn't enforced against any plan's capabilities, premium
  templates included. New `SubscriptionService.clientHasPremiumTemplateAccess(clientId)`
  backs both the gallery's per-item `locked` flag (client sees premium templates, badged, not
  hidden) and a real backend gate in `TemplateService.applyToOwnProfile` - verified live that
  `POST /client/profile/template/{uuid}` on a locked premium template is rejected server-side
  (`VALIDATION_ERROR`, "upgrade your plan to unlock it"), not just hidden by the frontend.
- Public profile theming is genuinely live, not just stored: `--primary` is already a plain hex
  CSS custom property every `bg-primary`/`text-primary` Tailwind utility in
  `PublicProfileCard` reads from (see `globals.css`), so overriding it via an inline `style` on
  the card's root element re-themes the whole card - Call/Save buttons, cover gradient, avatar
  ring - from one template color, with zero utility classes touched. `MINIMAL` layout swaps the
  gradient cover banner for a flat one and squares off the avatar; `CLASSIC` (the default,
  `template_id IS NULL`) renders byte-identical to the pre-template card, so every existing
  published profile is unaffected until a client actively picks a template.
- `TEMPLATE_PREVIEW` added to the shared `MediaCategory` enum and `MediaUploadController`'s
  `@PreAuthorize` broadened for `TEMPLATE_MANAGE`, reusing the one upload endpoint/service
  every other image type already goes through rather than a bespoke path (same pattern the
  support-attachment work established).

**A real bug found and fixed while verifying live, not a hypothetical**: the first attempt to
apply a template to a brand-new test client (admin-created, never once loaded their own
`/client/profile` page) failed with `RESOURCE_NOT_FOUND` - `TemplateService.applyToOwnProfile`
looked up the profile row directly and threw if missing, but profile rows are created lazily on
first `GET /client/profile` (`ProfileService.getOrCreateIndividual`/`getOrCreateCompany`), so a
client who goes straight to the new Templates page before ever touching their profile page has
no row yet. Fixed by exposing `ProfileService`'s existing lazy get-or-create methods (now
`public`, were `private`) and having `TemplateService` call those instead of a throwing lookup -
reuses the established pattern rather than duplicating profile-creation logic in a second
service. Re-verified: applying a template as a fresh client with zero prior profile activity now
creates the profile and applies the template in one call.

Verified live end-to-end (not just typecheck/build): created two real templates via
`AdminTemplateController` (one free/`CLASSIC`, one premium/`MINIMAL`); confirmed
`GET /client/templates` correctly showed the premium one as *unlocked* while the test client had
no subscription yet (the "no subscription = unrestricted" precedent above), then assigned the
client a real subscription to a plan with `premiumTemplates: false` and confirmed the gallery
flipped to `locked: true` and the apply endpoint rejected it server-side; upgraded the
subscription to a plan with `premiumTemplates: true` and confirmed both the gallery unlocked and
the apply succeeded.
Screenshotted the actual public profile page in a real browser before/after applying each
template - the cover banner, avatar shape, and every accent color visibly changed per template,
confirming the CSS custom-property theming approach actually works end-to-end rather than just
compiling. Exercised the full admin UI too: `/admin/templates` list, create dialog, edit dialog
with pre-filled values; `/client/templates` gallery grid with live "Selected"/"Upgrade to
unlock" states; the profile editor's new "Change template" button. `mvn test` (37 tests, 35
passing - same 2 pre-existing Testcontainers-only failures as every other entry this session,
unrelated to this change), `npm run typecheck`/`lint`/`build` all clean, including the two new
`/admin/templates` and `/client/templates` routes in the production build's route list.
`docs/API.md` updated with the new admin templates endpoints; `docs/DATABASE.md`'s stale
"not yet migrated" module list corrected to drop `templates`.

## Post-roadmap: S3-compatible storage (2026-08-17)

Clears the other half of the Phase 10 remainder item the Redis rate limiter entry below already
called out as "required before running more than one backend instance" -
`LocalStorageService` writes uploads to the container's own filesystem, which doesn't survive a
redeploy or replicate across instances the moment there's more than one. `StorageService` was
already an interface with exactly one implementation, so this shipped as a second
implementation plus a config switch, not a rewrite of any upload call site.

- New `S3StorageService` (`software.amazon.awssdk:s3`, pulled in via the SDK's BOM in `pom.xml`)
  implements the same `StorageService.store(file, category)` contract `MediaUploadController`
  already calls - `LocalStorageService` and `S3StorageService` are both `@Service` beans gated
  by `@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local"/"s3")`
  on the new `app.storage.type` property (`STORAGE_TYPE` env var, defaults to `local` via
  `matchIfMissing = true` so existing dev setups need no config change). `MediaResourceConfig`
  (the `/media/**` static resource handler that serves local uploads back out) is gated the same
  way, so it - and the local upload directory it eagerly creates - doesn't activate at all under
  `s3` mode.
- Works against real AWS S3 (leave `STORAGE_S3_ENDPOINT` unset; leave the access/secret key env
  vars unset too, to use the AWS SDK's default credential chain / IAM role rather than a static
  key checked into an env var) or any S3-compatible provider - MinIO, Cloudflare R2,
  DigitalOcean Spaces - via `STORAGE_S3_ENDPOINT` + `forcePathStyle(true)` (path-style is what
  every non-AWS provider expects; virtual-hosted-style only reliably resolves against real
  `*.amazonaws.com` DNS). `STORAGE_S3_PUBLIC_URL` overrides the returned URL's host for a CDN
  sitting in front of the bucket.

**Found and fixed a real, pre-existing bug while verifying live, unrelated to S3 itself**: a
request for a nonexistent `/media/**` path returned HTTP 500 instead of 404. Root cause:
`GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)` was catching Spring's
own `NoResourceFoundException` (thrown for any unmapped static resource, including a missing
file under a registered handler) and mapping every exception type it didn't recognize to 500 -
this predates the S3 work and affects `local` mode identically, it was just never noticed
because a missing file under `local` mode's real upload directory basically never happens in
practice, whereas testing S3 mode with `/media/**` now correctly serving nothing (the resource
handler bean doesn't even exist in that mode) hit the same path deliberately. Fixed by adding a
dedicated `@ExceptionHandler(NoResourceFoundException.class)` returning a proper 404 with a
`NOT_FOUND` error code, ahead of the generic catch-all. Verified both modes after the fix:
`local` mode's `/media/**` still serves real uploaded files and now 404s (not 500s) for missing
ones; `s3` mode 404s the same way.

Verified live end-to-end against a real MinIO container (not mocked): started a pinned
`minio/minio` image, created a bucket via `mc`, ran the backend with `STORAGE_TYPE=s3` pointed
at it, logged in as the seeded Super Admin, uploaded a real PNG through
`POST /api/v1/client/media/upload`, confirmed the response URL pointed at the MinIO bucket (not
a stale local path), and confirmed that URL was independently fetchable (HTTP 200) directly from
MinIO - a genuine externally-stored, redeploy-surviving file, not just "the endpoint returned
200." Backend restarted back to `local` mode and the MinIO container removed afterward so the
running dev environment matches the documented default. `mvn test` (37 tests, 35 passing - same
2 pre-existing Testcontainers-only failures as the Redis entry below, unrelated to this change).
`backend/.env.example` and `docs/DEPLOYMENT.md` (env var list + Go-live checklist) updated with
the six new `STORAGE_S3_*` variables.

## Post-roadmap: Redis-backed rate limiter (2026-08-17)

Clears one item of the Phase 10 remainder - explicitly called out as "required before running
more than one backend instance" since the previous implementation kept its Bucket4j buckets in
a plain `ConcurrentHashMap`, correct for a single instance but silently useless as a shared
limit the moment there's more than one (each instance would enforce the cap independently,
letting real traffic through at N times the intended limit). `spring-boot-starter-data-redis`
and `bucket4j-core` were already both dependencies and `spring.data.redis.host/port` already
pointed at the same Redis docker-compose already runs - nothing was consuming either, so this
shipped as a rewrite of the one `RateLimiterService` all 8 rate-limited endpoints already share
(login, password reset, public lead form, ticket creation, NFC/QR redirects, profile/menu view
beacons), not new call sites.

- `RateLimiterService` now does a plain Redis `INCR`+`EXPIRE` fixed-window counter as the
  primary path - not a port of Bucket4j's smoother token-bucket algorithm to Redis, which needs
  a dedicated `ProxyManager` per Redis client library. A fixed window can admit roughly 2x
  capacity right across a window boundary; acceptable for blunting brute-force/enumeration
  attempts, not billing-grade, and a straightforward well-known two-command pattern beats that
  extra dependency surface for what this actually needs to do.
  Falls back to the original in-memory Bucket4j implementation whenever the Redis call itself
  throws, exactly matching `docs/SECURITY.md`'s already-documented "Redis-backed when
  available, in-memory fallback in dev" plan - the interface every call site already used
  (`tryConsume(key, capacity, period)`) didn't change, so none of the 8 endpoints needed
  touching.
- New `RateLimiterServiceTest` (6 cases, pure Mockito): Redis-path allow/deny, expiry set only
  on the first increment of a window, independent keys don't cross-contaminate, and - the part
  that actually matters for a fallback - that the in-memory path still enforces the same
  capacity limit rather than just "always allow" once Redis is unreachable.

**A real, serious bug found and fixed while verifying against the real Redis container, not a
hypothetical**: the first live fallback test (stop the Redis container, hit a rate-limited
endpoint) returned the correct result but took **60.7 seconds** - Lettuce's default Redis
command timeout, hit on every single request while Redis stays down. For the NFC/QR tap
redirect hot path specifically, that's indistinguishable from the whole public site being down
during a Redis outage - worse than having no fallback, since a fallback whose whole purpose is
resilience shouldn't itself become the outage. Root-caused to `spring.data.redis.timeout` never
being set, so Lettuce used its own 60s default rather than anything tuned for a local,
low-latency dependency. Fixed by setting `spring.data.redis.timeout` and `connect-timeout` to
500ms in `application.yml`. Re-verified the identical stop-Redis-and-hit-the-endpoint test
afterward: same correct 200 response, **0.6 seconds** - confirmed via the backend log that it
was genuinely the fallback path firing (the "Redis call failed" warning), not a coincidence.
This is exactly the kind of bug that mocked unit tests structurally cannot catch (they patch
the failure mode itself; they can't discover that the real failure mode is "slow", not just
"exceptions") - the general lesson being that a fallback path's *latency* under real failure is
part of its correctness, not a separate concern from whether it eventually returns the right
answer.

Verified live end-to-end against the real Redis container (not mocked): fired 6 requests at the
5/minute public-leads limiter and confirmed the 6th got HTTP 429; confirmed the actual Redis key
(`ratelimit:public-leads:<ip>`) existed with the correct value and a 60-second TTL: stopped
Redis and confirmed the fallback both fired (log message) and returned the correct result fast
(after the timeout fix); restarted Redis and confirmed the primary path resumed writing to
Redis immediately, with no restart or manual intervention needed on the app side. `mvn test`
(37 tests, 35 passing - same 2 pre-existing Testcontainers-only failures, +6 new passing),
`npm run typecheck` clean (backend-only change, frontend untouched).

## Post-roadmap: support ticket file attachments (2026-08-17)

Clears the last Phase 9 remainder. Scoped to image attachments only (screenshots cover the
overwhelming majority of real support-ticket attachments, e.g. "cards not tapping" or "menu
item broken"), reusing the existing image upload pipeline end-to-end rather than adding a new
one: `MediaUploadController`/`StorageService`/`FileValidator` (MIME + magic-byte checked, 5MB
cap, SVGs rejected) already handled every other image in this app - a new `TICKET_ATTACHMENT`
`MediaCategory` (folder `tickets`) was the only addition needed there, no new validation
surface. `MessageCreateRequest`/`TicketCreateRequest` gained an optional `attachmentUrl` field
(V12 migration adds the matching column to `support_messages`) carrying the already-uploaded
file's URL, same pattern as every other image reference in this schema.
- Broadened `MediaUploadController`'s `@PreAuthorize` to also accept `SUPPORT_MANAGE`, mirroring
  the exact precedent already set for `PROFILE_MANAGE` (see that entry above) - an admin
  replying to a ticket needs to attach a screenshot too, and the alternative (a parallel
  `/admin/media` controller for one method) was already rejected once for the same reason.
- New shared `AttachmentPicker` (`features/support/attachment-picker.tsx`): uploads immediately
  on file selection (matches `ImageUploadField`'s existing UX, not deferred to message-send
  time, since the message body is a single JSON `POST` with an already-hosted URL rather than a
  multipart form). Dropped into the one shared `ReplyForm` used by both the client and admin
  ticket detail views, and into `CreateTicketDialog` for attaching a screenshot when first
  opening a ticket - three attachment entry points, one upload component.
- `TicketMessages` (also shared between client/admin views) renders the attachment as a
  clickable thumbnail inside the message bubble when present.

**A real regression found and fixed while verifying, not a hypothetical**: adding the second
field to the `MessageCreateRequest` record broke `SupportServiceTest` - five existing unit
tests called the record's old single-argument constructor directly
(`new MessageCreateRequest("Any update?")`), which a record's positional constructor doesn't
tolerate once a field is added. `mvn test` caught this immediately (`NoSuchMethodError`, not a
logic failure) during the routine final-check pass, confirming why that pass happens on every
change here regardless of how contained the change looks. Fixed by passing `null` for the new
`attachmentUrl` parameter at each of the five call sites - back to the standard 29/31 baseline.

Verified live against the real backend (Chrome): sent a real reply with an attached image as
the client, confirmed via the raw API response that `attachmentUrl` persisted correctly, then
confirmed the same from the admin side of the same ticket (both list the identical message with
`fromSupportStaff` correctly attributed), then sent an admin reply with its own attachment and
confirmed that round-tripped too. The rendered thumbnail was genuine but easy to mistake for a
render failure at a glance - the real 1x1 test PNG used for the upload displays as a near-invisible
dot at normal screenshot zoom; confirmed via the DOM (`naturalWidth: 1`, `complete: true`) that it
was a correctly-loaded image, not a broken one, rather than assuming from the screenshot alone.
`mvn test` (29/31 after the fix, same pre-existing Testcontainers-only failures),
`npm run typecheck`/`npm run lint`/`npm run build` (all 39 routes) all clean.

## Post-roadmap: convert a lead to a client (2026-08-17)

Clears one item of the Phase 9 remainder ("Convert to client" was a lead status label only,
with no action behind it - an admin had to manually re-type the lead's name/email/phone into
the Create Client dialog). New `POST /admin/leads/{uuid}/convert` (`LeadService.convertToClient`)
builds a `ClientCreateRequest` from the lead's captured fields (display name = company if set,
else the contact name) and hands it straight to the existing `ClientService.createClient` -
same temp-password/welcome-email/duplicate-email handling as any other client creation, not
reimplemented. Guards against re-converting an already-`CONVERTED` lead
(`ConflictException`), and gates on `LEAD_MANAGE` **and** `CLIENT_CREATE` rather than
`LEAD_MANAGE` alone - this creates a real client account, so lead-management access must not be
a backdoor around the separate client-creation permission. New `ConvertLeadDialog`
(`features/leads/convert-lead-dialog.tsx`) reuses the same show-once-temporary-password pattern
as `CreateClientDialog`/`CreateAdminUserDialog`, defaults the client-type picker to Business
when the lead has a company name, and links straight to the new client's detail page.

**A real bug found and fixed while verifying this, not a hypothetical**: the dialog originally
hid itself whenever `lead.status === "CONVERTED"` - correct for hiding the trigger button on an
already-converted lead in the list, but converting invalidates the leads query, which refetches
and flips that same prop to `CONVERTED` *while the result screen showing the one-time temporary
password is still open*, unmounting the whole dialog (password included) before the admin ever
saw it. First live test reproduced it twice in a row (status correctly updated to CONVERTED,
client correctly created, but the password screen never appeared) before the cause was found by
reading the guard against the invalidation timing, not by chance. Fixed by guarding on
`lead.status === "CONVERTED" && !open` - the dialog now only self-hides once the admin has
closed it, never out from under an open result screen. Re-verified live after the fix: the
result screen now stays open showing the real generated password and correctly linking to the
new client, exactly as designed.

Verified live end-to-end against the real backend (Chrome, admin login): converted three real
test leads (one Business via a company name, two Individual), confirmed the created clients had
the correct type/display-name/email in the real client list, confirmed the audit log recorded
`LEAD_CONVERTED` with the new client's uuid in metadata, and cleaned up all three test client
accounts via the client-delete feature (dogfooding it) afterward. `mvn test` (29/31, same
pre-existing Testcontainers-only failures), `npm run typecheck`/`npm run lint`/`npm run build`
(all 39 routes) all clean.

## Post-roadmap: PROFILE_VIEW and MENU_VIEW analytics events (2026-08-17)

Clears the Phase 7 remainder. Both event types (`AnalyticsEventType.PROFILE_VIEW`/`MENU_VIEW`)
and their read side already existed - `AnalyticsService.summaryFor` already counted
`PROFILE_VIEW` into the client dashboard's "Profile Views" tile - only nothing ever wrote one.
Went with the client-side-beacon option this doc's own earlier note called out, not the
header-forwarding fix: the public profile/menu pages are plain `fetch()` in a Server Component,
so recording the view during that SSR fetch would forward Next's own request headers instead of
the visitor's real browser User-Agent, corrupting the device/browser breakdown - the entire
reason this was deferred in the first place. A same-origin client-side POST needs no forwarding;
the browser sends the visitor's real headers on the actual wire request.

- New `POST /public/profile/{slug}/view`, `/public/company/{slug}/view`, `/public/menu/{slug}/view`
  (rate-limited 20/min/IP via the existing `RateLimiterService`, same pattern as the NFC/QR
  redirect hot paths). Each resolves the client by slug and delegates to the same
  `AnalyticsService.recordEvent` the NFC/QR taps already use - `ProfileService`/`MenuService`
  gained one new dependency and 2-3 small methods each, no new service. Silently no-ops on an
  unknown/unpublished slug rather than erroring - this fires on every page load and must never
  surface anything to the visitor.
- New `ViewBeacon` client component (`components/analytics/view-beacon.tsx`): fires once on
  mount via `navigator.sendBeacon` (falling back to a `keepalive` fetch), return value
  deliberately ignored. Dropped into all three public pages (`/p/[slug]`, `/company/[slug]`,
  `/menu/[slug]`) alongside their existing Server Component content - each just renders
  `<ViewBeacon path="..." />` next to the real page, no restructuring needed.
- Already under `/api/v1/public/**`, so no `SecurityConfig` change was needed - that prefix was
  already `permitAll()` and CSRF-ignored.

Verified against the real backend (Chrome): visited `/p/nadeesha-perera`, confirmed the beacon
POST (200) in the network log, then confirmed via the real admin analytics summary endpoint
that `totalProfileViews` incremented and the device breakdown correctly read `"desktop"` - the
exact accuracy problem this was built to fix, proven fixed, not just "no errors." Repeated for
`/menu/idor-test-client-b` (temporarily published that test client's menu to test against,
unpublished it again afterward). The beacon fired twice per page load in this dev session - React
18 StrictMode's intentional effect double-invocation in dev, not a bug; production builds only
mount once. `mvn test` (29/31, same pre-existing Testcontainers-only failures), `npm run
typecheck`/`npm run lint`/`npm run build` (all 39 routes) all clean.

## Post-roadmap: drag-to-reorder menu categories/items (2026-08-16)

Clears the Phase 6 remainder. The backend already supported arbitrary `sortOrder` on both
`MenuCategory` and `MenuItem` (`updateCategory`/`updateItem` already accepted an optional
`sortOrder` field) - only the editor UI was missing a way to set it, so this shipped as a
frontend-only change with no backend/migration work.

- Hand-rolled with native HTML5 drag-and-drop (`draggable`, `onDragStart`/`onDragOver`/`onDrop`)
  rather than pulling in a drag library - no dnd-kit/react-beautiful-dnd in this project's
  dependencies, matching the existing "hand-built over new library" pattern (see the client
  combobox's reasoning in its own post-roadmap entry). A small grip handle
  (`GripVertical`, `features/menu/menu-editor-content.tsx`) is the drag source for both category
  cards and item rows; item drags are scoped to reordering within their own category, not
  moving items across categories - out of scope for what "drag-to-reorder" was asked for here.
- On drop, reuses the *existing* `PUT categories/{uuid}` and `PUT categories/{uuid}/items/{uuid}`
  endpoints - no new reorder-specific endpoint - resending each row's already-known fields
  (name/active or name/price/description/etc., all already in the query cache from the last
  fetch) alongside the new `sortOrder`. Only rows whose computed position actually changed are
  sent (`Promise.all` over the filtered diff), not the whole list on every drag.
- Verified live against the real backend (Chrome, client login `idortest-b@ceylonfc-test.local`,
  seeded 2 categories / 3 items via direct API calls first). Real mouse-driven drag gestures
  don't reliably trigger native HTML5 DnD through this session's browser-automation tooling (a
  tooling limitation hit repeatedly today, not an app issue), so this was verified instead by
  dispatching real `DragEvent`s with a `DataTransfer` against the live rendered DOM - the exact
  same handlers a real drag fires. First attempt fired all three events synchronously in one
  tick and produced zero network requests; adding ~50ms waits between `dragstart`/`dragover`/
  `drop` (letting React actually commit the `dragstart` state update before `drop` reads it -
  correctly mirrors the natural timing of a real drag, not a workaround) fixed it, and dragging
  item A to the last slot correctly fired 3 concurrent PUTs (only the 3 rows whose position
  changed) that landed in the exact new order, confirmed to persist across a hard reload.
  Category reordering verified the same way. Test data cleaned up via direct API deletes
  afterward. This also organically re-verified the earlier auth-refresh fix a third time today,
  now under concurrent `Promise.all` PUTs specifically - all three requests raced a naturally
  expired access token, hit 401, and all recovered via the single deduped `/auth/refresh` rather
  than the reuse-detection logout bug the earlier fix addressed. `npm run typecheck`/
  `npm run lint` clean; backend untouched.

## Post-roadmap: QR code customization - colors, error correction, logo, SVG export (2026-08-16)

Clears the Phase 5 QR remainder. Correction first: the "admin has read-only list endpoints"
framing in this doc's Pending section was stale - `AdminQrCodeController` already had
`create`/status-toggle parity with the client controller (added in an earlier, undocumented
pass). What was genuinely still missing, and the only part of the Phase 5 QR note still
accurate, was the customization piece: `QrImage` was a fixed black-on-white canvas render with
no color/logo/error-correction control and PNG as the only export.

Every existing QR code's actual target URL is a one-time reveal at creation (`QrCodeResponse`,
returned by the list endpoint, deliberately excludes it - only `QrCodeCreateResponse` carries
`publicUrl`, mirroring the NFC register-token pattern). That constrains where customization can
live: it has to happen in the create flow's result step, not as a later action on an existing
QR code, since the URL needed to regenerate the image isn't retrievable again afterward.

- `QrImage` (`features/qr/qr-image.tsx`) now takes an optional `style` prop (foreground/
  background hex, error-correction level L/M/Q/H, optional logo data URL) instead of hardcoded
  black-on-white/level M. A logo is drawn centered on the canvas after the QR renders, backed
  by a white padding square for contrast - only reliable at level H (~30% redundancy), so
  picking a logo file auto-bumps the level there.
- New `QrCustomizationPanel` (`features/qr/qr-customization-panel.tsx`): color pickers, error
  correction select, logo file input, and PNG + SVG download buttons, live-previewing through
  the same `QrImage`. SVG export uses `QRCode.toString(..., {type:"svg"})` and, when a logo is
  set, string-injects a matching `<rect>`+`<image>` pair before `</svg>` - the canvas and SVG
  renderers are two separate code paths in the `qrcode` library with no shared logo-overlay
  support, so this is kept in sync by hand and was verified directly (parsed with `DOMParser`,
  confirmed no `parsererror`) rather than assumed.
- Both `create-qr-dialog.tsx` (client) and `admin-create-qr-dialog.tsx` (admin) were
  near-identical duplicates of the same result-step markup; both now render this one shared
  panel instead of each hand-rolling its own canvas-download button, so the two dialogs can't
  drift out of sync on this again.

Verified live in the real browser (Chrome, admin login, Ceylon Cafe Colombo): created a QR
code, confirmed the panel renders with a real scannable code; changed the foreground color
programmatically (native `<input type="color">` swatches open an OS-level picker that's unsafe
to drive via automation, so the value was set through React's native-setter + `input`/`change`
event path instead, same as a real color pick) and confirmed the canvas re-rendered live in
the new color immediately. SVG generation and the logo-embedding string injection were verified
directly against the real `qrcode` library and `DOMParser` (not just read for correctness).
`npm run typecheck`/`npm run lint` clean; `mvn` unaffected (frontend-only change, no backend
touched).
- **Aside, unrelated to this feature**: mid-session browser-automation clicks intermittently
  landed on the wrong element or triggered unintended navigation (a real but separate
  flakiness in the Chrome automation tooling itself, reproduced independently of any app
  code) and one stray click suspended the pre-existing "Front Door Menu" QR code; caught via a
  direct API check against all four of the client's QR codes afterward and reactivated
  immediately - no other side effects found.

## Post-roadmap: RBAC admin UI, user management, client delete (2026-08-16)

Clears the last Phase 2 remainder. Three real gaps, all previously unmanageable via any UI:

- **Per-admin permission overrides had no UI.** `admin_permission_overrides` and the
  `ADMIN` role (seeded with zero permissions - "granted individually" per
  `ROLE_PERMISSION_MATRIX.md`) already existed and were already read on every request
  (`PrincipalFactory`), but nothing ever wrote to that table outside a database console. In
  practice this meant a freshly created `ADMIN` account had literally no access to anything
  until someone hand-edited the database. New `AdminUserController`/`AdminUserService`
  (`/api/v1/admin/users/**`, entirely `SUPER_ADMIN`-only - granting access is inherently a
  Super Admin action, so a permission-holding `ADMIN` can't extend their own or another
  admin's grants) add list/create/status/`GET+PUT /{uuid}/permissions`. The update endpoint
  takes the full desired set of granted codes and replaces the user's override rows in one
  transaction rather than incremental add/remove calls, so the frontend just submits "here's
  the checked list" each save.
- **No user management screen.** New `/admin/users` (list + search) and `/admin/users/[uuid]`
  (status toggle + permission checkbox grid) pages, following the same `ClientPicker`-adjacent
  list/detail conventions as Clients. Sidebar's "Users" entry switched from disabled "Soon" to
  live. Disabling a staff account also revokes all of their refresh tokens immediately
  (mirrors what `ClientService`/`AuthService` already did for the equivalent client and
  self-logout paths) rather than just flipping a status flag that takes effect on next natural
  expiry.
- **No client delete.** `CLIENT_DELETE` was already a seeded permission code checked
  nowhere - `AdminClientController` had list/create/update/suspend but no delete. Added
  `DELETE /admin/clients/{uuid}`: soft-deletes the client (`deletedAt`, matching the existing
  pattern every other query already filters on), sets the owning user's account to `DISABLED`,
  and revokes their sessions - same effect as disabling a staff account, reused rather than
  duplicated. Historical records (orders, audit trail) are deliberately kept, not erased. New
  `ConfirmDialog` (`components/ui/confirm-dialog.tsx`) is the project's first destructive-action
  confirmation - no existing dialog primitive for this, so it's a small generic wrapper over the
  existing hand-built `Dialog`, reusable for future delete flows instead of `window.confirm`.

Verified live end-to-end in the real browser (Chrome, admin login): created a new `ADMIN`
user via the show-once-temporary-password flow (same pattern as client creation), confirmed
it starts with zero permissions (all 24 checkboxes unchecked), granted `ANALYTICS_VIEW` +
`CLIENT_VIEW` and confirmed persistence across a hard reload, then confirmed via direct
`/auth/login` that the issued session's `permissions` array was exactly
`["ANALYTICS_VIEW","CLIENT_VIEW"]` - the real access-control path, not just the DB row.
Confirmed a module still gated `SUPER_ADMIN`-only regardless of overrides (`/admin/products`,
pre-existing, unrelated to this change) correctly still 403s for this admin - overrides don't
over-grant. Confirmed Disable immediately kills the ability to log in (`INVALID_CREDENTIALS`
on next login attempt) and Activate restores it. Confirmed client delete: soft-deleted,
disappears from the list, owner account disabled, and along the way organically re-verified
the auth-refresh fix from earlier today - the delete's first attempt hit a naturally-expired
access token (401), transparently refreshed, and retried to success, exactly as designed.
Every action (`USER_CREATE`, `USER_PERMISSIONS_UPDATE`, `USER_STATUS_CHANGE`,
`CLIENT_DELETE`) shows up correctly in Audit Logs with accurate metadata. `mvn test` (29/31,
same pre-existing Testcontainers-only failures), `npm run typecheck`/`npm run lint`/
`npm run build` (all 39 routes, two new) all clean.

## Bug fix: session silently dropped after access-token expiry (2026-08-16)

Found while doing a full local verification pass (real backend + frontend + browser, not just
`mvn test`/`npm run build`) - a hard navigation issued after the 15-minute access token expired
landed on `/login` instead of transparently refreshing, even though the refresh token (30-day
expiry) was still valid. Two independent bugs compounded:

- **Backend returned 403, not 401, for unauthenticated requests.** `SecurityConfig` had no
  custom `AuthenticationEntryPoint`, so Spring Security's default (`Http403ForbiddenEntryPoint`)
  answered every request with an expired/missing access token with an empty-body `403`. The
  frontend's `apiClient` only treats `401` as "try `/auth/refresh` and retry" - a `403` just
  fails outright, so `AuthProvider`'s `/auth/me` query resolved to "not authenticated" and
  `RoleGuard` redirected to `/login` on the very next request after every token expiry. Fixed
  with a new `JwtAuthenticationEntryPoint` (`security/jwt/JwtAuthenticationEntryPoint.java`),
  wired via `.exceptionHandling(...).authenticationEntryPoint(...)` in `SecurityConfig`, that
  returns a real `401` in the same `ApiResponse` JSON envelope every other endpoint uses.
- **Concurrent 401s each triggered their own `/auth/refresh` call, racing the single-use
  rotating refresh token.** Once the first bug was fixed, reproducing the flow with several
  requests expiring at once (e.g. the notification poll + a data fetch, both totally normal)
  surfaced a second, worse bug: `AuthService.refresh()` treats reuse of an already-rotated
  refresh token as compromise and revokes the *entire* session
  (`revokeAllForUser`) - so a losing concurrent refresh call could kill the token the winning
  call had just legitimately issued, forcing a real logout under ordinary traffic, not an
  attack. Fixed on the frontend (`lib/api/client.ts`) by de-duplicating concurrent refreshes
  into a single shared in-flight `Promise` (`refreshSession()`) that every 401'd caller awaits,
  instead of each firing its own `/auth/refresh`. The backend's revoke-on-reuse behavior itself
  is correct and was left alone.

Reproduced deterministically by temporarily running the backend with
`JWT_ACCESS_EXPIRATION=5000` (5s) locally to force expiry quickly, confirmed both bugs via
real network traces in Chrome (`403` -> now `401`; multiple `/auth/refresh` calls racing ->
now exactly one per expiry burst, both retries succeed), then re-verified against the normal
15-minute expiry. `mvn test` (29/31, same pre-existing Testcontainers-only failures - see Known
Issues), `npm run typecheck`/`npm run lint` all clean after the fix.

## Post-roadmap: Write NFC page (2026-08-14)

Clears the Phase 4 remainder item: an admin UI showing the secure URL + QR for writing to a
physical chip - the register API already supported this, no backend change needed. Added
`/admin/write-nfc` (`WriteNfcPageContent`), a full-page version of the existing "Register NFC
Card" dialog's register step, reusing the same `nfcCardsApi.register` mutation/schema and the
already-verified `QrImage`/download-PNG pattern from the QR Codes feature - no new API client
method, no new chart/QR code. Sidebar's "Write NFC" entry switched from disabled "Soon" to a
live link. The existing dialog on `/admin/nfc-cards` (Phase 4, already shipped) is unchanged
and still the quick-register path; only its description text was corrected - it referenced
"Write NFC page, coming soon", now points at the real page instead. Verified in the real
browser (Chrome, admin login): registered a real card (`CEY-TEST-001`), got a real secure URL
and scannable QR code back, "Register another" correctly resets the form, and the new card
shows up in the `/admin/nfc-cards` list afterward (cache invalidation works). No console
errors. `npm run typecheck`/`npm run lint` clean.

## Post-roadmap: searchable client combobox on the NFC assign form (2026-08-15)

Clears the last Phase 4 remainder item. The NFC card assign form's client field was a plain
`<Select>` fed by `clientsApi.list({ size: 100 })`, filtered client-side against whatever fit in
that capped page - correct today, but silently wrong past 100 clients (the 101st+ client
couldn't be assigned a card at all, not just hard to find). New `ClientCombobox`
(`components/admin/client-combobox.tsx`) queries `clientsApi.list({ search })` server-side as
the admin types, debounced 250ms, so it scales with the actual client count instead of a fixed
page size. No `cmdk`/Radix Popover in this project's dependencies (design system is hand-built
per TECHNICAL_DECISIONS.md - shadcn's CLI hung here), so this is a small hand-built input +
absolutely-positioned dropdown rather than a new library pulled in for one field. Label
resolution for a pre-set `value` (not just one picked through the component) is derived
directly from query data during render rather than mirrored into state via an effect - the
first draft did that with `useEffect(() => setState(...))` and ESLint's
`react-hooks/set-state-in-effect` rule (ejected out of `next lint`'s defaults toward React's
newer effect guidance) correctly flagged both instances as unnecessary render-cascading state
sync for something fully derivable from existing query data.
- Replacing the old `<Select>` also removed the `clientsPage?.content.find(...)` lookup
  `nfc-card-detail-content.tsx` used to get the selected client's `type` (for the Digital
  Profile/Company Profile destination-type branch) - since the combobox no longer holds the
  full client list in memory. Replaced with a direct `clientsApi.get(selectedClientUuid)`
  lookup, which is actually more correct than before: the old `.find()` could silently return
  `undefined` for any client outside the first 100, making that branch quietly disappear.
- Verified live in the real browser (Chrome, admin login): opened an unassigned card's assign
  form, typed "Nadeesha" and confirmed the dropdown narrowed to the one real matching client via
  a live backend query (not a pre-loaded list), selected it and confirmed the field displays the
  chosen name and closes the dropdown, then confirmed the destination-type dropdown correctly
  showed "Digital Profile" for that INDIVIDUAL client - proving the `.find()` replacement
  carries the client-type branch correctly. No console errors. `npm run typecheck`/
  `npm run lint`/`npm run build` all clean.

## Post-roadmap: admin-side profile editing UI (2026-08-15)

Clears the Phase 3 remainder item: `PROFILE_MANAGE` existed in the permission matrix with no
admin UI - only client self-service editing was built. `ProfileService`'s four own-client
methods (`getOrCreateOwnProfile`, `updateOwnProfile`, `setPublished`, `replaceSocialLinks`)
were each split into a public own-client entry point plus a private `Client`-parameterized
core, so the new admin entry points (`getOrCreateProfileForClient`/`updateProfileForClient`/
`setPublishedForClient`/`replaceSocialLinksForClient`, all resolving the client by uuid instead
of by the authenticated owner) share the exact same validation/slug/publish logic rather than
duplicating it - a real behavior difference (draft-vs-published enforcement, slug uniqueness)
would otherwise drift between the two paths over time. New `AdminProfileController`
(`GET/PUT /admin/profile`, `POST /admin/profile/publish`, `PUT /admin/profile/social-links`,
all `?clientUuid=`, mirroring the existing Reviews/Analytics admin-by-clientUuid convention)
under the controller-level `PROFILE_MANAGE`/`SUPER_ADMIN` gate.
- **A real gap found while wiring this up, not a hypothetical**: the profile editor needs image
  upload (photo/logo/cover), but `MediaUploadController` was `hasRole('CLIENT')`-only - an
  admin editing someone else's profile would have hit a 403 on every image field. Broadened the
  `@PreAuthorize` to also accept `SUPER_ADMIN`/`PROFILE_MANAGE` rather than standing up a
  parallel `/admin/media/upload` controller for one method - `StorageService.store` was already
  category-scoped, not per-client-scoped, so there was no tenant-isolation reason to keep it
  client-only, just an oversight from when only client self-service existed.
- **Frontend:** `ProfileEditorContent` and `SocialLinksEditor` (previously hard-wired to the
  client's own-profile API calls) both gained an optional `clientUuid` prop that switches them
  to the new `adminProfileApi` - same split-panel form + live phone preview + image upload +
  social links UI reused verbatim for admin, not a parallel copy. New `/admin/profile` page
  (`AdminProfilePageContent`) follows the `ClientPicker` convention already used by Reviews/
  Analytics/Subscriptions, with one addition: it reads an optional `?clientUuid=` query param
  (via `useSearchParams`, page wrapped in `Suspense` per the same pattern
  `(auth)/reset-password` already established) to preselect the client - wired to a new "Edit
  Profile" button on `/admin/clients/[uuid]`, so the actual admin workflow is one click from a
  client's detail page straight into their profile editor, not picker-then-search. Sidebar
  "Profiles" entry added (client-facing sidebar already has its own separate "My Profile" entry
  pointing at the unchanged client-only page - these are deliberately different routes/pages).
- Verified live in the real browser (Chrome, admin login), both profile types: opened a
  BUSINESS client's profile via the new client-detail deep link, edited and saved a real field
  (Industry), confirmed persistence via the admin GET endpoint, published it, and confirmed the
  public `/company/{slug}` endpoint actually started returning the profile (was 404 while
  draft). Separately opened an INDIVIDUAL client's profile via the picker and confirmed the
  form correctly renders the individual-specific fields (full name/job title/"Profile photo"
  vs. company name/industry/"Company logo"). Confirmed the media-upload permission fix with a
  real authenticated request as the admin user (not the client) - a 1x1 PNG uploaded
  successfully (200, not 403) and was retrievable back over HTTP at its returned URL. No
  console errors. `npm run typecheck`/`npm run lint`/`npm run build` all clean (the
  `useSearchParams` + `Suspense` combination has stricter production-build requirements than
  dev, so a full `next build` was run specifically to catch what typecheck/lint alone would
  miss). Backend `mvn test`: 29/31, same pre-existing Testcontainers gap as every prior phase.

## Post-roadmap: card replacement workflow (2026-08-14/15)

Clears the Phase 4 remainder item: replacing a lost/damaged physical card previously meant
manually re-registering a new card and redoing the whole assign flow, re-typing the same
destination. `NfcCardService.replace` registers a new card that inherits the old card's
`clientId`/`destinationId` directly (not through `assign()`, so it deliberately does not
re-run `SubscriptionService.assertCanAssignCard` - a like-for-like swap isn't a new assignment
against the plan's card limit) and marks the old card `REPLACED` - an enum value that already
existed but was previously unreachable from any API. No separate "deactivate old card" step is
needed: `resolveRedirectTarget` already treats any non-ACTIVE card as inactive, so the old
token stops resolving the instant this runs. `POST /admin/nfc-cards/{uuid}/replace` returns the
same `NfcCardRegisterResponse` shape the register endpoint does (new secure URL + rawToken,
shown once), so the frontend `ReplaceNfcCardDialog` reuses the exact QR/copy/download UI
already verified for Write NFC and QR Codes. Wired into `/admin/nfc-cards/[uuid]` as a
"Replace card" button next to Activate/Suspend, shown only while the card is actually assigned
and not already replaced.
- **Caught and fixed a real bug while building this**: `cardsUsed` (used for both the plan's
  card-limit enforcement and the display on `/admin/subscriptions` and the client detail page)
  counted every `NfcCard` row for a client regardless of status. Before this feature, a client
  could only ever have one row per card, so this never mattered; the moment a REPLACED row and
  its live replacement coexist for the same client, the count would double for a client who
  simply had a card replaced - showing "2/1" for a client on an unchanged 1-card plan. Fixed by
  excluding REPLACED cards from the count in both `SubscriptionService.assertCanAssignCard` and
  the single/batched `toResponse`/`enrich` paths (SUSPENDED/LOST/etc. still count - only a
  REPLACED card is genuinely decommissioned). The existing `SubscriptionServiceTest` mocks
  default `NfcCard.status` to `UNASSIGNED`, so this fix didn't require touching the tests -
  confirmed by re-running them (still 0 failures).
- Verified live in the real browser (Chrome, admin login): replaced an actively-assigned card
  (`CEY-0001`, 6 taps of real history), confirmed the old card immediately shows `REPLACED` and
  loses its action buttons, the new card (`CEY-0001-REPLACEMENT`) shows the same
  client/destination and is `ACTIVE`, both rows appear correctly in the NFC Cards list, and
  `/admin/subscriptions` still shows `1/1` cards used for that client (not `2/1`) - proving the
  counting fix works against real data, not just the unit test. No console errors.
  `npm run typecheck`/`npm run lint` clean; backend `mvn test` still 29/31 (same pre-existing
  Testcontainers gap as every prior phase, not a new failure).

## Post-roadmap: admin standalone subscriptions page (2026-08-14)

Clears the Phase 8 remainder item: "which clients are on plan X" needed its own view instead of
being reachable only per-client. No list capability existed at all - `SubscriptionRepository`
only had `findByClientId` (a 1:1 lookup) - so this needed real backend work, not just a
frontend page. Added `SubscriptionRepository.search` (status + package-plan filters, paginated),
`SubscriptionService.list` with the same batched-enrichment pattern `OrderService.enrich` and
Phase 10's performance pass established (`findAllById`/`findAllByClientIdIn` instead of a query
per row), and `GET /admin/subscriptions/list`, gated by the controller's existing
`SUBSCRIPTION_MANAGE`/`SUPER_ADMIN` authorization. `SubscriptionResponse` gained
`clientUuid`/`clientDisplayName` (additive, matching the `OrderResponse` convention) so the list
can show which client each row belongs to - the per-client detail endpoint just passes its
already-known `Client` through the same `toResponse` now. Frontend: `/admin/subscriptions`
(status + plan filters, paginated table, client name links to `/admin/clients/[uuid]`); sidebar
"Subscriptions" entry re-added (it was deliberately removed in Phase 8 when this was only
reachable per-client - see that phase's entry above). Verified live in the real browser (Chrome,
admin login): real data from an existing subscription plus a second one created via the API to
confirm multi-row rendering, status filter narrows correctly, plan filter narrows correctly, and
the client name link lands on the matching client detail page showing the same plan/status/cards
data. No console errors. `npm run typecheck`/`npm run lint` clean; backend compiles clean.

## Post-roadmap: order detail/print view (2026-08-14)

Clears the Phase 8 remainder item: no single-order endpoint existed at all (`AdminOrderController`
only had list/create/status/payment-status), so this needed a real backend addition, not just a
frontend page. Added `OrderService.getByUuid` (reuses the existing single-order `toResponse`
helper, not the list path's batched `enrich` - a detail page is exactly the N=1 case the
batching optimization doesn't matter for) and `GET /admin/orders/{uuid}`, same
`ORDER_MANAGE`/`SUPER_ADMIN` authorization as the rest of the controller. Frontend:
`/admin/orders/[uuid]` (`OrderDetailContent`) with a Print button; order numbers in the orders
list are now links to it. Print support is a real `window.print()` of the actual page, not a
separate template - added `print:hidden` to `DashboardSidebar`/`DashboardHeader` (shared by
every admin/client page, so this is now available platform-wide, not just for orders) and
`print:border-none print:shadow-none` on the order card, verified by inspecting the compiled
stylesheet's `@media print` rules directly rather than by driving the native print dialog
(which blocks browser automation the same way an `alert()` does).

**Caught and fixed a real, pre-existing environment bug while verifying this**: the long-running
`npm run dev` process had a stale Turbopack worker pool from earlier in this session (visible as
an orphaned `pool_entry-[turbopack-node]_transforms_postcss` node process) that made *every*
dynamic `[uuid]` route - not just the new one - fail with "Jest worker encountered 2 child
process exceptions, exceeding retry limit" on first compile. Confirmed it wasn't a bug in this
change by reproducing the identical error on the already-shipped, already-verified
`/admin/nfc-cards/[uuid]` page against the same dev server process. Fixed by killing the stale
node/Turbopack processes and restarting `npm run dev` clean; both routes then compiled and
rendered correctly. Verified live in the real browser (Chrome, admin login): order detail page
renders real data (client, notes, line items, subtotal/total, status/payment badges) for a real
order, and the fix didn't regress the NFC card detail page. No console errors.
`npm run typecheck`/`npm run lint` clean.

## Doc correction: client order history page (2026-08-14)

Went looking to build the Phase 8 remainder item "client-facing order history page (`GET
/client/orders` API exists, no page wired to it yet)" and found it already fully built:
`ClientOrderController`, `ordersApi.listOwn()`, `ClientOrdersPageContent`, and the sidebar
"Orders" link all already exist and are already wired together - the doc just never got
updated when it shipped. Verified it's real, not just present in the tree: created a fresh
test client and a real order for them via the admin API, logged in as that client in the
browser, and confirmed `/client/orders` renders the order correctly scoped to that client
(order number, items, total, status/payment badges, created date) with no console errors.
Pending list corrected accordingly.

## Post-roadmap: admin analytics UI (2026-08-13)

Clears the Phase 7 remainder item: `GET /admin/analytics/summary?clientUuid=` existed with no
admin page. Added `/admin/analytics` (`AdminAnalyticsPageContent`), following the exact
`ClientPicker` + `enabled: Boolean(clientUuid)` read-only-admin pattern already used by
Google Reviews/QR Codes, and reusing the same `ActivityChart`/`DeviceChart` components the
client-facing `/client/analytics` page already verified in Phase 7 - no new chart code, no new
API client method (`analyticsApi.adminSummary` already existed, unused until now). Sidebar's
"Analytics" entry switched from a disabled "Soon" item to a live link. Verified in the real
browser (Chrome, admin login): empty state before a client is selected, then selecting "Ceylon
Cafe Colombo" loads real data (4 QR scans / 0 NFC taps in the last 7 days, correct activity
chart with tooltip, device breakdown bar chart, and an honest "No taps yet in this range." empty
state for the top-cards panel rather than a fabricated placeholder) - not just a curl check of
the endpoint, which already worked. No console errors. `npm run typecheck`/`npm run lint` clean.

## Post-roadmap: media/storage module (2026-08-12)

The roadmap's 10 phases are complete; this and further entries below clear the accumulated
"remainder" backlog each phase's docs entry left behind, starting with the item three
different phases independently flagged as missing: "no StorageService/media module yet."

Real image uploads now exist end-to-end - not stub URL text fields, not a fake preview.
Verified by actually uploading a file through the real browser UI (profile logo), confirming
the "Image uploaded" toast, saving the profile, publishing it, and loading the real public
`/company/[slug]` page to see the uploaded image rendered - the exact page a customer would
see after scanning an NFC card.

- **Backend:** `StorageService` interface with one implementation today,
  `LocalStorageService` (writes under `app.storage.path`, served back out at `/media/**`).
  An S3-compatible implementation is intentionally not built yet - it can't be verified
  without real S3-compatible credentials this environment doesn't have, and a wrong-but-
  unverified implementation is worse than an honest gap; the interface exists specifically so
  swapping it in later doesn't touch any caller. `FileValidator` enforces
  docs/SECURITY.md's file-upload rules for real: 5MB cap, magic-byte signature detection
  (not just trusting the extension or declared content-type - a renamed non-image is
  rejected), a cross-check that the declared content-type actually matches the detected
  bytes, and SVGs rejected outright (no sanitizer in this project, so "reject unless
  sanitized" means reject). `MediaCategory` is a closed enum, not a client-supplied folder
  string, specifically because the category becomes part of a filesystem path in
  `LocalStorageService` - an open string there would be a path-traversal vector. One upload
  endpoint (`POST /client/media/upload`) serves every image type (profile photo, cover,
  company logo, menu item photo) rather than a bespoke path per feature.
- **Caught and fixed a real bug while building this**: the `/media/**` static resource
  handler returned 404 for every freshly-uploaded file on a clean checkout.
  `Path.toUri()` only appends the trailing slash Spring's resource resolver needs for a
  directory-style resource location when that directory already exists on disk at the time
  `toUri()` is called - on first boot, before any upload has happened, `./media` doesn't
  exist yet, so the location URI silently came out wrong and every request 404'd. Fixed by
  creating the directory eagerly in `MediaResourceConfig` instead of deferring to
  `LocalStorageService`'s lazy `createDirectories` call. Caught by actually requesting the
  uploaded file back over HTTP after uploading it, not just checking the upload response
  was 200 - the upload endpoint itself never touched this code path, so a curl-only test of
  just the POST would have missed this entirely.
- **Frontend:** `ImageUploadField`, one reusable component (preview thumbnail, upload/replace/
  remove, client-side size/type pre-check before ever hitting the network) wired into the
  profile editor (photo/logo/cover, `Controller`-managed since it's not a native form input)
  and the menu item dialog. Required a small extension to the shared `apiClient` - the
  existing `request()` helper always `JSON.stringify`'d the body and force-set
  `Content-Type: application/json`, which breaks a `FormData` upload (the browser needs to
  set its own multipart boundary); `apiClient.upload()` is the new escape hatch. Also fixed a
  real, separate gap while wiring this in: three rendering surfaces
  (`phone-preview.tsx`'s live dashboard preview, `public-profile-card.tsx`'s real public
  profile page, `public-menu-view.tsx`'s real public menu page) had `logo`/`profileImage`/
  `coverImage`/item `image` fields sitting in their data models and Zod schemas but never
  once referenced in JSX - there was no upload before, so nothing to render, and the gap
  simply carried forward silently. All three now render the real image when set, falling
  back to the existing initials/icon placeholder when not.

## Phase 10: Security audit, test coverage, Playwright E2E, performance, deployment (2026-08-12)

The roadmap's final phase - hardening and verifying what Phases 1-9 built, rather than new
features. Every finding below came from actually running something (a test, a live curl
attack, a real Docker build), not a read-through.

- **Security audit.** Cross-checked the running system against `docs/SECURITY.md`'s claims
  line by line. Two real gaps found and fixed: (1) `docs/SECURITY.md` promised Bucket4j rate
  limiting on "login, forgot-password, reset-password, public lead form, support ticket
  creation" but only the NFC/QR redirect endpoints actually had it - added to all five
  (login/forgot/reset keyed by IP via a new shared `ClientIpResolver`, since the identical
  IP-extraction logic was independently duplicated four times already; leads by IP; support
  ticket creation by user id, since it's an authenticated endpoint); verified live by
  hammering each endpoint past its limit and confirming 429s. (2) `Content-Security-Policy`
  and `Permissions-Policy` response headers were documented but never configured (only
  `X-Content-Type-Options`/`X-Frame-Options`/`Referrer-Policy` were) - added both to
  `SecurityConfig`, verified via curl. The tenant-isolation audit (SECURITY.md's "critical
  security test") checked all 9 client-facing controllers for the `findByUuid` +
  after-the-fact-check anti-pattern the codebase forbids - found zero violations; every
  lookup already used `findByUuidAndClientId` or an equivalent ownership-chained query.
  Verified live, not just by reading code: created two real client accounts, gave one
  (`Client B`) an NFC card, QR code, Google Review location, menu category and support
  ticket, then confirmed the other (`Client A`) got a 404 attempting to read or modify every
  one of them by UUID, while `Client B` could still access its own - real attacker-shaped
  proof, not a code review.
- **Test coverage.** `TenantIsolationTest.java` encodes the exact live-verified IDOR scenario
  above as a permanent Testcontainers regression test (see "Known Issues" - can't execute on
  this Windows dev machine, runs in CI). Four new pure-Mockito unit test classes (no DB, run
  anywhere): `SubscriptionServiceTest` (card-limit enforcement across every subscription
  status/limit combination), `SupportServiceTest` (the reply-driven status auto-transitions),
  `OrderServiceTest` (subtotal computation, per-day order-number sequencing), and
  `DestinationResolverServiceTest` (every destination-type branch, including the
  unpublished-profile/menu masking behavior). 29 of 31 backend tests pass locally; the other 2
  are the pre-existing Testcontainers/Docker Desktop limitation, not failures.
- **Playwright E2E**, new to this project. 7 specs against the real running dev stack (no
  mocked backend): admin login (success + rejected-credentials), admin creates a client
  end-to-end through the UI, a client logs in and sees only their own NFC card, a client opens
  a support ticket and reads the reply thread, the public pricing page renders live backend
  data, and the public contact form submits a real lead. All 7 pass.
- **Performance review** found and fixed real N+1 query patterns introduced under Phase 8/9's
  time pressure (a regression from the batching pattern every earlier module already used,
  e.g. `NfcCardService.enrich`): `OrderService`'s admin order list did one query per order for
  line items plus one per line item for its product; `SupportService`'s admin/client ticket
  lists did one query per ticket for messages plus one per message for the sender, despite
  neither list view rendering message bodies at all (fixed by returning an empty message list
  for list rows and only hydrating the full thread on the single-ticket detail endpoints);
  `QrCodeService`'s client QR list did one query per code for its destination. All three now
  batch-fetch with `findAllById`/`findAllByOrderIdIn`-style queries, mirroring the pattern
  `NfcCardService` already used. Re-verified via curl (list endpoints still return identical,
  correct data) and the full Playwright suite (still 7/7) after the change.
- **Production deployment run-through.** `docker compose build backend` succeeds and the
  resulting image boots cleanly against fresh containers (Flyway validates all 11 migrations,
  health check returns 200). The frontend image build hit a real, deterministic bug - a
  `package-lock.json`/npm-version mismatch between this dev machine and the Node 20 pinned in
  `frontend/Dockerfile` - now fixed (see "Known Issues" for the network-flakiness complication
  that followed). Added the two `.dockerignore` files and root `.env.example` that didn't
  exist before (a real gap: `frontend/Dockerfile` does `COPY . .` with nothing excluding
  `node_modules`/`.next`/test artifacts). Wrote a "Go-live checklist" in `docs/DEPLOYMENT.md`
  covering secret rotation, the `prod` vs `docker` Spring profile distinction, TLS/HSTS,
  single-instance-only constraints on the current rate limiter and local media storage, and
  the recommendation to build the frontend image in CI going forward.

## Phase 9: Leads, support tickets, notifications, audit log viewer (2026-08-11)

Turns four more "Soon"-badged sidebar placeholders into working features, and - notably -
wires up the notification bell that had sat disabled ("Coming soon") in the header since
Phase 1. Verified end-to-end via curl (public lead submit -> admin notified/listed/updated;
client ticket create -> admin notified/replied -> status auto-transitions -> client
re-replies and ticket auto-reopens; notification list/unread-count/mark-read/mark-all-read;
audit log filters+search) and in the real browser (contact form on the landing page, admin
Leads/Support/Audit Logs pages, notification bell dropdown, ticket conversation thread).

- **Backend:** `leads` / `support_tickets` / `support_messages` / `notifications` tables
  (V11 migration). New `notification` module: `NotificationService.notify`/`notifyUsers`
  write one row per recipient (never a broadcast row) so unread counts stay a plain
  per-user count; `NotificationController` is deliberately not split into admin/client
  variants like every other module - notifications are addressed to a *user*, not a
  *tenant*, so one endpoint set scoped to the authenticated principal serves both roles.
  `lead` module: `POST /public/leads` (the second deliberately public write endpoint
  besides account creation) fans out a notification to every SUPER_ADMIN/ADMIN user via a
  new role-based `UserRepository.findAllByRoleCodesIn` query - simpler than resolving
  LEAD_MANAGE-permission holders individually, with the real access-control boundary
  staying on the `@PreAuthorize` of the admin read/update endpoints, not the notification
  fan-out list. `support` module: `SupportTicket`/`SupportMessage`, status
  auto-transitions on reply (`OPEN`/`RESOLVED`/`CLOSED` -> `IN_PROGRESS` on an admin reply;
  `RESOLVED`/`CLOSED` -> `OPEN` on a client reply, so closing a ticket doesn't silently
  swallow a customer's follow-up); `fromSupportStaff` on each message is derived by
  comparing the sender's user id against the ticket's client owner id rather than stored
  redundantly, so it can never drift from reality. Extended `AuditLogRepository` with a
  filtered `search` query plus `findDistinctActions`/`findDistinctEntityTypes` (free-form
  strings across many services, not a fixed enum, so the frontend filter dropdowns are
  populated from real data rather than a hardcoded list that would drift).
- **Frontend:** `/admin/leads`, `/admin/support` (+ `/admin/support/[uuid]` ticket thread),
  `/admin/audit-logs`, `/client/support` (+ `/client/support/[uuid]`), and a public
  `ContactLeadForm` replacing the landing page's old mailto-only contact section (first use
  of `POST /public/leads` from the frontend). The notification bell in `DashboardHeader`
  polls unread-count and the latest 10 notifications every 30s and is shared by both admin
  and client layouts unchanged - matching the backend's single non-split endpoint.

## Phase 8: Packages, subscriptions, products, orders (2026-08-11)

Turns the placeholder "Packages"/"Products"/"Orders" sidebar entries into a real commerce
layer: admin-defined package plans drive both the public pricing page and real, backend-
enforced per-client card limits; admin-defined products can be ordered for a client with
computed line-item totals. Verified end-to-end via curl (create plan -> assign subscription
-> confirm card-limit block -> create product -> create order -> status/payment transitions)
and in the real browser (Packages/Products/Orders admin pages, client-detail subscription
section, public `/pricing`).

- **Backend:** `package_plans` / `subscriptions` / `products` / `orders` / `order_items`
  tables (V10 migration). `PackagePlan` limits (`cardLimit`, `profileLimit`,
  `reviewLocationLimit`, `menuLimit`) are nullable = unlimited, matching the "blank means
  unlimited" convention used for the frontend forms. `SubscriptionService.assertCanAssignCard`
  is the real enforcement point (not just a hidden button): called from
  `NfcCardService.assign` right before a card is newly attached to a client, throwing
  `SubscriptionExpiredException` (403) for an unusable subscription status or a plain
  `ValidationException` (400) once `cardLimit` is reached. No subscription record at all is
  treated as unmanaged/unlimited (a client an admin hasn't put on a plan yet) - only an
  *existing* subscription is enforced, and only on genuinely new assignment (re-assigning the
  same card to the same client is a no-op, not a re-check), so an already-over-limit legacy
  client isn't retroactively locked out of routine operations. `GET /public/packages` lists
  active plans only (sorted) with no auth - the one deliberately public admin-authored
  endpoint in the platform, since Phase 1 explicitly required pricing never be hardcoded in
  the frontend. `OrderService` generates order numbers as `ORD-YYYYMMDD-NNN` (per-day
  sequence), resolves each line's product to snapshot its *current* price into `OrderItem`
  (an order's total doesn't silently change if a product's price is edited later), and
  computes subtotal/total as `BigDecimal` throughout.
- **Frontend:** `/admin/packages`, `/admin/products`, `/admin/orders` (list + create/edit
  dialogs; order status and payment status are separately updatable via a dropdown menu,
  matching the two independent backend enums `OrderStatus`/`PaymentStatus`). Order creation
  uses a `useFieldArray` line-item form (client select + repeatable product/quantity rows) -
  the first use of that RHF pattern in this codebase. Subscription assignment lives inside
  each client's detail page (`/admin/clients/[uuid]`) rather than a standalone list page,
  consistent with it being a per-client 1:1 resource, not an independent collection; the
  sidebar's old placeholder "Subscriptions" entry was removed accordingly. `/client/subscription`
  shows the client's own plan, status and cards-used/limit (read-only - assignment is admin-
  only). Public `/pricing` fetches `GET /public/packages` live and renders nothing hardcoded;
  an empty-plans state points visitors at the contact section instead of showing fake pricing.
- **A real Zod+RHF typing issue, not just a lint nit**: the "blank input = unlimited" fields
  (`cardLimit` etc.) need `NaN` (from `valueAsNumber` on an empty number input) converted to
  `null` before hitting the backend, which `z.transform()` normally handles - but a
  transform makes the schema's input type and output type diverge, and `zodResolver`'s
  `Resolver<TFieldValues>` generic doesn't reconcile that with `useForm<TFieldValues>` when
  they're declared as the same type (real `tsc` errors, not something `any` papers over).
  Fixed by keeping the form schema type NaN/undefined-passthrough and doing the
  NaN-to-null conversion in a plain `toPackagePlanPayload()` helper called right before the
  API call, instead of inside the zod schema.

## Phase 7: Real analytics events + client dashboard charts (2026-08-10)

The dashboards' "No data yet" placeholders are now backed by a genuine event pipeline, not
a mock. Verified end-to-end: tapped/scanned with different User-Agents (iPhone, Android,
iPad, desktop Chrome), confirmed correct per-day bucketing and device breakdown via curl,
then confirmed the same numbers rendering correctly in the real chart UI (including
catching that a screenshot mid-animation looked broken and wasn't).

- **Backend:** `analytics_events` table (V9 migration; composite `(client_id, created_at)`
  index matches the documented dashboard range-query pattern). `AnalyticsService.recordEvent`
  is `@Async` (fire-and-forget from the redirect hot path - never delays the visitor's
  redirect, per the NFC_FLOW.md commitment) and parses User-Agent into
  device/browser/OS **family only** via a small dependency-free `UserAgentParser` - the raw
  header is never stored (privacy-friendly analytics per SECURITY.md). Wired into both
  `NfcCardService.resolveRedirectTarget` and `QrCodeService.resolveRedirectTarget`.
  `GET /client/analytics/summary?days=7|30` returns totals, a zero-filled daily series (so
  the chart never has gaps), device breakdown, and top-5 cards; an admin equivalent takes
  `clientUuid`.
- **Caught and fixed a real bug while building this**: `/t/[token]` and `/q/[token]`
  do a server-side `fetch()` from the Next.js route handler to the backend - which does
  **not** forward the incoming request's headers automatically. Without an explicit fix,
  every single tap/scan would have recorded the Next.js server's own User-Agent (wrong
  device/browser analytics for 100% of traffic) and, worse, shared one rate-limit bucket
  across every real visitor instead of one per visitor (since the backend's IP-based limiter
  would only ever see the Next.js server's IP). Fixed by explicitly forwarding
  `User-Agent`/`Referer`/`X-Forwarded-For` in both route handlers. This is exactly the kind
  of bug that's invisible in a curl-only test (curl talks to the backend directly) and only
  shows up testing the real frontend path - another argument for the "verify live, not just
  unit-level" approach used throughout this build.
- **Frontend:** `/client/analytics` - range toggle (7/30 days), KPI cards, a two-series
  activity line chart and a device-breakdown bar chart (Recharts), built per the `dataviz`
  skill: fixed hue-by-identity color assignment (device type always maps to the same color,
  never re-cycled), palette validated for CVD-safety before use, single y-axis (no dual-axis
  trap), legend for the 2-series chart, `linear` interpolation (not `monotone`) so a single
  day's spike isn't visually smoothed into a false gradual ramp-up.

## Phase 6: Restaurant/digital menu module + MENU destination (2026-08-10)

Same auto-create-on-first-access, draft/publish, and dynamic-destination pattern as
Profile and now Menu - the fourth destination type resolved through
`DestinationResolverService`. Verified end-to-end via curl and in the real browser: add
category -> add item -> publish -> public menu renders -> draft correctly 404s before
publish.

- **Backend:** `menus` / `menu_categories` / `menu_items` tables (V8 migration; one menu
  per client, same 1:1-with-auto-create pattern as `individual_profiles`/`company_profiles`).
  `MenuService` owns full category/item CRUD (`POST/PUT/DELETE` on both, scoped through the
  menu so a client can never touch another client's categories/items even by guessing a
  UUID), price stored as `BigDecimal`/`DECIMAL(10,2)` per the platform's money-handling rule.
  Wired `MENU` into `DestinationService.create` (any client type, unlike PROFILE/
  COMPANY_PROFILE which are type-specific) and `DestinationResolverService` (resolves to the
  client's own published menu URL at redirect time - same "never touch the physical card
  when content changes" property as Profile).
- **Frontend:** `/client/menu` - single-page editor: menu settings form, category cards each
  with inline item lists, add/edit/delete dialogs for both categories and items, availability
  and "featured" flags. `/menu/[slug]` - server-rendered, mobile-first public menu (grays out
  unavailable items instead of hiding them, per the spec). NFC assign form gained "Menu" and
  "Google Review" as destination options (the latter needed a review-location picker,
  fetched via the admin-side Google Reviews read endpoint added in Phase 5).

## Phase 5: QR codes + Google Review locations (2026-08-10)

QR scanning now mirrors the NFC tap pipeline exactly, and Google Review destinations are
real (previously rejected with "module not built"). Verified end-to-end: create a review
location -> create a QR code pointing at it -> scan the QR -> real redirect to the Google
Review URL, with scan counting - all confirmed via curl and in the real browser UI.

- **Backend:** `google_review_locations` and `qr_codes` tables (V7 migration), plus a
  `destinations.google_review_location_id` FK column - a deliberate typed-FK alternative to
  the original spec's generic `target_reference` field (see TECHNICAL_DECISIONS.md).
  Extracted **`DestinationResolverService`**: the PROFILE/COMPANY_PROFILE/GOOGLE_REVIEW/
  URL-based resolution branching that lived in `NfcCardService` now lives in one shared
  service, used by both the NFC and QR redirect paths - refactored now specifically because
  QR was about to duplicate it verbatim. `QrCodeService` reuses `NfcTokenService` for secure
  token generation/hashing (same show-once, hash-only pattern as NFC cards - the raw QR
  token/URL is returned exactly once, at creation). `GoogleReviewLocationService` is
  client-self-service (a client can have multiple locations, e.g. one per branch, matching
  the spec's CEYCOL Colombo/Kandy/Galle example) with a read-only admin list endpoint.
  `PublicQrRedirectController` (`GET /api/v1/public/q/{token}`) is rate-limited identically
  to the NFC endpoint.
- **Frontend:** `/client/qr-codes` (create dialog with a destination-type picker including
  "Google Review" wired to the client's own locations; real QR PNG rendering via the
  `qrcode` package, downloadable at creation time) and `/client/google-reviews` (location
  CRUD). `/q/[token]` passthrough route mirrors `/t/[token]`.
- **Caught and fixed a bug in my own code before shipping it:** the QR list view initially
  tried to re-render a scannable QR image for existing codes using the code's UUID - but the
  raw token is never stored (by design, same show-once pattern as NFC), so that QR would have
  looked real but silently not worked. Replaced with an honest static icon + a note that the
  image is only available at creation time, consistent with the "no fake buttons" principle
  applied throughout this build.

## Phase 3: Individual/Company Profile + public profile pages + vCard (2026-08-10)

Full profile module, backend + frontend, verified end-to-end - including wiring it into the
NFC redirect flow so `PROFILE`/`COMPANY_PROFILE` destinations (previously rejected as
"module not built") now resolve dynamically.

- **Backend:** `individual_profiles`, `company_profiles`, `social_links` tables (V6
  migration; `template_id` omitted - Template module doesn't exist yet). `ProfileService`
  auto-creates a draft profile (seeded from the client's display name/email/phone, with a
  unique auto-generated slug via `Slugify`) the first time a client hits `GET /client/profile`
  - one unified `ProfileResponse` DTO covers both individual and business shapes so the
    frontend renders one adaptive form instead of two. Draft vs published is enforced at the
    repository level (`findBySlugAndPublishedTrue`) so unpublished profiles are genuinely
    unreachable publicly, not just hidden in the UI. `VCardGenerator` produces a real
    `text/vcard` file (`GET /public/profile/{slug}/vcard`, `.../company/{slug}/vcard`).
  - **Redirect integration:** `DestinationService.create` now accepts `PROFILE`/
    `COMPANY_PROFILE` (validated against the client's actual type), storing no `external_url`
    - `NfcCardService.resolveRedirectTarget` resolves the target dynamically via
    `ProfileService.resolvePublicUrl` at tap time. This is the payoff of the destination
    abstraction: a client can keep editing/republishing their profile forever without ever
    touching the physical card or the destination record. An unpublished profile behind an
    active card correctly serves `410` (via `NfcCardInactiveException`), not a broken link.
- **Frontend:** `/client/profile` - desktop split-panel editor (form + sticky live phone
  preview, per the spec's profile-builder brief) that adapts fields based on client type,
  publish/unpublish toggle, and a social-links editor. `/p/[slug]` and `/company/[slug]` -
  public, mobile-first digital business cards (server-rendered for real `<title>`/meta per
  profile) with Call/WhatsApp/Email/Save-Contact actions and a working vCard download. NFC
  card assign form gained "Digital Profile"/"Company Profile" as destination options
  (filtered by the selected client's type). Client sidebar's separate "My Profile"/"My
  Company" entries were unified into one live "My Profile" link, matching the unified backend
  endpoint.
- **Verified:** auto-create on first access, slug uniqueness, draft profile 404s publicly,
  publish -> profile appears at `/public/profile/{slug}` and `/p/{slug}`, vCard downloads,
  assigning a card to a `PROFILE` destination and tapping it redirects to the live profile
  URL, unpublishing breaks the tap with `410` - all via curl and confirmed visually in Chrome
  (public card renders correctly, editor's live preview updates, "Published" badge/public URL
  link work).

## Phase 4 (core slice): NFC Card + Destination + secure redirect (2026-08-10)

The platform's core value proposition - tap a card, land on the right destination, change
the destination without touching the physical card - is live end-to-end, verified via curl
(status transitions, tenant isolation, rate limiting) and in a real browser.

- **Backend:** `destinations` + `nfc_cards` tables (V5 migration). `NfcTokenService`
  generates a 20-char `SecureRandom` base62 token and stores only
  `SHA-256(token + pepper)` - the raw token is returned exactly once, at registration, in
  `NfcCardRegisterResponse` (same "show-once" pattern as the client temp password).
  `DestinationUrlValidator` enforces http/https-only URLs (open-redirect protection).
  `PublicRedirectController` (`GET /api/v1/public/t/{token}`) is the redirect hot path:
  format check → hash lookup → card/client/destination status checks → tap counter update →
  real HTTP 302, rate-limited per-IP via the new `RateLimiterService` (Bucket4j,
  in-memory - see TECHNICAL_DECISIONS.md for the Redis-upgrade note). Admin endpoints:
  register/list/get/assign/activate/suspend NFC cards, create/list destinations. Client
  endpoints (`/api/v1/client/nfc-cards`) are tenant-safe (`findByUuidAndClientId`, never
  `findByUuid` + after-the-fact check) - manually verified: Client A gets `404` when
  requesting Client B's card UUID directly, exactly the critical test in SECURITY.md.
  Destination types are scoped: only URL-based ones (WEBSITE/WHATSAPP/SOCIAL/CUSTOM_URL) are
  implemented now; PROFILE/COMPANY_PROFILE/MENU/GOOGLE_REVIEW/VCARD are reserved enum values
  rejected with a clear error until their modules exist (Phase 3/5/6).
- **Frontend:** `/admin/nfc-cards` (list + "Register NFC Card" dialog showing the one-time
  secure URL), `/admin/nfc-cards/[uuid]` (assign-to-client-and-destination form, which
  activates the card in one step; Activate/Suspend actions), `/client/nfc-cards` (read-only,
  own cards). `/t/[token]` route handler forwards to the backend redirect endpoint
  server-side (so it can branch on the response instead of exposing raw backend JSON) and
  a `/card-unavailable` page explains inactive/rate-limited/not-found taps to the visitor.
  Client detail page and both dashboards now show real NFC card data (assigned cards, active
  count, tap totals) instead of placeholders. Admin sidebar's "NFC Cards" is live.
- **Verified:** register → create destination → assign → tap the secure URL → real 302 to
  the destination (confirmed both via curl and via the frontend's `/t/[token]` passthrough);
  suspending a card correctly serves the "card unavailable" page instead of redirecting;
  invalid/unknown tokens return 404 without leaking which case it was; tenant isolation
  confirmed with two separate client logins.

## Phase 2: Client Management + colorized UI (2026-08-10)

Full Client Management module, backend + frontend, verified end-to-end (curl and real
browser clicks via Chrome automation - create, list/search/filter, view detail, edit, and
activate/suspend all confirmed working against the live local stack):

- **Backend:** `clients` table (V4 migration), `Client` entity/repository/service,
  `AdminClientController` (`GET/POST /api/v1/admin/clients`, `GET/PUT /{uuid}`,
  `PATCH /{uuid}/status`), permission-gated (`CLIENT_VIEW/CREATE/UPDATE/SUSPEND`) via the new
  `PrincipalFactory` + `PermissionCodes` mechanism (see TECHNICAL_DECISIONS.md). Creating a
  client atomically creates a `CLIENT`-role `User` account with a generated temporary
  password (shown once in the response + emailed), audit-logs `CLIENT_CREATE`/`CLIENT_UPDATE`/
  `CLIENT_STATUS_CHANGE`, and rejects duplicate emails with `409 CONFLICT`.
- **Frontend:** `/admin/clients` (searchable, filterable, paginated table + "Create Client"
  dialog showing the one-time temporary password) and `/admin/clients/[uuid]` (edit form +
  Activate/Suspend action). Added shadcn-style `Dialog`, `Select`, `Table` primitives. Admin
  sidebar's "Clients" link is now live (no longer "Soon"); dashboard's "Total Clients" KPI
  shows a real live count.
- **Colors:** Replaced the pure black/white palette with an indigo brand accent + semantic
  success/warning/info tokens (see TECHNICAL_DECISIONS.md "Color system") - visible on
  buttons, active nav state, and status badges (ACTIVE=green, SUSPENDED=red, PENDING=amber).

## Branding: CeyloNfc (2026-08-10)

Product/brand name is **CeyloNfc** (domain: `ceylonfc.com`). `nfc-platform` remains the
internal/technical repo and Java package codename only. All user-facing brand strings are
centralized in `frontend/src/lib/config/brand.ts` (`name`, `domain`, `supportEmail`,
`tagline`) - components import from there rather than hardcoding text, so future rebrands or
a move to the backend-driven Settings module (Phase 9) touch one file. Also updated:
backend `MAIL_FROM` default and Swagger/OpenAPI title (`CeyloNfc API`), health check message,
`frontend/.env.example` (`NEXT_PUBLIC_SITE_URL=https://ceylonfc.com`), `backend/.env.example`
production URL comments. Verified visually in Chrome - header, tab title, and hero all show
"CeyloNfc" correctly, `npm run lint`/`typecheck` clean.

## Verified with a full local run (2026-08-10)

Ran the actual stack end-to-end (Docker MySQL + Redis, `mvnw spring-boot:run`, `npm run dev`)
and drove the real login flow through Chrome. This surfaced and fixed a genuine bug that
`mvn test`/`mvn package` alone did not catch:

- **Schema/entity type mismatch**: Flyway defined `uuid`/`*_hash` columns as `CHAR(n)`, but
  Hibernate's default mapping for a plain `String` field is `VARCHAR`, so schema validation
  failed on startup (`ddl-auto=validate`). Fixed by changing the affected columns in
  `V1__create_auth_tables.sql` / `V2__create_audit_log.sql` to `VARCHAR(n)` to match the
  entity mappings.
- **`LazyInitializationException` on every authenticated request after login**:
  `JwtAuthenticationFilter` built `UserPrincipal` (which reads `role.getPermissions()`) from a
  `User` loaded outside any transaction, so `Role.permissions` (LAZY `@ManyToMany`) blew up
  reading it — masked by Spring Security as an empty `403` rather than a visible stack trace
  in the response. Login itself worked (it runs inside `AuthService`'s `@Transactional`
  method) but `/auth/me` and every other protected endpoint failed right after. Fixed by
  adding fetch-joined repository queries (`findByUuidWithRolesAndPermissions`,
  `findByEmailWithRolesAndPermissions`) and using them in `JwtAuthenticationFilter` and
  `CustomUserDetailsService` instead of the plain finders.

After both fixes: full login -> `/auth/me` -> logout verified both via `curl` (cookie jar,
checking `Set-Cookie`, CSRF header, 403-after-logout) and through the real browser UI
(Chrome via claude-in-chrome) — sign-in redirects to the role-appropriate dashboard, the
account dropdown shows the correct email/role, and logout correctly redirects back to
`/login`. This is a good example of why `docs/PROJECT_PROGRESS.md` distinguishes "tests
pass" from "verified locally" - the Testcontainers test written for this exact flow
(`AuthFlowIntegrationTest`) would have caught the lazy-loading bug too, but couldn't run in
this environment (see Known Issues).

### How to run it yourself

```bash
docker compose up -d mysql redis
cd backend
SUPER_ADMIN_EMAIL=admin@nfc-platform.local SUPER_ADMIN_PASSWORD='DevAdmin!2026' \
  DB_HOST=localhost DB_PORT=3307 DB_NAME=nfc_platform DB_USERNAME=nfc_app DB_PASSWORD=nfc_app_password \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# in another terminal
cd frontend && npm run dev
```

Backend: http://localhost:8080 (health at `/api/v1/health`, Swagger at `/swagger-ui.html`).
Frontend: http://localhost:3000. Dev Super Admin: `admin@nfc-platform.local` /
`DevAdmin!2026` (seeded once on first startup via the env vars above - local dev only, never
set those env vars this way in production).

## Completed (Phase 1 - foundation)

- [x] Repository structure (`backend/ frontend/ docker/ docs/`)
- [x] Architecture docs: ARCHITECTURE, DATABASE, SECURITY, NFC_FLOW, API, DEPLOYMENT,
      ROLE_PERMISSION_MATRIX, TECHNICAL_DECISIONS, PROJECT_PROGRESS
- [x] Docker Compose (mysql, redis, backend, frontend; healthchecks; named volumes)
- [x] Spring Boot backend scaffold (Maven + wrapper, Java 17, Spring Boot 3.3.5, all
      requested dependencies wired in `pom.xml`, `dev`/`prod`/`docker` profiles)
- [x] Flyway migrations V1-V3: users/roles/permissions/user_roles/role_permissions,
      admin_permission_overrides, refresh_tokens, password_reset_tokens, audit_logs,
      seeded role + permission catalog
- [x] Spring Security JWT cookie auth + RBAC end-to-end: `User/Role/Permission` entities,
      `UserPrincipal`, `JwtService` (HS256, minimal claims), `JwtAuthenticationFilter`,
      `CookieUtil` (HttpOnly/Secure/SameSite=Lax), CSRF (cookie + header, login/refresh
      exempted per SECURITY.md), CORS locked to `APP_FRONTEND_URL`, security headers,
      `AuthController` (login/logout/refresh/forgot-password/reset-password/change-password/me),
      account lockout after N failed attempts, refresh-token rotation with reuse detection,
      append-only `AuditService`, global exception handler with typed `ApiException`s,
      env-var-driven Super Admin seed (`DataSeedRunner`, no fixed default credential)
- [x] Backend tests: `JwtServiceTest` (unit, passing), `AuthFlowIntegrationTest`
      (Testcontainers MySQL end-to-end login/me/logout + invalid-credentials + unauthenticated
      cases - written and correct, but could not be executed in this session because this
      machine's Docker Desktop build has a client/engine protocol mismatch with the Java
      Docker client Testcontainers uses over the Windows named pipe; `docker info`/`docker ps`
      from the CLI work fine, so this is specific to the Java npipe transport, not a real
      Docker outage). `mvn compile`, `mvn test` (JwtServiceTest), and `mvn package` all verified
      green.
- [x] Next.js frontend scaffold: Next.js 16.3.0 (App Router, Turbopack), React 19.2.8,
      TypeScript strict, Tailwind CSS v4, TanStack Query, React Hook Form + Zod, Recharts
      installed, Lucide icons, `output: "standalone"` for Docker
- [x] Design system: hand-built shadcn/ui-style primitives (Button, Input, Label, Card,
      Badge, Separator, Skeleton, Avatar, DropdownMenu, Sonner toaster) on Radix primitives,
      premium black/white/gray token palette in `globals.css` (light + dark), Geist font
      (shadcn's own CLI hung non-interactively in this environment - documented, not blocking)
- [x] Centralized API layer: `lib/api/client.ts` (credentialed fetch, CSRF header injection,
      one-shot 401->refresh->retry, typed `ApiClientError`), `lib/api/auth.ts`,
      `lib/providers/query-provider.tsx`, `lib/providers/auth-provider.tsx` (`/auth/me`-backed
      session state - UX only, backend remains sole authorization authority)
- [x] Public landing page (hero, products, how-it-works, benefits, contact - no fake lead
      form since `POST /api/v1/public/leads` doesn't exist yet; a real `mailto:` CTA is used
      instead), `/privacy` and `/terms` stub pages, `/login`, `/forgot-password`,
      `/reset-password` (fully wired to the real backend endpoints)
- [x] Admin layout + dashboard shell, Client layout + dashboard shell: role-guarded
      (`RoleGuard`), full sidebar IA from the spec with only implemented routes clickable -
      everything else renders as a disabled "Soon" item rather than a dead link; dashboard
      KPI cards show an honest empty state, no fabricated numbers
- [x] 404 (`not-found.tsx`) and 500 (`error.tsx`) pages, `sitemap.ts`, `robots.ts`
      (disallows `/admin`, `/client`)
- [x] GitHub Actions CI (`.github/workflows/ci.yml`): backend `mvn test` + `mvn package`,
      frontend `npm run lint` + `npm run typecheck` + `npm run build`
- [x] Build verification: backend `mvn compile/test/package` green; frontend
      `npm run lint` (0 errors), `npm run typecheck` (0 errors), `npm run build` (all 13
      routes compiled/prerendered); manually smoke-tested with `npm run dev` - `/`, `/login`,
      `/privacy` return 200, unknown routes return 404, `/admin/dashboard` correctly
      client-redirects an unauthenticated visitor to `/login`

## Pending (future phases, not started)

- Phase 2 (remainder): none - RBAC admin UI, user management screen, and client delete all
  shipped post-roadmap (see "Post-roadmap: RBAC admin UI, user management, client delete"
  above). Phase 2 is fully complete.
- Phase 3 (remainder): none - the template system (`template_id` on profiles, template gallery,
  premium/free flag) shipped post-roadmap (see "Post-roadmap: template system" above). Image
  upload for profile/cover/logo and admin-side profile editing both shipped post-roadmap too
  (see their "Post-roadmap" entries above). Phase 3 is fully complete.
- Phase 4 (remainder): none - Write NFC page, card replacement workflow, and the searchable
  client combobox all shipped post-roadmap (see their "Post-roadmap" entries above). Phase 4 is
  fully complete.
- Phase 5 (remainder): none - admin CRUD for QR codes and review locations already had full
  create/edit/status parity with client self-service (this doc's earlier "read-only" framing
  was stale), and QR customization (logo/colors/error-correction/SVG export) shipped
  post-roadmap (see "Post-roadmap: QR code customization" above). Phase 5 is fully complete.
- Phase 6 (remainder): none - drag-to-reorder categories/items and item image upload both
  shipped post-roadmap (see their "Post-roadmap" entries above). Phase 6 is fully complete.
- Phase 7 (remainder): none - admin analytics UI and PROFILE_VIEW/MENU_VIEW event tracking both
  shipped post-roadmap (see their "Post-roadmap" entries above). Phase 7 is fully complete.
- Phase 8 (remainder): payment gateway integration (orders track `paymentStatus` manually
  today - no real payment processor is wired in). Admin standalone subscriptions page and order
  detail/print view both shipped post-roadmap (see their "Post-roadmap" entries above).
  Client-facing order history page was already shipped despite
  being listed here as a remainder - correction below (2026-08-14).
- Phase 9 (remainder): in-app notifications only (30s polling, no WebSocket/SSE push, no
  email delivery for new-lead/new-ticket alerts - rate limiting on `POST /public/leads` and
  support ticket creation was added in Phase 10). "Convert to client" and ticket file
  attachments both shipped post-roadmap (see their "Post-roadmap" entries above). The email/push
  notification gap needs real infrastructure (SMTP or a push provider) this dev environment
  doesn't have - the rest of Phase 9 is complete.
- Phase 10 (remainder): **full per-module JUnit/Mockito coverage - done.** Every backend service
  now has a dedicated test class (auth, QR, lead conversion, client lifecycle, admin
  staff/permissions, menu, NFC, profile, subscription, template, support, order, destination,
  Google Review, analytics, notification, package plan, product) plus the security-relevant
  pure-utility classes (file-upload magic-byte validation, the open-redirect URL validator,
  vCard generation, User-Agent parsing - the last of which turned up a real live bug, see
  "Post-roadmap: remaining service + utility unit test coverage" above). `AuditLogQueryService`
  and the storage services (`LocalStorageService`/`S3StorageService`) remain the only backend
  classes without dedicated unit tests - both are thin/already covered by live verification
  (S3 storage's own post-roadmap entry above) rather than meaningfully undertested. Still open:
  more Playwright journeys beyond the 11 now covered (see "Post-roadmap: Playwright
  journeys - order creation, package/product CRUD, QR code creation" above - login, client
  creation, NFC card ownership isolation, support tickets, and the public pricing/lead form
  were already covered); a verified-buildable frontend Docker image (blocked on this machine by
  sandbox network flakiness, not a code issue - see "Known Issues").
  Both halves of the Go-live checklist's "required before running more than one backend
  instance" item are now shipped post-roadmap: the Redis-backed rate limiter (see "Post-roadmap:
  Redis-backed rate limiter" above) and S3-compatible storage (see "Post-roadmap: S3-compatible
  storage" above) - `LocalStorageService` remains the dev/single-instance default.

## Known Issues / Constraints

- Java 21 requested but unavailable in this environment; running Java 17 (see
  TECHNICAL_DECISIONS.md). No functional impact, revisit when JDK 21 is provisioned.
- `AuthFlowIntegrationTest` and `TenantIsolationTest` (both Testcontainers-based) could not be
  executed in this session due to a Docker Desktop <-> Testcontainers named-pipe protocol
  mismatch on this machine (confirmed still present as of Phase 10 - `docker` CLI commands
  work fine directly, only Testcontainers' Java client fails against both npipe endpoints).
  Both tests are written, test-compile cleanly, and fail at exactly the Docker-client-init
  step (not a logic error) when run locally - they execute normally in CI
  (`.github/workflows/ci.yml` runs on `ubuntu-latest`, a real Linux Docker socket). The other
  29 backend tests (plain JUnit/Mockito, no Testcontainers) run and pass locally.
- The frontend production Docker image (`docker compose build frontend`) failed 4 consecutive
  times in this sandbox, each after 15-25 minutes, with `npm ci` hitting an ECONNRESET
  ("network aborted") partway through installing packages - even after hardening npm's retry
  config in the `Dockerfile`. The backend image builds and boots cleanly (verified: Flyway
  validated all 11 migrations, health check returns 200). A real bug was found and fixed along
  the way - `package-lock.json`, generated on this machine's Node 24/npm 11, was incompatible
  with the Node 20/npm 10 pinned in `frontend/Dockerfile` and CI, so `npm ci` failed
  deterministically with a "lock file out of sync" error before any network attempt; the lock
  file was regenerated inside a `node:20-alpine` container to match, and the failure mode
  changed to the network reset described above, confirming the fix. Two new `.dockerignore`
  files (backend, frontend) and a root `.env.example` were also added as part of this
  run-through - see the Phase 10 entry above and `docs/DEPLOYMENT.md`'s "Go-live checklist".
  Recommended next step: build the frontend image in CI rather than this local sandbox.
- shadcn/ui's own CLI (`npx shadcn@latest init`) hung when run non-interactively in this
  shell even with `-y`/preset flags; the design system was hand-built instead using the same
  Radix primitives and Tailwind v4 token conventions shadcn generates, so it is drop-in
  compatible if the CLI is used later to add more components.
