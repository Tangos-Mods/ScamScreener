# ScamScreener Upload Relay - Bootstrap Data Migration

This document defines the server-side data changes to support automatic client onboarding (`/api/v1/client/bootstrap`) so players can upload without manually entering invite codes.

## 1) Objective

Move from:

- invite-only credential provisioning (`/api/v1/client/redeem`)

to:

- invite-compatible + automatic bootstrap provisioning (`/api/v1/client/bootstrap`)

while keeping replay-safe signed uploads unchanged.

## 2) Compatibility Rules

1. Keep `/api/v1/client/redeem` working (no breaking removal).
2. Add `/api/v1/client/bootstrap` as an additional onboarding path.
3. Keep upload signing contract unchanged for `/api/v1/training-uploads`.
4. Existing clients remain valid.

## 3) SQLite Migration

Run this migration once:

```sql
BEGIN;

ALTER TABLE clients ADD COLUMN install_id TEXT;
ALTER TABLE clients ADD COLUMN provision_method TEXT NOT NULL DEFAULT 'redeem';
ALTER TABLE clients ADD COLUMN created_ip TEXT;
ALTER TABLE clients ADD COLUMN last_seen_ip TEXT;
ALTER TABLE clients ADD COLUMN last_seen_at TEXT;
ALTER TABLE clients ADD COLUMN upload_day_key TEXT;
ALTER TABLE clients ADD COLUMN upload_day_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE clients ADD COLUMN upload_day_limit INTEGER NOT NULL DEFAULT 30;

CREATE UNIQUE INDEX IF NOT EXISTS idx_clients_install_id_active
ON clients(install_id)
WHERE active = 1 AND install_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS bootstrap_audit (
  request_id TEXT PRIMARY KEY,
  install_id TEXT,
  client_id TEXT,
  ip TEXT NOT NULL,
  user_agent TEXT,
  decision TEXT NOT NULL,
  reason TEXT,
  created_at TEXT NOT NULL
);

COMMIT;
```

## 4) Bootstrap Endpoint Contract

### `POST /api/v1/client/bootstrap`

Request JSON:

1. `installId` (UUID, required)
2. `modVersion` (string, required)
3. `signatureVersion` (string, optional, default `v1`)

Success response:

```json
{
  "ok": true,
  "clientId": "relay-client-...",
  "clientSecret": "relay-secret-...",
  "signatureVersion": "v1"
}
```

## 5) Bootstrap Decision Logic

1. Validate request schema.
2. Apply per-IP rate limit (for example `60/min`) and a stricter per-install limit (for example `10/hour`).
3. If active client exists for `installId`, return existing credentials.
4. Else create a new active client with:
   - generated `client_id`
   - generated `client_secret`
   - `install_id = request.installId`
   - `provision_method = 'bootstrap'`
5. Write `bootstrap_audit` row for every attempt (allow + deny).
6. Never return webhook values or internal secrets.

## 6) Upload Quota Data Logic

On each accepted upload:

1. Compute day key (`YYYY-MM-DD` UTC).
2. If `upload_day_key` differs, reset `upload_day_count = 0` and set new key.
3. Reject with `429 RATE_LIMITED` when `upload_day_count >= upload_day_limit`.
4. Increment `upload_day_count` after successful validation.

This protects against low-effort abuse when bootstrap is open.

## 7) Revocation and Recovery

1. Revoke compromised client by setting `active = 0`.
2. A revoked installation can bootstrap again and receive fresh credentials.
3. Keep `bootstrap_audit` + `upload_audit` for incident review.

## 8) Rollout Plan

1. Deploy DB migration.
2. Deploy server with `/bootstrap` endpoint and limits.
3. Keep `/redeem` enabled during transition.
4. Monitor:
   - bootstrap success rate
   - 429 rate by IP/install
   - upload accept/reject ratio
5. Tune `upload_day_limit` and rate limits as needed.

## 9) Required Security Baseline

1. Public access only through HTTPS reverse proxy (Nginx/Caddy).
2. App process bound to localhost (`127.0.0.1`).
3. Strict request size limit.
4. Nonce replay checks remain mandatory on upload endpoint.
5. Constant-time signature comparison remains mandatory.
