# service.md

## Ziel
Der Training Hub soll den Mod-Upload ohne Benutzer-Login annehmen.
Der Mod authentifiziert sich nur ueber seine lokale `clientId`.

## Feste API
- Endpunkt: `POST /api/v1/client/uploads/anonymous`
- Request-Header:
  - `Content-Type: application/x-ndjson`
  - `X-ScamScreener-Filename: training-cases-v2.jsonl`
  - `X-ScamScreener-Client-Id: <normalized clientId>`
  - `X-ScamScreener-Payload-Sha256: <sha256(raw ndjson body)>`
  - `X-ScamScreener-Handshake-Sha256: <sha256(normalized clientId + ":" + payload sha256)>`
  - `User-Agent: ScamScreener/<version>+<mc>`
- Request-Body:
  - unveraenderte `training-cases-v2.jsonl` als NDJSON

## Client-ID-Regeln
- `clientId` genauso normalisieren wie im Mod:
  - `trim`
  - `lowercase`
- Uploads immer einer technischen Client-Identitaet zuordnen, auch wenn noch kein Web-Account verknuepft ist.
- Die `clientId` ist die einzige Mod-seitige Identitaet fuer Uploads.
- Der Server muss `payload sha256` aus dem Request-Body neu berechnen und gegen den Header pruefen.
- Der Server muss `handshake sha256` aus `normalized clientId + ":" + payload sha256` neu berechnen und gegen den Header pruefen.

## Antwortformat
- Erfolgs-/Business-Status:
  - `accepted`
  - `duplicate`
  - `quota-exceeded`
- Fehlerklassen:
  - `400` invalid schema / invalid payload
  - `413` file too large
  - `415` invalid content type
  - `429` rate limit / quota
  - `5xx` serverfehler

## Web-Flow
- Web-Account-Login bleibt fuer Dashboard und Admin erhalten.
- Der Mod nutzt diesen Login nicht mehr.
- Nutzer koennen im Training Hub spaeter eine oder mehrere `clientId`s mit ihrem Account verknuepfen.
- Nach der Verknuepfung zeigt das Dashboard auch historische Uploads dieser `clientId`s an.

## Umsetzungshinweise
- Anonyme Uploads duerfen nicht an fehlender Account-Verknuepfung scheitern.
- Duplicate-Erkennung und Quota-Regeln sollen weiterhin serverseitig pro Upload/Inhalt greifen.
- Historische Uploads muessen nachtraeglich einer spaeter verknuepften `clientId` zugeordnet sichtbar werden.
- Bei Hash-Mismatch muss der Upload mit `400` abgelehnt werden.
