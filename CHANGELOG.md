# Changelog

## 2.0.0 - 2026-03-04

### Added
- Canonical `training_case_v2` export format as JSONL (`training-cases-v2.jsonl`).
- Case-review export flow in UI (`Export for Dev`) and command (`/scamscreener review export`).
- Stable stage/reason mapping IDs (`stage.*`, `reason.*`, `stage.*::reason.*`) for training calibration.
- `ContextStage` integration into the default pipeline and runtime rules.
- New developer auto-tuning script: `scripts/auto_tune_pipeline.ps1`.
- Player-facing documentation for case review and training export workflow.
- Training Hub under `server/` with player accounts, upload dashboard, admin panel and pipeline trigger.

### Changed
- Export flow now writes a single canonical file instead of multiple artifacts.
- Review/training persistence sanitization removes UUID identity from stored payloads.
- Blacklist and whitelist persistence remains UUID-based by design.
- Auto-capture wording clarified in UI (`Auto-Capture Cases`) and behavior text.
- Multiple settings screens received spacing and wording fixes to avoid header overlap.

### Removed
- Legacy review-detail routing in favor of unified alert info flow.
- Legacy preview export path and obsolete training/legacy UI labels.
- Training auto-upload flow in the mod (`trainingUpload` config + `/scamscreener review upload`).
