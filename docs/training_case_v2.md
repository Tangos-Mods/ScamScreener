# training_case_v2

## Zweck
`training_case_v2` ist das kanonische Exportformat aus dem Case-Review.
Es ist die einzige Quelle fuer:

- ContextStage-Training
- Kalibrierung der festen Stages (Rule, Similarity, Behavior, Trend, Funnel)

Training selbst passiert **nicht im Mod** und **nicht beim Spieler**, sondern beim Dev im IDE.

## Rollen

- Spieler:
  - markiert Cases im Review
  - exportiert eine JSONL-Datei
  - laedt die Datei im Training Hub hoch (`server/`)
- Dev:
  - greift im Training Hub auf Uploads zu
  - trainiert/tuned ContextStage und feste Stage-Gewichte
  - baut neue Mod-Versionen

## Export-Datei
Alle Exporte landen unter `config/scamscreener/`.

- `training-cases-v2.jsonl`
  - kanonischer Voll-Case inkl. Context-Targets und Fixed-Stage-Kalibrierungen

Nur Cases mit Verdict `RISK` oder `SAFE` werden exportiert.
`PENDING` und `IGNORED` werden uebersprungen.

## JSONL-Grundregel
Jede Zeile ist genau ein JSON-Objekt.
Keine Arrays ueber mehrere Zeilen.

## Kanonisches Schema

```json
{
  "format": "training_case_v2",
  "schemaVersion": 2,
  "caseId": "case_000001",
  "caseData": {
    "label": "risk",
    "messages": [
      {
        "index": 0,
        "text": "add me on discord",
        "sourceType": "player",
        "speakerRole": "other",
        "trigger": true,
        "caseRole": "signal",
        "signalTagIds": ["external_platform"],
        "mappingIds": ["stage.rule::rule.external_platform"]
      }
    ],
    "caseSignalTagIds": ["external_platform"]
  },
  "observedPipeline": {
    "scoreAtCapture": 25,
    "outcomeAtCapture": "review",
    "decidedByStageId": "stage.rule",
    "stageResults": [
      {
        "stageId": "stage.rule",
        "decision": "pass",
        "scoreDelta": 20,
        "reasonIds": ["rule.external_platform"]
      }
    ]
  },
  "supervision": {
    "contextStage": {
      "targetLabel": "risk",
      "signalMessageIndices": [0],
      "contextMessageIndices": [],
      "excludedMessageIndices": [],
      "targetSignalTagIds": ["external_platform"]
    },
    "fixedStageCalibrations": [
      {
        "mappingId": "stage.rule::rule.external_platform",
        "stageId": "stage.rule",
        "reasonId": "rule.external_platform",
        "action": "increase",
        "strength": "strong",
        "weightDeltaHint": 2,
        "becauseMessageIndices": [0]
      }
    ]
  }
}
```

## Feld-Erklaerung

- `caseId`
  - neutral, stabil, export-sequenziell (`case_000001`, ...)
- `caseData.messages[].mappingIds`
  - stabile IDs fuer feste Stage-Hinweise (`stage.*::reason.*`)
- `observedPipeline.stageResults[].reasonIds`
  - stabile maschinenlesbare Rule/Signal-IDs
- `supervision.contextStage`
  - Ziel-Labels und Message-Rollen fuer spaeteres ContextStage-Training
- `supervision.fixedStageCalibrations`
  - direkte Kalibrier-Hinweise fuer feste Stages

## Weight-Anweisungen fuer feste Stages
`fixedStageCalibrations` enthaelt:

- `action`
  - `increase`, `decrease`, `keep`
- `strength`
  - `none`, `normal`, `strong`
- `weightDeltaHint`
  - numerischer Hint (`+2`, `-1`, ...)
- `becauseMessageIndices`
  - welche Messages den Hint begruenden

Damit kann der Dev im IDE Stage-Gewichte gezielt nachziehen, ohne freie Textauswertung.

## Stabile Mapping-IDs
Mapping basiert auf stabilen IDs statt Anzeige-Text.

- Stage-ID: `stage.rule`, `stage.behavior`, `stage.context`, ...
- Reason-ID: `rule.external_platform`, `behavior.repeated_message`, ...
- Mapping-ID: `stage.rule::rule.external_platform`

Legacy-Strings werden beim Laden normalisiert, damit alte Reviews weiter nutzbar bleiben.

## Privacy und Persistenz

- UUIDs:
  - nicht in persistierten Review-/Trainingsexporten
  - UUID bleibt absichtlich nur in `whitelist.json` und `blacklist.json`
- Spielernamen:
  - duerfen in Message-Texten vorkommen
  - keine teure nachtraegliche Entschluesselung/Resolver-Pflicht

## Dev-Workflow im IDE

1. Spieler exportiert Cases (`Export for Dev` oder `/scamscreener review export`).
2. Spieler meldet sich im Training Hub an und laedt `training-cases-v2.jsonl` hoch.
3. Dev legt die final geprueften Exporte in `trainingdata/` (rekursiv, beliebige Unterordner).
4. Dev startet den Autotuner:
   - Dry run:
     - `.\scripts\auto_tune_pipeline.ps1 -TrainingDataDir trainingdata -RulesFile run/config/scamscreener/rules.json`
   - Apply:
     - `.\scripts\auto_tune_pipeline.ps1 -TrainingDataDir trainingdata -RulesFile run/config/scamscreener/rules.json -Apply`
5. Dev validiert das Ergebnis in Tests und Ingame.
6. Dev trainiert/entwickelt ContextStage optional weiter direkt aus `training-cases-v2.jsonl`.
7. Dev liefert neue Mod-Version.

## Was der Autotuner macht

- Aggregiert alle `weightDeltaHint` Werte pro `mappingId`.
- Rechnet daraus begrenzte Schrittweiten (`MaxStep`) mit Konflikt-Daempfung.
- Schreibt neue Score-/Threshold-Werte in `rules.autotuned.json` (oder direkt in `rules.json` mit `-Apply`).
- Nutzt stabile IDs (`stage.*::reason.*`), keine UI-Texte.
