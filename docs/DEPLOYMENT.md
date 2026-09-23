# Deployment

## Local development

```
docker compose up -d mysql redis     # start dependencies only
cd backend && ./mvnw spring-boot:run  # runs against localhost:3307 mysql (dev profile)
cd frontend && npm install && npm run dev
```

Or run everything in containers: `docker compose up --build`.

## Environment variables

Backend (`backend/.env.example`): `DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD,
JWT_SECRET, JWT_ACCESS_EXPIRATION, JWT_REFRESH_EXPIRATION, NFC_TOKEN_PEPPER,
APP_FRONTEND_URL, APP_BACKEND_URL, STORAGE_TYPE, STORAGE_PATH, STORAGE_S3_BUCKET,
STORAGE_S3_REGION, STORAGE_S3_ENDPOINT, STORAGE_S3_ACCESS_KEY, STORAGE_S3_SECRET_KEY,
STORAGE_S3_PUBLIC_URL, MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM,
REDIS_HOST, REDIS_PORT`. The `STORAGE_S3_*` vars are only read when `STORAGE_TYPE=s3`.

Frontend (`frontend/.env.example`): `NEXT_PUBLIC_API_URL`. Never put backend secrets in a
`NEXT_PUBLIC_*` variable — anything with that prefix ships to the browser bundle.

## Production topology

```
domain.com        -> Next.js (Vercel or Docker/Nginx)
api.domain.com     -> Spring Boot (Docker on a VPS / Railway / AWS / DigitalOcean)
MySQL              -> managed MySQL 8 instance (or private MySQL container with a volume)
Media storage       -> S3-compatible bucket (StorageService abstraction, LocalStorageService only for dev)
```

Nginx (or the platform's edge) terminates TLS and proxies `api.domain.com` to the Spring Boot
container; `domain.com` serves the Next.js app directly or via `/api` rewrites to the backend.

## Database migrations in production

Flyway runs automatically on backend startup (`spring.flyway.enabled=true` in all profiles).
Never run destructive hand-edits against production schema — every change is a new
`V{n}__description.sql` file, reviewed like any other code change.

## Health checks

`GET /api/v1/health` (unauthenticated) is used by Docker Compose `healthcheck:` blocks and by
the load balancer/Nginx upstream check.

## Backups

MySQL: nightly `mysqldump` (or managed-provider automated snapshots) retained per the
provider's policy; media bucket versioning enabled at the storage-provider level. Document
the exact restore procedure per environment once a managed provider is chosen.

## CI

GitHub Actions (`.github/workflows/ci.yml`): backend `mvn test && mvn package`, frontend
`npm run lint && npm run typecheck && npm run build`. Both must pass before merge; deployment
is a separate, manual/gated step, not triggered by CI itself in this phase. The backend job
runs on `ubuntu-latest`, so `AuthFlowIntegrationTest` and `TenantIsolationTest` (both
Testcontainers-based) execute there even though they can't run on this project's Windows dev
machine - see "Known Issues / Constraints" in `PROJECT_PROGRESS.md`.

## Go-live checklist

Verified via a full production-deployment run-through (Phase 10). Copy `.env.example` to
`.env` at the repo root before running any of this - every value below has a recognizable
dev-only fallback baked into `docker-compose.yml`, none of them safe to deploy as-is.

- [ ] **Rotate every secret** off its dev default: `JWT_SECRET`, `NFC_TOKEN_PEPPER`,
      `DB_PASSWORD`, `DB_ROOT_PASSWORD` - each a random, unique value per environment, never
      reused across dev/staging/prod.
- [ ] **Run the backend with `SPRING_PROFILES_ACTIVE=prod`**, not `docker` - `prod` is the
      profile that sets `cookie-secure: true` and disables Swagger/OpenAPI
      (`application-prod.yml`); `docker` is for local container testing only and deliberately
      leaves cookies non-secure since local Compose has no TLS.
- [ ] **Put a TLS-terminating proxy in front of the backend** (Nginx or the platform's edge -
      see "Production topology" above). `Strict-Transport-Security` is added automatically by
      Spring Security's default header writer only on HTTPS requests, so it silently does
      nothing until this is in place - it is not something `prod` profile "turns on".
- [ ] **Point `APP_FRONTEND_URL`/`APP_BACKEND_URL`/`NEXT_PUBLIC_API_URL` at the real domains.**
      `APP_FRONTEND_URL` in particular drives the CORS allow-list
      (`SecurityConfig.corsConfigurationSource`) - a stale value here means the deployed
      frontend can't call the API at all, not a silent bug.
- [ ] **Confirm Flyway migrates cleanly against a fresh database** before pointing real
      traffic at it (`mvn spring-boot:run` or the container's own boot log should show
      "Successfully validated N migrations" with no errors - verified for all 11 migrations
      as of Phase 10).
- [ ] **Leave `SUPER_ADMIN_EMAIL`/`SUPER_ADMIN_PASSWORD` unset** after the very first boot that
      seeds the initial account - they exist only for that one-time bootstrap
      (`DataSeedRunner`), never as a standing admin-creation path.
- [x] **Media storage can now be moved off local disk to an S3-compatible bucket**
      (`STORAGE_TYPE=s3`, shipped post-roadmap - see `PROJECT_PROGRESS.md`) - `S3StorageService`
      is a drop-in `StorageService` swap (`@ConditionalOnProperty`) alongside the original
      `LocalStorageService`, which still writes to the container's own filesystem and remains
      the default. Set `STORAGE_S3_BUCKET`/`STORAGE_S3_REGION` plus `STORAGE_S3_ACCESS_KEY`/
      `STORAGE_S3_SECRET_KEY` (or leave the keys unset to use the AWS SDK's default credential
      chain / IAM role on real AWS); `STORAGE_S3_ENDPOINT` + `STORAGE_S3_PUBLIC_URL` are only
      needed for non-AWS providers (MinIO, R2, Spaces...). Required before running more than one
      backend instance, since local disk doesn't survive a redeploy or replicate across
      instances. Verified live end-to-end against a real MinIO bucket (upload -> stored object
      -> publicly fetchable URL).
- [x] **Rate limiter is now Redis-backed** (`RateLimiterService`, shipped post-roadmap - see
      `PROJECT_PROGRESS.md`) - a shared fixed-window counter across however many backend
      instances are running, matching the platform-wide limit described in
      `docs/SECURITY.md`. Falls back to the original in-memory limiter only if Redis itself is
      unreachable (fast-failing, ~500ms, not the 60s Lettuce default - verified live). Requires
      `REDIS_HOST`/`REDIS_PORT` to point at a real, reachable Redis in production, same as any
      other environment.
- [ ] **Build the frontend Docker image in CI, not a constrained local sandbox** - the
      `Dockerfile` and `package-lock.json` are correct and the backend image builds cleanly,
      but frontend image builds in this project's local dev sandbox have hit repeated
      `npm ci` network resets after 15-25 minutes even after hardening npm's retry config (see
      "Known Issues / Constraints" in `PROJECT_PROGRESS.md`). CI's `ubuntu-latest` runners
      already build the frontend successfully today via plain `npm run build`; adding a
      `docker build` step there is the recommended way to get a verified image rather than
      retrying locally.
