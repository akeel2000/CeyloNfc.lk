# nfc-platform (CeyloNfc)

`nfc-platform` is the internal/technical codename for this repository; **CeyloNfc** is the
product/brand name shown to users (landing page, dashboards, emails). Brand values live in one
place: `frontend/src/lib/config/brand.ts`.

A commercial SaaS platform for selling and managing NFC digital business cards, Google Review
NFC cards, QR codes, digital profiles and menus. A physical NFC card is registered once and
receives a permanent secure URL (`/t/{token}`); its destination (profile, review page, menu,
website, ...) can be changed at any time without rewriting the chip.

## Tech stack

- **Backend:** Java 17 (see `docs/TECHNICAL_DECISIONS.md` — Java 21 was requested but is
  unavailable in this environment), Spring Boot, Spring Security, Spring Data JPA, MySQL 8,
  Flyway, JWT (HttpOnly cookie strategy), MapStruct, Lombok, OpenAPI/Swagger, JUnit 5,
  Mockito, Testcontainers.
- **Frontend:** Next.js (App Router), React, TypeScript, Tailwind CSS, shadcn/ui,
  React Hook Form, Zod, TanStack Query, Recharts, Lucide Icons.
- **Infra:** Docker, Docker Compose, Nginx-ready reverse proxy, MySQL, Redis (optional),
  GitHub Actions CI.

## Repository layout

```
nfc-platform/
├── backend/        Spring Boot REST API
├── frontend/        Next.js application
├── docker/           supporting docker assets (nginx config, init scripts)
├── docs/              architecture & process documentation
├── docker-compose.yml
└── README.md
```

See `docs/ARCHITECTURE.md` for the full module breakdown, `docs/PROJECT_PROGRESS.md` for what
is implemented so far, and `docs/TECHNICAL_DECISIONS.md` for the reasoning behind key choices.

## Prerequisites

- JDK 17+, Maven (or use the `./mvnw` wrapper)
- Node.js 20+, npm
- MySQL 8 (via Docker Compose, or a local install)
- Docker + Docker Compose (optional but recommended)

## Local development

### 1. Start MySQL (and Redis)

```bash
docker compose up -d mysql redis
```

### 2. Backend

```bash
cd backend
cp .env.example .env   # fill in local values
./mvnw spring-boot:run  # runs with the `dev` profile, http://localhost:8080
```

Swagger UI: `http://localhost:8080/swagger-ui.html` (dev profile only).

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev   # http://localhost:3000
```

### 4. Everything via Docker

```bash
docker compose up --build
```

## Environment variables

See `docs/DEPLOYMENT.md` and each app's `.env.example` for the full list. Never commit real
secrets — `.env*` files are gitignored.

## Database migrations & seed data

Flyway migrations run automatically on backend startup (`backend/src/main/resources/db/migration`).
Development seed data (Super Admin, sample clients/cards) is added as later phases land — see
`docs/PROJECT_PROGRESS.md`.

## Testing

```bash
cd backend && ./mvnw test          # JUnit 5 / Mockito / Testcontainers
cd frontend && npm run lint && npm run typecheck && npm run build
```

## Security

Tenant isolation, RBAC, NFC token hashing, JWT cookie strategy, and the full security
checklist are documented in `docs/SECURITY.md`. The redirect flow (the highest-risk,
highest-traffic path) is documented in `docs/NFC_FLOW.md`.

## Deployment

See `docs/DEPLOYMENT.md` for production topology, Nginx routing, and CI.

## Status

This is an active build. `docs/PROJECT_PROGRESS.md` is the single source of truth for what is
done vs. pending — check it before assuming a feature exists.
