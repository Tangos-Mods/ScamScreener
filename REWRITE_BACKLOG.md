# Rewrite Backlog

Stand: 2026-02-07
Ziel: Verhaltensparitaet bei klarerer Architektur, weniger Kopplung, weniger Boilerplate.

## 1. Zielarchitektur (neu)

Package-Zielbild:

- `eu.tango.scamscreener.domain`
- `eu.tango.scamscreener.domain.model`
- `eu.tango.scamscreener.domain.rules`
- `eu.tango.scamscreener.domain.pipeline`
- `eu.tango.scamscreener.domain.safety`
- `eu.tango.scamscreener.app`
- `eu.tango.scamscreener.app.usecase`
- `eu.tango.scamscreener.app.port`
- `eu.tango.scamscreener.adapter.fabric`
- `eu.tango.scamscreener.adapter.ui`
- `eu.tango.scamscreener.adapter.command`
- `eu.tango.scamscreener.infra.config`
- `eu.tango.scamscreener.infra.storage`
- `eu.tango.scamscreener.infra.network`

Regel:

- Domain kennt kein Fabric, kein Minecraft API, kein Dateisystem, kein HTTP.
- Adapter kapseln Minecraft/Fabric Events und UI Components.
- Infra kapselt JSON/CSV/HTTP.

## 2. Migrationsreihenfolge (empfohlen)

1. Paritaet absichern (Tests/Fixtures), dann refactoren.
2. `ScamRules` entkoppeln (Config + Runtime Service).
3. Pipeline in reine Domain-Engine ziehen.
4. Commands/Message-UI auf Use-Cases umstellen.
5. Client-Bootstrap ausduennen.
6. Restliche technische Schulden entfernen.

## 3. Epics und Tasks

## Epic A - Paritaetsschutz vor Umbau

- A1: Golden-Inputs fuer Parser anlegen (`ChatLineParser`).
- A2: Golden-Inputs fuer Detection-Pipeline anlegen (Score, Level, Rules, Warnflag, Captureflag).
- A3: Golden-Inputs fuer Safety-Guard anlegen (block/bypass/allow-once).
- A4: Command-Snapshot-Tests fuer zentrale Flows anlegen.
- A5: Testdaten fuer Persistenz-Migrationen anlegen (Legacy -> aktuelle Dateien).

Definition of Done:

- Fuer Kernfaelle existiert vorher/nachher identische fachliche Ausgabe.
- Refactors duerfen nur bei absichtlicher Regelanpassung Test-Fixtures aendern.

## Epic B - Rules Runtime entkoppeln

- B1: `ScamRulesConfigRepository` einfuehren (load/save nur IO).
- B2: `ScamRulesRuntimeService` einfuehren (nur geladene Runtime-Werte).
- B3: Pattern-Compile/Parsing aus `ScamRules` auslagern.
- B4: Bestehende statische Zugriffe auf RuntimeService umstellen.
- B5: `ScamRules` auf Typcontainer reduzieren (Enums, Records, evtl. kleine Validatoren).

Definition of Done:

- Keine JSON-I/O mehr in `ScamRules`.
- Kein globaler statischer Runtime-Mix aus Config + Behavior mehr.

## Epic C - Detection als Domain Engine

- C1: `ScreeningEngine` (pure) einfuehren.
- C2: Stages als Domain-Komponenten modellieren:
- `RuleDetector`
- `SimilarityDetector`
- `BehaviorDetector`
- `AiRiskDetector`
- `TrendDetector`
- C3: `DecisionPolicy` fuer Warnen/Capture/Dedupe einfuehren.
- C4: Fabric-Adapter uebersetzt Chat-Events -> Domain-Input und Domain-Output -> UI.
- C5: Reset- und Session-State explizit ueber Engine-Session kapseln.

Definition of Done:

- Domain-Pipeline kann ohne Minecraft Klassen getestet werden.
- Aktuelles Scoring bleibt paritaetisch.

## Epic D - App Use-Cases statt God-Client

- D1: `ScamScreenerClient` auf Bootstrap + Wiring reduzieren.
- D2: Use-Cases einfuehren:
- `ProcessIncomingMessageUseCase`
- `ProcessOutgoingMessageUseCase`
- `TickUseCase`
- `BlacklistUseCase`
- `TrainingUseCase`
- `ModelUpdateUseCase`
- D3: Controller/Handler auf Use-Cases umstellen.
- D4: Shared State (`currentlyDetected`, `warnedContexts`, warning dedupe) in Session-Objekt verschieben.

Definition of Done:

- `ScamScreenerClient` enthaelt keine Fachlogik mehr.
- Flows sind in kleinen Use-Case Klassen testbar.

## Epic E - Commands und UI presenterisieren

- E1: Command-Parsing von Command-Aktionen trennen.
- E2: `ScamScreenerCommands` in Command-Tree + Action-Handler splitten.
- E3: `RiskMessages` in ViewModel + Renderer splitten.
- E4: `ScreenMessages` und `BlacklistMessages` auf gemeinsame Formatter-Helfer bringen.
- E5: Hover/Click/Flagging als separaten Presenter kapseln.

Definition of Done:

- UI-Bausteine sind fuer sich testbar.
- Command-Handler rufen nur Use-Cases auf.

## Epic F - Infra Vereinheitlichung

- F1: Einheitliche Repositories fuer:
- Blacklist
- RulesConfig
- DebugConfig
- MuteConfig
- LocalAiModel
- TrainingData
- F2: Einheitliches Fehler-Mapping fuer IO/HTTP.
- F3: Model-Update Netzwerkcode hinter Port kapseln.
- F4: Archivierungslogik zentralisieren (bereits teilweise vorhanden) und ueberall verwenden.

Definition of Done:

- Keine verstreute Dateipfadlogik in Fachklassen.
- Persistenzverhalten bleibt kompatibel.

## Epic G - Aufraeumen und Vereinfachen

- G1: DTOs ohne Verhalten als `record` markieren (wo sinnvoll).
- G2: Lombok nur dort nutzen, wo LOC sinkt und Lesbarkeit steigt.
- G3: Ueberfluessige Builder/Factory-Ketten entfernen.
- G4: Doppelte Utility-Logik eliminieren.
- G5: Dokumentation aktualisieren (`AGENTS.md` + kurzer Architektur-Ueberblick).

Definition of Done:

- Klassenanzahl und LOC sinken weiter.
- Keine funktionalen Regressionen.

## 4. Konkrete Reihenfolge fuer die naechsten 3 Arbeitsbloecke

### Block 1 (sicher)

- Epic A komplett.
- Epic B bis B3.

### Block 2 (mittel)

- Epic B fertig.
- Epic C bis C3.

### Block 3 (riskant)

- Epic C fertig.
- Epic D starten.
- Epic E starten.

## 5. Messbare KPIs

- Build bleibt gruen.
- Kein Delta in Golden-Tests der Kernlogik.
- `ScamScreenerClient.java` deutlich kleiner (nur Bootstrap).
- Weniger direkte Abhaengigkeiten pro Kernklasse.
- Gesamtklassenzahl und Gesamt-LOC sinken.
