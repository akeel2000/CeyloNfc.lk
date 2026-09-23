# Architecture

## Overview

nfc-platform is a multi-tenant SaaS system for selling and managing NFC business cards,
Google Review cards, QR codes, digital profiles and menus.

```
Browser / Mobile
      |
      v
Next.js (App Router) ---- UI, routing, forms, SSR/CSR, public rendering
      |  REST (JSON, credentialed fetch)
      v
Spring Boot REST API  ---- auth, business rules, validation, RBAC, tenant isolation
      |  Spring Data JPA / Hibernate
      v
MySQL 8  (+ Redis-ready cache/rate-limit layer, S3-compatible media storage)
```

Next.js **never** talks to MySQL directly. All business logic, authorization and
persistence lives in the Spring Boot service. The frontend is a pure API consumer.

## Backend module layout (package-by-feature)

`com.nfcplatform.<module>` where module ∈:

```
common/        shared response envelope, pagination, base entities, exceptions
config/        Spring config beans (CORS, OpenAPI, Jackson, async, scheduling)
security/      JWT, filters, SecurityConfig, permission evaluator, CSRF
auth/          login/logout/refresh/password endpoints
user/          User entity/service, account lifecycle
role/          Role entity/service
permission/    Permission entity, RBAC evaluation
client/        Client (tenant) entity/service, ownership enforcement
profile/       IndividualProfile, CompanyProfile, SocialLink
nfc/           NfcCard, secure token lifecycle, redirect flow
destination/   Destination abstraction (profile/company/review/menu/url/...)
qr/            QrCode entity + redirect flow
review/        GoogleReviewLocation
menu/          Menu, MenuCategory, MenuItem
template/      Template gallery
analytics/     AnalyticsEvent ingestion + aggregation
subscription/  Subscription lifecycle
packageplan/   PackagePlan (pricing/limits)
order/         Order, OrderItem
product/       Physical NFC product catalog
lead/          Public sales leads
support/       SupportTicket, SupportMessage
notification/  Notification
media/         StorageService abstraction (local/S3-compatible)
audit/         AuditLog (append-only)
settings/      Platform branding/settings
```

Each feature module contains `controller/ service/ repository/ entity/ dto/ mapper/`
as applicable. Controllers are thin: validate → delegate to service → map DTO.
Services own transactions, ownership checks and business rules. Repositories are
Spring Data JPA interfaces only.

## Request flow example — NFC redirect

```
GET /t/{token}  (Next.js catches this route)
   -> proxies/redirects to Spring Boot GET /api/v1/public/t/{token}
       -> NfcRedirectService
            1. validate token format
            2. SHA-256(token + pepper) -> lookup NfcCard by token_hash
            3. verify card status == ACTIVE
            4. verify owning client status == ACTIVE
            5. verify subscription entitlement (if applicable)
            6. resolve Destination -> target URL
            7. emit AnalyticsEvent asynchronously (does not block redirect)
            8. return 302 to a validated, allow-listed URL shape
```

No full Client/Profile aggregate is loaded during the redirect — only the minimal
projection needed to resolve the destination, to keep the hot path fast.

## Frontend layout (Next.js App Router)

```
src/
  app/
    (public)/        landing site, /t/[token], /q/[token], /p/[slug], /menu/[slug]
    (auth)/           /login
    admin/            admin dashboard (role-guarded layout)
    client/           client dashboard (role-guarded layout)
  components/         shared UI primitives (shadcn/ui based)
  features/           feature-scoped components (auth, clients, nfc, qr, ...)
  lib/
    api/              typed API client, one module per backend resource
    hooks/            TanStack Query hooks
    types/             TS types mirroring backend DTOs
    schemas/           Zod schemas mirroring Bean Validation rules
    providers/          QueryClientProvider, AuthProvider
    config/             env, constants
```

Frontend route guards are UX only. The backend is the sole authority for
authorization — every protected endpoint re-validates the caller's identity,
role and tenant ownership independently of what the UI shows.

## Deployment topology

```
                    ┌────────────┐
 domain.com   ───▶  │  Next.js   │
                    └────────────┘
                          │  /api/* proxy (Nginx or Next rewrites)
                          ▼
 api.domain.com ───▶ ┌────────────┐      ┌─────────┐
                     │ Spring Boot│─────▶│  MySQL  │
                     └────────────┘      └─────────┘
                          │
                          ▼
                     ┌─────────┐
                     │  Redis  │ (rate limiting / cache, optional in dev)
                     └─────────┘
```

See `DEPLOYMENT.md` for Docker Compose / production details.
