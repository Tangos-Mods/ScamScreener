# ScamScreener Rewrite Guide

Dieses Dokument beschreibt die aktuelle Mod-Logik als technische Spezifikation, damit die Mod neu geschrieben werden kann, ohne Verhalten zu verlieren.

## 1. Ziel und Scope

- Zweck der Mod: Scam-Risiko in Chat-Nachrichten erkennen, warnen, markieren, optional Trainingsdaten sammeln und Blacklist-Risiken anzeigen.
- Laufzeit: Client-seitige Fabric-Mod.
- Primaere Inputs:
- eingehende Chat-/Game-Nachrichten
- ausgehende Chat-/Command-Nachrichten
- Client-Ticks
- User-Commands (`/scamscreener ...`)
- Primaere Outputs:
- dekorierte Chatzeilen
- Risiko-Warnkarten + Sound
- Blacklist-Warnungen
- persistierte JSON/CSV-Dateien unter `config/scamscreener`

## 2. Einstiegspunkt und Lifecycle

- Entrypoint: `src/main/java/eu/tango/scamscreener/ScamScreenerClient.java`.
- `onInitializeClient()`:
- laedt Blacklist, Regeln, Mute-Patterns, Debug-Config
- baut Services/Controller auf
- registriert Commands und Keybinds
- registriert Fabric-Events fuer Receive/Send/Tick
- registriert einmalige Model-Update-Pruefung beim ersten aktiven Tick

## 3. End-to-End Flows

### 3.1 Eingehende Nachricht (Erkennung + Darstellung)

- Event-Hooks:
- `ALLOW_GAME`: blockt gemutete Zeilen frueh
- `ALLOW_CHAT`: blockt gemutete Zeilen, parsed Spielerzeilen, fuehrt Screening aus, zeigt dekorierte Nachricht, unterdrueckt Original-Rendering (`return false`)
- `GAME` und `CHAT`: leiten in `IncomingMessageProcessor.process(...)` weiter
- `IncomingMessageProcessor`:
- speichert rohe Zeile im Chat-Ringpuffer (fuer Training-Capture)
- parsed Spielerzeile ueber `ChatLineParser`
- baut `MessageEvent` und ruft `DetectionPipeline.processWithResult(...)`
- fuehrt Trigger-basierte Blacklist-Pruefungen aus (`TriggerContext`)
- `ChatDecorator`:
- versieht Nachricht mit Hover/Click-Metadaten (Message-ID)
- faerbt Blacklist-Nachrichten rot
- haengt bei aktivem Screen-Modus ein Score-Badge `[NN]` mit Hover-Details an

### 3.2 Ausgehende Nachricht (Safety-Guard)

- Hooks: `ClientSendMessageEvents.ALLOW_CHAT` und `ALLOW_COMMAND`.
- `OutgoingMessageGuard`:
- blockt E-Mail- oder Discord-Invite-Muster
- erzeugt Bypass-ID in `SafetyBypassStore`
- User muss `/scamscreener bypass <id>` ausfuehren
- bei gueltigem Bypass: exakt eine Wiederholung derselben Nachricht/Command wird erlaubt

### 3.3 Tick-Loop

- `ClientTickController.onClientTick(...)`:
- aktualisiert Hover-Target fuer Flagging
- verarbeitet `CTRL+Y` (legit) und `CTRL+N` (scam)
- bei Disconnect: leert Dedupe-/Pipeline-State
- prueft einmal pro Session auf AI-Model-Update
- zeigt periodisch "X Nachrichten geblockt"-Info (Mute)
- scannt Team-Mitglieder gegen Blacklist und warnt beim ersten Auftreten

### 3.4 Training und lokales AI-Modell

- Capture:
- manuell ueber Commands
- per Hover + Keybind
- optional automatisch bei hoher Warnung (`autoCaptureAlertLevel`)
- Speicherung: `scam-screener-training-data.csv`
- Train:
- `LocalAiTrainer.trainAndSave(...)` trainiert logistisches Modell
- ueberschreibt lokales Modell-JSON
- archiviert altes Modell und Trainingsdatei
- laedt Regeln/Modell neu (`ScamRules.reloadConfig()`)

### 3.5 Remote-Model-Update

- `ModelUpdateService` laedt Version-Metadaten von GitHub.
- Wenn Update verfuegbar:
- erzeugt Pending-ID
- optional Auto-Download (bei Command-Flow)
- User-Aktionen:
- `accept`: Modell komplett ersetzen
- `merge`: incoming Modell uebernehmen, lokale Token-Weights ergaenzend mergen
- `ignore`: Pending verwerfen

## 4. Detection-Pipeline (fachliche Kernlogik)

Implementierung: `src/main/java/eu/tango/scamscreener/pipeline/core/DetectionPipeline.java`.

Pipeline-Reihenfolge:

1. Mute-Filter (frueher Exit als `muted=true`).
2. Behavior-Analyse (klassifiziert Verhaltensflags + Kontakt-Streak pro Spieler).
3. Rule-Signale (`RuleSignalStage`).
4. Levenshtein-Signale (`LevenshteinSignalStage`).
5. Behavior-Signale (aus Analysis).
6. AI-Signal (`AiScorer` -> `LocalAiScorer`).
7. Trend-Signal (`TrendStore`) ueber mehrere Nachrichten.
8. Summenscore + Level + Rule-Details + evaluatedMessages.
9. Warnentscheidung + Dedupe.

Wichtige feste Gewichte:

- Rule:
- `SUSPICIOUS_LINK` +20
- `PRESSURE_AND_URGENCY` +15
- `UPFRONT_PAYMENT` +25
- `ACCOUNT_DATA_REQUEST` +35
- `TOO_GOOD_TO_BE_TRUE` +15
- `TRUST_MANIPULATION` +10
- `DISCORD_HANDLE` +50
- Similarity-Regeln: konfigurierbar (`similarityRuleWeight`, `similarityTrainingWeight`)
- Behavior:
- `EXTERNAL_PLATFORM_PUSH` +15
- `UPFRONT_PAYMENT` +25
- `ACCOUNT_DATA_REQUEST` +35
- `FAKE_MIDDLEMAN_CLAIM` +20
- `SPAMMY_CONTACT_PATTERN` +10 (ab 3 Kontaktversuchen)
- Trend:
- `MULTI_MESSAGE_PATTERN` +20

Trend-Bedingungen:

- Fenster: 45 Sekunden
- mindestens 3 Nachrichten
- mindestens 2 Nachrichten mit Rule-Signal
- kumulierter Score >= 35
- es werden max. 8 letzte Nachrichten pro Spieler gehalten

Level-Grenzen (default aus Config):

- `MEDIUM >= 20`
- `HIGH >= 40`
- `CRITICAL >= 70`

Warnung wird nur ausgeloest wenn:

- Score > 0
- mindestens eine Rule getriggert
- tatsaechliches Risk-Level >= `minAlertRiskLevel`
- Dedupe-Key `behavior-risk:<player>:<level>` wurde noch nicht gesehen

## 5. Parsing und Trigger-Kontrakte

- Spielerchat-Parsing: `ChatLineParser.parsePlayerLine(...)`
- erkennt `name: message`
- filtert Systemlabels und `[ScamScreener]`-Nachrichten
- Trigger-Kontexte (`TriggerContext`):
- eingehender Trade-Request
- ausgehender Trade-Request
- aktive Trade-Session
- Triggerwarnungen deduplizieren ueber Kontext+Spieler/UUID.

## 6. Persistenz-Kontrakte (Dateien)

Alle unter `config/scamscreener/` (Legacy-Migration aus `config/` vorhanden):

- `scam-screener-blacklist.json`
- `scam-screener-rules.json`
- `scam-screener-mute.json`
- `scam-screener-debug.json`
- `scam-screener-local-ai-model.json`
- `scam-screener-training-data.csv`

Archive:

- `old/training-data/*`
- `old/models/*`

Training-CSV Header (aktuell):

- `message,label,pushes_external_platform,demands_upfront_payment,requests_sensitive_data,claims_middleman_without_proof,too_good_to_be_true,repeated_contact_attempts,is_spam,asks_for_stuff,advertising`

## 7. Command-Oberflaeche (externes API)

Root: `/scamscreener`

- `add`, `remove`, `list`
- `mute`, `unmute`
- `bypass`
- `ai ...`
- `rules ...`
- `alertlevel ...`
- `screen ...`
- `debug ...`
- `version`
- `preview`

AI-Unterkommandos:

- `capture <player> legit|scam [count]`
- `capturebulk <count>`
- `flag <messageId> legit|scam`
- `migrate`
- `model download|accept|merge|ignore <id>`
- `update [force]`
- `train`
- `reset`
- `autocapture [off|low|medium|high|critical]`

Aliase:

- `/1 <player> <count>` = scam capture
- `/0 <player> <count>` = legit capture

## 8. UI/UX-Verhalten, das erhalten bleiben muss

- Risiko-Warnkarte mit Regeln/Details/Actions (`RiskMessages`).
- Blacklist-Warnkarte mit Score/Reason/Zeit.
- Chat-Hover fuer Screening-Details.
- Score-Badge `[NN]` an dekorierter Nachricht (wenn Screen aktiv und nicht gemutet).
- Message-Flagging per Hover + `CTRL+Y`/`CTRL+N`.
- 3x kurzer Warnsound bei Risiko-/Blacklist-Warnung.

## 9. Rewrite-Invarianten (nicht brechen)

- Keine stillen Verhaltensaenderungen in Score-Logik, Gewichten, Schwellenwerten oder Dedupe-Regeln.
- Persistenzpfade und Migrationsverhalten kompatibel halten.
- Command-Syntax und Semantik kompatibel halten.
- Outgoing-Safety muss weiterhin "block -> bypass id -> one-shot allow" erzwingen.
- Disconnect muss zustandsbehaftete Detektoren zuruecksetzen.
- Fehler bei IO/Netzwerk duerfen Mod nicht crashen (heute weitgehend fail-soft).

## 10. Empfohlene Zielarchitektur fuer Rewrite

- Core Domain ohne Minecraft/Fabric-Abhaengigkeit:
- `ScreeningEngine` (pure)
- `RuleEngine`, `SimilarityEngine`, `BehaviorEngine`, `AiEngine`, `TrendEngine`
- `DecisionPolicy`
- Adapter-Schicht:
- Fabric Event Adapter
- UI Presenter
- Config/Repository Adapter
- Explizite Ports fuer:
- Clock
- Storage
- Network (Mojang/Model-Update)
- Notification/Sound

Damit kann die Erkennung separat getestet und spaeter auch ausserhalb Minecraft wiederverwendet werden.

## 11. Minimale Test-Matrix fuer Paritaetsnachweis

- Parser:
- gueltige Spielerzeile, Systemzeile, Farbcodes, Edge-Cases
- Pipeline:
- jede Rule einzeln
- Kombinationen + Trend-Bonus
- Warn-Dedupe pro Spieler/Level
- Auto-Capture Schwellwerte
- Mute:
- Pattern-Hit blockt
- ScamScreener-Nachrichten werden nicht gemutet
- Safety:
- Email/Discord blocken
- Bypass erlaubt genau einmal
- Commands:
- add/remove/list
- ai capture/train/reset/update
- rules enable/disable
- screen/debug toggles
- Persistenz:
- Legacy-Datei-Migration
- Archivierung alter Trainings-/Model-Dateien
