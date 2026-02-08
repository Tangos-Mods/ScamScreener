# ScamScreener

Client-seitiger Fabric-Mod fuer **Minecraft 1.21.10**, der Hypixel-SkyBlock-Chat auf Scam-Risiken prueft.
Der Mod kombiniert:

- eine manuelle Blacklist,
- regelbasierte Erkennung (Regex + Verhaltenssignale),
- ein lokales, trainierbares KI-Scoring (ohne Cloud-Zwang),
- sowie Chat-Muting ueber eigene Pattern.

> Aktueller Stand laut `gradle.properties`: **Version 0.13.4**

## Warum dieser Mod?

Scams in Trading-/Party-Situationen laufen oft ueber:

- Druck ("quick", "now", "last chance"),
- Vorkasse-Forderungen,
- "trust me"/"legit middleman" Claims,
- externe Plattformen/Links (Discord, Telegram, `/visit` etc.),
- wiederholte Kontaktversuche.

ScamScreener bewertet solche Signale in Echtzeit und zeigt Warnungen mit nachvollziehbaren Gruenden an (inkl. Rule-Details im Hover).

---

## Kernfunktionen

## 1) Blacklist-Warnungen

- Spieler koennen manuell auf die lokale Blacklist gesetzt werden.
- Der Mod warnt, wenn Blacklist-Spieler in relevanten Kontexte auftauchen:
  - Team/Party-Kontext,
  - Party-Join/Party-Tab Situationen,
  - Party Finder Dungeon-Join (`Party Finder > <Name> joined the dungeon group! ...`).
- Warnungen enthalten Name, Score, Reason, Timestamp und Trigger-Kontext.

## 2) Live-Chat-Erkennung (Rules + Local AI)

Jede erkannte Spieler-Chatzeile wird analysiert:

- **Regelbasiert** ueber konfigurierbare Pattern (Link, Urgency, Payment-first, etc.).
- **Verhaltensbasiert** ueber Flags (z. B. "demands upfront payment").
- **Local AI** (logistisches Modell mit Token-/N-Gram-Weights) liefert zusaetzliches Risikosignal.
- **Multi-Message-Trend**: mehrere Nachrichten eines Spielers in kurzer Zeit koennen zusaetzlich Risiko ausloesen.

Wenn Schwellwerte erreicht werden:

- visuelle Risk-Warnung im Chat,
- Rule-Liste mit Hover-Erklaerung ("Why triggered"),
- 3x kurzer Warnsound,
- optionales Auto-Capturing in Trainingsdaten (ab einstellbarem Level).

## 3) AI-Training mit lokalen Daten

- Trainingsdaten landen in CSV.
- `/scamscreener ai train` trainiert ein lokales Modell und speichert es als JSON.
- Alte Trainingsdateien/Modelle werden archiviert (`old/` Unterordner).
- Manuelles Capturing moeglich: letzte Nachrichten eines bestimmten Spielers als `scam|legit` labeln.

## 4) Chat-Muting ueber Pattern

- Eigene Pattern per Command muten.
- Case-insensitive.
- Regex wird unterstuetzt.
- **Wichtige Default-Logik**: wenn kein Regex-Metazeichen genutzt wird, wird als **ganzes Wort/ganze Phrase** gematcht (nicht als Teilstring in anderen Woertern).
- Blockierte Nachrichten koennen periodisch zusammengefasst gemeldet werden.

---

## Technischer Aufbau (vereinfacht)

1. Minecraft liefert eingehende Chat-/Game-Messages.
2. Mute-Manager entscheidet zuerst, ob Message geblockt wird.
3. Ungeblockte Messages laufen in den Detector:
   - Parsing der Spielerzeile,
   - Rule + Behavior + LocalAI Scoring,
   - Trend-Analyse ueber mehrere Nachrichten.
4. Bei Trigger:
   - Warning-Message mit Level/Score/Rules,
   - Hover mit Rule-Details + evaluierte Message(s),
   - optional Auto-Capture in Trainingsdaten.
5. Parallel: Party-/Kontext-Checks fuer Blacklist-Warnungen.

---

## Internal architecture (pipeline)

- Jede Chatzeile wird zu einem `MessageEvent` (Raw + Normalized) geparst.
- `MuteStage` filtert gemutete Nachrichten frueh aus.
- `RuleSignalStage` erzeugt Signals aus Regex-Regeln (Pattern werden einmal kompiliert).
- `BehaviorSignalStage` erzeugt Signals aus Verhalten (z. B. upfront payment, external platform) und nutzt Patterns aus `scam-screener-rules.json`.
- `AiSignalStage` kapselt das Local-AI-Scoring und liefert ein neutral-gewichtetes Signal.
- `TrendSignalStage` nutzt einen `TrendStore` pro Spieler (TTL-Deque) fuer Multi-Message-Patterns.
- `ScoringStage` kombiniert alle Signals in Score + Level und generiert Explainability-Daten.
- `DecisionStage` prueft Alert-Level + Dedupe, bevor gewarnt wird.
- `OutputStage` kapselt UI/Audio (Chat-Warnung, Hover-Details, Sound).
- Behavior-Patterns stammen aus `scam-screener-rules.json` (`externalPlatformPattern`, `upfrontPaymentBehaviorPattern`, `accountDataBehaviorPattern`, `middlemanPattern`).

---

## Installation (User)

Voraussetzungen:

- Minecraft `1.21.10`
- Fabric Loader `>= 0.18.4`
- Fabric API (passend zur MC-Version)
- Java `21+`

Mod-JAR in den `mods/` Ordner legen, Spiel starten.

---

## Build (Developer)

```powershell
.\gradlew.bat build
```

Artefakt liegt danach in `build/libs/`.

---

## Commands

## Allgemein

- `/scamscreener` -> Hilfe
- `/scamscreener add <player> [score] [reason]`
- `/scamscreener remove <player>`
- `/scamscreener list`
- `/scamscreener alertlevel [low|medium|high|critical]`
- `/scamscreener rules <list|disable|enable> [rule]`
- `/scamscreener version`
- `/scamscreener preview` (Dry-Run + Live-Preview anhand letzter Chatzeile)

## AI

- `/scamscreener ai` -> AI-Hilfe
- `/scamscreener ai capture <player> <scam|legit> [count]`
- `/scamscreener ai train`
- `/scamscreener ai reset`
- `/scamscreener ai autocapture [off|low|medium|high|critical]`

## Mute

- `/scamscreener mute` -> listet aktuelle Pattern
- `/scamscreener mute <pattern>`
- `/scamscreener unmute <pattern>` (Autocomplete)

---

## Konfigurationsdateien

Alle Mod-Dateien liegen unter:

- `config/scamscreener/`

Wichtige Dateien:

- `scam-screener-blacklist.json` -> Blacklist-Entries
- `scam-screener-rules.json` -> Rule-Pattern, AI-Thresholds, disabled rules
- `scam-screener-local-ai-model.json` -> lokales Modell + Weights
- `scam-screener-training-data.csv` -> Trainingssamples
- `scam-screener-mute.json` -> Mute-Pattern + Notify-Settings

Archivstruktur:

- `config/scamscreener/old/training-data/`
- `config/scamscreener/old/models/`

---

## Training kurz erklaert

1. Mit `ai capture` Samples sammeln (am besten sauber gelabelt).
2. `ai train` ausfuehren.
3. Modell wird aktualisiert, Trainingsdatei archiviert, Rules neu geladen.
4. Ergebnis pruefen (Warnungen + Tooltips + Fehlalarme beobachten).

Tipps:

- Scam **und** Legit Beispiele nutzen (beide Klassen sind wichtig).
- Keine System-/Mod-Statusmeldungen als Training aufnehmen.
- Lieber weniger, aber saubere Samples.

---

## Datenschutz / Offline-Verhalten

- Der Kern der Erkennung laeuft lokal auf dem Client.
- Modelltraining + Scoring passieren lokal in Dateien unter `config/scamscreener`.
- Es wird kein externer KI-Dienst zwingend benoetigt.
- Mojang-Name-Lookup kann bei Bedarf fuer Namensaufloesung verwendet werden.

---

## Grenzen

- Rule/Keyword/N-Gram Systeme verstehen Semantik nur begrenzt.
- Sarkasmus/mehrdeutige Kontexte koennen Fehlalarme erzeugen.
- Zu aggressive Mute-Pattern koennen legitime Messages unterdruecken.
- Gute Trainingsdatenqualitaet ist entscheidend fuer das lokale Modell.

---

## Lizenz

Siehe `LICENSE`.
