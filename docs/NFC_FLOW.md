# NFC & QR Redirect Flow

## Token lifecycle

1. Admin registers a physical card (`POST /api/v1/admin/nfc-cards`) → backend generates a
   `SecureRandom` token, stores only its salted hash, returns the raw token **once**.
2. Admin assigns the card to a client and a destination.
3. Staff writes `https://domain.com/t/{token}` to the physical NFC chip (Write NFC page).
4. The physical chip never changes again — destination changes happen server-side only.

## Tap flow

```
Customer taps card
  -> phone opens https://domain.com/t/{token}
  -> Next.js route /t/[token] forwards to Spring Boot GET /api/v1/public/t/{token}
  -> validate token charset/length (cheap, no DB hit for junk input)
  -> hash token with server pepper, look up NfcCard by token_hash (indexed)
  -> 404 if not found
  -> verify card.status == ACTIVE (else render inactive/expired page)
  -> verify owning client.status == ACTIVE
  -> verify subscription entitlement if the destination type requires it
  -> resolve Destination -> target URL (validated scheme/host)
  -> fire-and-forget AnalyticsEvent(NFC_TAP) - async, does not block response
  -> HTTP 302 to destination
```

## QR flow

Identical pipeline via `/q/{token}` and `AnalyticsEvent(QR_SCAN)`. QR codes always encode
the platform's own redirect URL, never the raw external destination — otherwise scans could
not be attributed or measured, and destination changes would require reprinting the QR.

## Google Review flow

```
Tap/scan -> validate card/QR -> record event -> Destination(type=GOOGLE_REVIEW)
  -> resolve GoogleReviewLocation.google_review_url -> redirect
```

A business with multiple branches gets one GoogleReviewLocation + one NFC card/QR per branch,
each with independent analytics.

## Card replacement

```
Old card -> status = LOST or REPLACED (audit logged)
Admin registers new card -> new token generated
New card assigned to same client + same destination_id (not copied, referenced)
Old token_hash remains in DB but card.status != ACTIVE, so /t/{oldToken} now renders
the "card inactive" error page instead of redirecting.
```

## Open-redirect protection

`CUSTOM_URL` destinations are the only ones holding an admin/client-supplied URL. On save,
the backend parses the URL and rejects anything whose scheme isn't `http`/`https` or whose
host fails basic validation (`javascript:`, `data:`, empty host, etc. are rejected at write
time, not redirect time, so bad data can never reach the hot path).
