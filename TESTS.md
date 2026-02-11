# Tests

Diese Datei beschreibt, welche automatisierten Tests aktuell vorhanden sind, was sie abdecken und wie sie die Funktionalitaet pruefen.

## Test-Ausfuehrung

- Alle Tests laufen mit: `.\gradlew.bat test`
- Framework: JUnit 5

## Aktuelle Testklassen

### `src/test/java/eu/tango/scamscreener/ai/ModelUpdateServiceHashTest.java`
- **Was wird getestet:**
  - Hash-Validierung bei Model-Updates (`hashMatchesExpected` in `ModelUpdateService`).
  - Regression fuer LF/CRLF + BOM/no BOM Varianten.
- **Wie wird getestet:**
  - Aufruf der privaten statischen Methode per Reflection.
  - Vergleich mit lokal berechneten SHA-256 Hashes.
  - Positive und negative Faelle:
    - exakter Hash matcht,
    - semantisch gleicher Text mit anderen UTF-8 Varianten matcht,
    - geaenderter Inhalt matcht nicht,
    - fehlende Eingaben (`null`/blank) liefern `false`.

### `src/test/java/eu/tango/scamscreener/chat/parser/ChatLineParserTest.java`
- **Was wird getestet:**
  - Erkennung gueltiger Player-Chat-Zeilen.
  - Abgrenzung zu System- und NPC-Zeilen.
- **Wie wird getestet:**
  - Direkte Input-Output Assertions auf `parsePlayerLine` und `isSystemLine`.
  - Positivfaelle (Direktchat, Whisper) und Negativfaelle (Trade-Systemmeldung, `[NPC]`).

### `src/test/java/eu/tango/scamscreener/pipeline/core/MessageEventParserTest.java`
- **Was wird getestet:**
  - Mapping von Chat-Formaten auf Kontext/Kanal (`party`, `team`, `pm`, `public`).
  - Filterung von Systemzeilen.
- **Wie wird getestet:**
  - Parsing typischer Beispielzeilen mit festen Timestamps.
  - Assertions auf `MessageContext`, `channel` und `null` bei Systemzeilen.

### `src/test/java/eu/tango/scamscreener/pipeline/core/WarningDeduplicatorTest.java`
- **Was wird getestet:**
  - Deduplizierung: gleiche Kombination aus Spieler + Risiko-Level nur einmal warnen.
  - Verhalten bei ungueltigen Eingaben.
  - Ruecksetzen via `reset()`.
- **Wie wird getestet:**
  - Stateful Sequenztests:
    - erster Aufruf `true`, zweiter gleicher Aufruf `false`,
    - anderes Level fuer gleichen Spieler weiterhin `true`,
    - nach `reset()` wieder `true`.

### `src/test/java/eu/tango/scamscreener/security/SafetyBypassStoreTest.java`
- **Was wird getestet:**
  - Blockieren per Pattern und Abruf von Pending-Eintraegen.
  - One-time Allow-Logik (`allowOnce`/`consumeAllowOnce`).
  - Begrenzung der Allow-Queue.
- **Wie wird getestet:**
  - Test-Store mit einfachem Regex (`secret`) in `@BeforeEach`.
  - Lifecycle-Assertions:
    - `blockIfMatch` erzeugt ID + Pending,
    - `takePending` konsumiert genau einmal,
    - `allowOnce` ist single-use und nach `isCommand` getrennt,
    - bei 6 Eintraegen wird der aelteste (Limit 5) verworfen.

### `src/test/java/eu/tango/scamscreener/util/IoErrorMapperTest.java`
- **Was wird getestet:**
  - Fehlertext-Mapping fuer Training-IO-Fehler.
- **Wie wird getestet:**
  - Gezielte Exception-Typen (`NoSuchFileException`, `AccessDeniedException`, allgemeine `IOException`).
  - Assertions auf exakte erwartete Meldung fuer:
    - `null` Fehler,
    - fehlende Datei,
    - Zugriff verweigert,
    - path-only Nachricht,
    - leere Nachricht (Fallback auf Klassenname).

### `src/test/java/eu/tango/scamscreener/util/TextUtilTest.java`
- **Was wird getestet:**
  - Text-Normalisierung und Command-Normalisierung.
  - Anonymisierung fuer AI (`@name`, Command-Targets, gemischte Name-Tokens).
  - Stabilitaet von `anonymizedSpeakerKey`.
- **Wie wird getestet:**
  - Deterministische String-zu-String Assertions.
  - Gleichheitspruefungen fuer case-insensitive Key-Bildung und Fallback `"speaker-unknown"`.

### `src/test/java/eu/tango/scamscreener/util/UuidUtilTest.java`
- **Was wird getestet:**
  - UUID-Parsing fuer gueltige/ungueltige Eingaben.
- **Wie wird getestet:**
  - Positivfall mit zufaellig generierter UUID (inkl. Whitespace-Trim).
  - Negativfaelle (`null`, leer, ungueltig) mit `null`-Erwartung.
