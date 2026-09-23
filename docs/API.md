# API Design

Base path: `/api/v1`. JSON only. Auth via HttpOnly cookies (no `Authorization` header from
the browser app; server-to-server callers, if ever added, would use a separate API-key path).

## Response envelope

Success:
```json
{ "success": true, "data": {}, "message": "Success", "timestamp": "2026-08-09T12:00:00Z" }
```

Error:
```json
{ "success": false, "error": { "code": "CLIENT_NOT_FOUND", "message": "Client was not found" }, "timestamp": "..." }
```

Paginated data uses Spring's `Page` shape inside `data`: `content, totalElements, totalPages,
number, size`. List endpoints accept `?page=0&size=20&sort=createdAt,desc`.

## Phase 1 endpoints (implemented)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
POST   /api/v1/auth/refresh
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password
POST   /api/v1/auth/change-password
GET    /api/v1/auth/me

GET    /api/v1/health
```

## Planned endpoints (later phases, documented now for stability)

```
# Admin — clients
GET    /api/v1/admin/clients
POST   /api/v1/admin/clients
GET    /api/v1/admin/clients/{uuid}
PUT    /api/v1/admin/clients/{uuid}
PATCH  /api/v1/admin/clients/{uuid}/status

# Admin — NFC
GET    /api/v1/admin/nfc-cards
POST   /api/v1/admin/nfc-cards
POST   /api/v1/admin/nfc-cards/{uuid}/assign
POST   /api/v1/admin/nfc-cards/{uuid}/activate
POST   /api/v1/admin/nfc-cards/{uuid}/suspend

# Admin — Templates
GET    /api/v1/admin/templates
POST   /api/v1/admin/templates
PUT    /api/v1/admin/templates/{uuid}

# Admin — Menu (on behalf of a client, mirrors /client/menu below)
GET    /api/v1/admin/menu?clientUuid={uuid}
PUT    /api/v1/admin/menu?clientUuid={uuid}
POST   /api/v1/admin/menu/publish?clientUuid={uuid}
POST   /api/v1/admin/menu/categories?clientUuid={uuid}
PUT    /api/v1/admin/menu/categories/{categoryUuid}?clientUuid={uuid}
DELETE /api/v1/admin/menu/categories/{categoryUuid}?clientUuid={uuid}
POST   /api/v1/admin/menu/categories/{categoryUuid}/items?clientUuid={uuid}
PUT    /api/v1/admin/menu/categories/{categoryUuid}/items/{itemUuid}?clientUuid={uuid}
DELETE /api/v1/admin/menu/categories/{categoryUuid}/items/{itemUuid}?clientUuid={uuid}

# Admin — Settings
GET    /api/v1/admin/settings
PUT    /api/v1/admin/settings

# Client
GET    /api/v1/client/nfc-cards
GET    /api/v1/client/nfc-cards/{uuid}
GET    /api/v1/client/profile
PUT    /api/v1/client/profile
POST   /api/v1/client/profile/publish
GET    /api/v1/client/templates
POST   /api/v1/client/profile/template/{templateUuid}

# Public
GET    /api/v1/public/settings
GET    /api/v1/public/profile/{slug}
GET    /api/v1/public/company/{slug}
GET    /api/v1/public/menu/{slug}
GET    /api/v1/public/t/{token}
GET    /api/v1/public/q/{token}
GET    /api/v1/public/packages
POST   /api/v1/public/leads
```

## Error codes (growing registry)

`VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN, RESOURCE_NOT_FOUND, CONFLICT, RATE_LIMITED,
INVALID_CREDENTIALS, ACCOUNT_LOCKED, TOKEN_EXPIRED, CLIENT_NOT_FOUND, NFC_CARD_INACTIVE,
SUBSCRIPTION_EXPIRED`

## HTTP status usage

`200` reads/updates, `201` creates, `204` deletes with no body, `400` malformed request,
`401` not authenticated, `403` authenticated but not authorized / wrong tenant, `404` not
found, `409` conflict (e.g. duplicate slug), `422` semantically invalid, `429` rate limited,
`500` unexpected server error (never leaks stack traces to the client).

OpenAPI/Swagger UI is served at `/swagger-ui.html` in the `dev` profile only; disabled
(or protected) in `prod`.
