# Changelog

All notable changes to this project are documented in this file.

## 2.0.0 - 2026-03-06

Full transition from `legacy-v1` to `v2`.

### Scope (legacy-v1 -> v2)
- 385 files changed.
- 21,410 insertions and 23,314 deletions.
- Core architecture rewritten across runtime, pipeline, UI, command, config, and test layers.

### Added
- New runtime container and startup wiring (`ScamScreenerRuntime`, `ScamScreenerMod`).
- New typed config model with dedicated stores:
  - `runtime.json`, `rules.json`, `review.json`, `whitelist.json`, `blacklist.json`.
- New modular detection pipeline architecture:
  - engine + factory (`PipelineEngine`, `ScamScreenerPipelineFactory`)
  - stages: `PlayerListStage`, `RuleStage`, `LevenshteinStage`, `BehaviorStage`, `TrendStage`, `FunnelStage`, `ContextStage`, `MuteStage`
  - stage/rule data model (`ChatEvent`, `PipelineDecision`, `StageResult`, `RuleCatalog`, `SafeRegex`).
- New runtime state stores:
  - `BehaviorStore`, `TrendStore`, `FunnelStore`, `PipelineStateStore`.
- New list domain model and API events:
  - in-memory `Whitelist`/`Blacklist`
  - events: `WhitelistEvent`, `BlacklistEvent`, `PipelineDecisionEvent`.
- New command architecture:
  - unified `/scamscreener` handler with settings, debug, list, and review/export commands.
- New GUI system:
  - shared base screens and list widgets
  - dedicated screens for settings, rules, runtime, message settings, metrics, whitelist, blacklist, and case review.
- New review workflow:
  - review queue store and entities (`ReviewStore`, `ReviewEntry`, `ReviewCaseMessage`, `ReviewVerdict`)
  - review capture/action handling and alert context registry.
- New training export stack:
  - canonical JSONL export `training-cases-v2.jsonl`
  - stable mapping IDs (`stage.*`, `reason.*`, `stage.*::reason.*`)
  - `TrainingCaseV2`, `TrainingCaseV2Mapper`, `TrainingCaseMappings`, `TrainingCaseExportService`.
- New developer tooling:
  - `scripts/auto_tune_pipeline.ps1`
  - expanded audit/docs files (`V1_RULE_AUDIT.md`, `GAP_LIST.md`, new review/training docs).
- Expanded CI/security baseline:
  - Dependabot configuration
  - server security workflow.
- New one-time v1->v2 config migration:
  - imports legacy whitelist/blacklist files on first v2 start
  - writes migration marker `.v1-to-v2-migration.done`
  - deletes legacy v1 config folder artifacts after migration.

### Removed
- Legacy local-AI training/model stack:
  - local scorer/trainer/model update services and related commands/scripts.
- Legacy command-per-file structure under `commands/`.
- Legacy parser/location/lookup/security utility stacks that v2 no longer uses.
- Legacy pipeline model (`Detection*`, `Signal*`, old scoring chain).
- Legacy GUI package and deprecated screens/routes.
- Legacy UI text/resources under `assets/scam-screener/`.
- Legacy feature-documentation tree under `docs/features/`.
- Legacy training upload flow in the mod:
  - `trainingUpload` runtime option
  - `/scamscreener review upload`.
- Legacy in-repo Training Hub server stack from release scope.

### Changed
- Packaging and namespacing cleanup:
  - resource namespace moved to `assets/scamscreener`.
- Build/publish setup refreshed for v2 release flow.
- Review persistence sanitization now removes UUID identity from exported review payloads.
- Whitelist/blacklist persistence remains UUID-capable by design.
- Training contribution buttons in GUI are intentionally disabled until Training Hub relaunch.
- Main screen now shows explicit "coming soon" Training Hub note.

### Migration Notes
- On first startup of v2, legacy files are migrated when present:
  - whitelist: `scamscreener-whitelist.json`, `scam-screener-whitelist.json`
  - blacklist: `scamscreener-blacklist.json`, `scam-screener-blacklist.json`
  - legacy text fallback: `scam-screener-blacklist.txt`.
- Search locations include `config/`, `config/scamscreener/`, `config/scam-screener/`, and `config/scamscreener-v1/`.
- Existing v2 target files are never overwritten.
