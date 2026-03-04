# trainingdata

Lege hier deine gesammelten Exportdateien ab.

Der Autotuner sucht rekursiv nach:

- `training-cases-v2.jsonl`

Beispielstruktur:

```text
trainingdata/
  player_a/training-cases-v2.jsonl
  player_b/training-cases-v2.jsonl
  batch_2026_03_04/training-cases-v2.jsonl
```

Danach aus dem Repo-Root ausfuehren:

```powershell
.\scripts\auto_tune_pipeline.ps1 -TrainingDataDir trainingdata -RulesFile run/config/scamscreener/rules.json -Apply
```

Ohne `-Apply` laeuft der Tuner im Dry-Run und schreibt nur `trainingdata/rules.autotuned.json`.
