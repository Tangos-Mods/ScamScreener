# Repo-Aufblaehung: Analyse und Kuerzungsvorschlaege

Stand: 2026-02-06

## Kurzfazit

Der Code ist nicht "kaputt", aber einige zentrale Klassen tragen zu viel Verantwortung und enthalten doppelte Hilfslogik. Das fuehrt zu unnoetiger Groesse und erschwert spaetere Aenderungen.

## Gemessene Kennzahlen

- Java-Dateien: `100`
- Java-Zeilen gesamt: `8023`
- Groesste Pakete:
  - `ui`: `1790` Zeilen (`22.3%`)
  - `ai`: `1660` Zeilen (`20.7%`)
  - `pipeline`: `1502` Zeilen (`18.7%`)
  - `commands`: `863` Zeilen (`10.8%`)
- Groesste Einzeldateien:
  - `src/main/java/eu/tango/scamscreener/ai/LocalAiTrainer.java` (`375`)
  - `src/main/java/eu/tango/scamscreener/rules/ScamRules.java` (`364`)
  - `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java` (`355`)
  - `src/main/java/eu/tango/scamscreener/pipeline/stage/LevenshteinSignalStage.java` (`354`)
  - `src/main/java/eu/tango/scamscreener/ai/ModelUpdateService.java` (`329`)
  - `src/main/java/eu/tango/scamscreener/pipeline/stage/RuleSignalStage.java` (`312`)

## Konkrete Aufblaehungsstellen (priorisiert)

### 1) `ScamRules` ist gleichzeitig API, Runtime-State, Config-I/O und Typcontainer

Belege:
- Sehr viele reine Weiterleiter (`minimumAlertRiskLevel`, `localAiEnabled`, ...): `src/main/java/eu/tango/scamscreener/rules/ScamRules.java:29`
- Persistenz-Aenderungen direkt in derselben Klasse (`setMinimumAlertRiskLevel`, `disableRule`, `enableRule`): `src/main/java/eu/tango/scamscreener/rules/ScamRules.java:105`, `src/main/java/eu/tango/scamscreener/rules/ScamRules.java:141`
- Interner `RuntimeConfig` + Parser + Pattern-Compile in derselben Datei: `src/main/java/eu/tango/scamscreener/rules/ScamRules.java:290`
- Bereits vorhandener TODO auf genau dieses Problem: `src/main/java/eu/tango/scamscreener/rules/ScamRules.java:20`

Kleiner machen:
- `ScamRuleService` (read-only Runtime-Zugriff)
- `ScamRulesConfigRepository` (load/save)
- `ScamRuleParsers` (Pattern/Enum Parsing)
- `ScamRules` nur noch als Typcontainer (`enum`, `record`)

### 2) `RiskMessages` mischt Darstellung, Konvertierung und Aktionslogik

Belege:
- Konvertiert `DetectionResult` -> `ScamAssessment`: `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java:37`
- Baut UI-Komponenten inkl. Zentrierungslogik: `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java:82`
- Erzeugt Command-Strings (`/scamscreener add ...`) und Hover-Details: `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java:295`

Kleiner machen:
- `RiskMessageViewModelFactory` (Konvertierung/Sortierung)
- `RiskActionBuilder` (Action-Tag + Commands)
- `RiskBannerFormatter` (rein visuelles Rendering)

### 3) Doppelte Hilfslogik fuer Archivierung und CSV-Parsing

Belege:
- `nextArchiveTarget(...)` doppelt:
  - `src/main/java/eu/tango/scamscreener/ai/LocalAiTrainer.java:223`
  - `src/main/java/eu/tango/scamscreener/ai/ModelUpdateService.java:232`
- `parseCsvLine(...)` doppelt:
  - `src/main/java/eu/tango/scamscreener/ai/LocalAiTrainer.java:345`
  - `src/main/java/eu/tango/scamscreener/pipeline/stage/LevenshteinSignalStage.java:337`
- `formatTimestamp(...)` doppelt:
  - `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java:380`
  - `src/main/java/eu/tango/scamscreener/ui/messages/BlacklistMessages.java:86`

Kleiner machen:
- `ArchiveFileUtil.nextArchiveTarget(...)`
- `CsvLineParser.parse(...)`
- `TimeFormatUtil.formatIsoTimestamp(...)`

### 4) `ScamScreenerClient` bleibt grosse Orchestrierungs- + Runtime-Klasse

Belege:
- Viele direkte Dependencies/Felder: `src/main/java/eu/tango/scamscreener/ScamScreenerClient.java:57`
- Enthaelt Init, Event-Hooks, Chat-Decoration, Debug-Config, Auto-Training in einer Klasse: `src/main/java/eu/tango/scamscreener/ScamScreenerClient.java:80`

Kleiner machen:
- `ClientBootstrap` (Wiring)
- `ChatEventRegistrar` (Fabric-Events)
- `DebugStateController` (Debug + screen flag)
- `AutoCaptureService` (autoAddFlaggedMessageToTrainingData)

### 5) Rule/Behavior-Signal-Logik stark verteilt und teils redundant

Belege:
- `RuleSignalStage` enthaelt grosse Keyword/Phrase-Bloecke und Scoring-Details: `src/main/java/eu/tango/scamscreener/pipeline/stage/RuleSignalStage.java:22`
- `BehaviorAnalyzer` und `RuleSignalStage` arbeiten beide mit Kontext-/Discord-Patterns: `src/main/java/eu/tango/scamscreener/pipeline/core/BehaviorAnalyzer.java:14`, `src/main/java/eu/tango/scamscreener/pipeline/stage/RuleSignalStage.java:17`

Kleiner machen:
- Keywords/Phrases in externe Konfig (`json`) oder dedizierte `RuleLexicon`-Klasse
- Gemeinsame Matcher-Helfer statt mehrfacher Pattern-Hilfsfunktionen

### 6) Uebergangsmodell `ScamAssessment` erhoeht Komplexitaet im UI-Pfad

Belege:
- Neue Pipeline arbeitet primaer mit `DetectionResult`
- In `RiskMessages` wird trotzdem zurueck auf `ScamAssessment` gemappt: `src/main/java/eu/tango/scamscreener/ui/messages/RiskMessages.java:46`
- `ScamAssessment` wird ausserhalb von UI/Preview kaum genutzt (`UiPreview`, `RiskMessages`)

Kleiner machen:
- `RiskMessages` vollstaendig auf `DetectionResult` standardisieren
- `ScamAssessment` nur behalten, wenn wirklich als oeffentliche API benoetigt

### 7) Kleine Repo-Aufblaehung durch versionierte Backup-Datei im `scripts`-Ordner

Beleg:
- `scripts/scam-screener-training-data.csv.bak` ist im Repo getrackt

Kleiner machen:
- Backup-Dateien aus Git entfernen und per `.gitignore` ausschliessen

## Quick Wins (wenig Risiko)

1. `nextArchiveTarget` in Utility extrahieren.
2. `parseCsvLine` in Utility extrahieren.
3. `formatTimestamp` in Utility extrahieren.
4. `RiskMessages` von `ScamAssessment` entkoppeln (nur `DetectionResult`).
5. `.bak`-Dateien aus Versionskontrolle nehmen.

## Mittelfristig (groesster Effekt)

1. `ScamRules` in Config-Repository + Runtime-Service + Typen aufteilen.
2. `ScamScreenerClient` auf reine Bootstrap-Verantwortung reduzieren.
3. Rule-/Behavior-Lexikon (Keywords/Phrases) aus Java-Code in Datenstruktur auslagern.

## Erwarteter Effekt

- Weniger Code-Duplikate und geringere Datei-Komplexitaet.
- Deutlich bessere Testbarkeit (kleinere Units, weniger statischer Zustand).
- Schnellere Refactors bei Regeln/AI/Message-UI ohne Seiteneffekte.
