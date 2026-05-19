# Changelog

All notable changes to this project are documented in this file.

## 2.4.1 - 2026-05-19

### Added
- Periodic local Training Hub upload reminder that appears once more than 5 reviewed `SAFE`/`RISK` cases are saved and repeats every 30 minutes.
- Clickable reminder actions for `[upload]` and `[don't show again]`.
- Review-settings toggle for enabling or disabling the Training Hub upload reminder.

### Changed
- The in-game Training Hub screen now shows the full local client ID and lets the player copy it with one click.
- Manual Training Hub uploads now postpone the next reminder window instead of allowing an immediate repeat reminder.
- Client command help now includes the new `training upload` and `training reminder` actions.

## 2.4.0 - 2026-05-13

### Added
- Anonymous Training Hub upload integrity handshake via SHA-256 payload and client-id hash headers.
- Optional local Training Hub integration test against `http://128.0.0.1/` that skips automatically when the local test site is unavailable.

### Changed
- Removed the old in-mod ScamScreener account login flow for Training Hub uploads.
- Training Hub uploads now authenticate directly with the installation-local `trainingClientId` instead of an in-memory user session.
- The in-game Training Hub screen is now a minimal anonymous upload UI that shows the local client ID, export file path, and reviewed case count.
- Main-screen, review-screen, and documentation text now describe the anonymous `trainingClientId` upload flow and later web-side account linking.

### Removed
- Legacy Training Hub username/password inputs, login/logout buttons, and runtime upload session handling from the mod.

## 2.3.4 - 2026-04-29

### Changed
- Rule and funnel regex checks now reuse cached pattern matches per chat event instead of repeating the same regex work across stages.

### Fixed
- Inbound chat callbacks that only expose the sender through Fabric message params now still enter the scam pipeline as `PLAYER` chat messages.

## 2.3.3 - 2026-04-21

### Fixed
- Education follow-up messages now link to the moved Community Wiki scams page at `https://hypixelskyblock.minecraft.wiki/w/Scams`.
- The education help hover text no longer references Fandom and now labels the destination as `Community Wiki`.

## 2.3.2 - 2026-04-19

### Added
- Blacklist entries added by player name now resolve the player's UUID asynchronously in the background and persist it once the lookup succeeds.

### Changed
- Runtime-backed API queries now return the current state when called again instead of exposing only the initialization state.
- Returned API list and entry values remain snapshots, so companion mods must query again to observe later changes.

## 2.3.1 - 2026-04-09

### Fixed
- Removed the published `MarketGuard` dependency metadata again so `MarketGuard` can depend on `ScamScreener` without a circular Fabric dependency.

## 2.3.0 - 2026-04-05

### Added
- `MarketGuard` is now declared as a required dependency for supported Fabric releases.

## 2.2.1 - 2026-03-30

### Added
- Integrated the Training Hub directly into the main screen and review screen.
- In-game ScamScreener account login and upload flow for reviewed `SAFE` and `RISK` cases.
- Stable per-installation `trainingClientId` handling for deterministic exported case IDs.
- Restored education follow-up messages and the `/scamscreener edu disable <messageId>` command.
- Added tests for Training Hub session parsing, education follow-up handling, and deterministic training export IDs.

### Changed
- Training export now writes canonical LF-based JSONL output so duplicate detection stays consistent across systems.
- Training Hub now explains in-game data handling more clearly, including password, session, and local reviewed-case behavior.
- Main and review contribution actions now open the in-game Training Hub instead of the old external-link flow.
- Training Hub version display now resolves from the actual Fabric mod metadata.

### Fixed
- Training Hub form labels and inputs no longer overlap.
- Placeholder text in Training Hub fields no longer stays on top of typed input.
- The warning about Minecraft credentials is now highlighted in red.

## 2.1.3 - 2026-03-26

### Added
- Runtime loading of external `PipelineContributor` stages from the `scamscreener-pipeline` Fabric entrypoint.
- Direct `Review Case` button on the `Alert Rule Details` screen.

### Changed
- Risk warning action labels now show `[review]` instead of `[manage]` while keeping the existing `/scamscreener review manage <alertId>` command path for compatibility.
- Versioned config loading now uses a smaller direct store implementation instead of the older generic migration wrapper.
- Internal review, list, and pipeline-state code was simplified by removing unused wrappers and dead abstraction layers.
- `1.21.10` release metadata now uses the `_EOL` suffix in the publish title.

### Fixed
- The public pipeline extension API is now actually wired into runtime pipeline construction instead of existing only as an exposed contract.
- Alert detail flow no longer requires going back through the review queue before opening the case editor.

## 2.1.0 - 2026-03-13

### Added
- Live client-thread profiler HUD with `/ss profiler`, `/ss profiler on`, and `/ss profiler off`.
- Optional Tango Web API integration for `/ss profiler open`.
- Clickable missing-dependency message that points users to Tango Web API on Modrinth when the web profiler is requested without the mod installed.
- Browser profiler page with rolling metrics, lifetime averages, phase breakdown, recent event log, reset action, and connection-state indicators.
- `.sspp` (`scamscreener performance profile`) export from the web profiler for developer analysis.
- Profiler instrumentation around inbound chat callbacks, message classification, pipeline stages, decision dispatch, review capture, client messages, sounds, mute filtering, and queued UI work.
- Metrics screen toggle for the profiler HUD.
- Public runtime settings API for other mods covering `pingOnRiskWarning`, `pingOnBlacklistWarning`, and `alertMinimumRiskLevel`.
- Public config schema API for exposing the current runtime/rules/whitelist/blacklist/review config versions to companion mods.
- Dedicated developer API guide in `API.md` with integration examples for Fabric mods.

### Changed
- The web profiler phase breakdown now uses lifetime averages since profiler start instead of rebuilding rows from a short-lived rolling window.
- Web profiler refresh interval is now `1000 ms`, and the `Recent Event` / `Phase Breakdown` panels use fixed-height scroll regions.
- Profiler disable now hard-stops recording and clears retained samples and recent profiler events.
- Config persistence now runs through a shared single-threaded async file worker.
- Training export now starts immediately and finishes in the background.
- Client-thread message processing does less duplicate work through earlier ingress filtering, single-pass visible-line classification, cached normalized/similarity/fingerprint fields on `ChatEvent`, sender-indexed recent-chat lookups, and fingerprint-indexed trend lookup.

### Fixed
- Senderless `CHAT` messages from other mods are classified before pipeline entry instead of being treated as player chat.
- Local ScamScreener warning echoes no longer re-enter the chat listener and null out the current pipeline decision.
- Metrics/profiler screen layout overlap around the profiler controls.
- Web profiler connection state and lifetime summary behavior when the profiler is disabled.
- Additional false processing and small lag spikes from system-style or mod-generated chat lines, especially on noisier SkyBlock screens.

## 2.0.2 - 2026-03-10

### Added
- `/ss enable` and `/ss disable` commands to toggle ScamScreener without removing the mod.
- Join-time disabled notice with a clickable re-enable action.
- Join-time Modrinth update notification with clickable link and changelog hover preview.
- Manual chat inspection harness for quickly checking how pasted chat lines are classified.
- Central config schema registry and migration documentation under `config.migration`.

### Fixed
- Only validated player messages enter the scam pipeline; system, unknown, and ignored lines are skipped.
- Mod/system-prefixed lines such as `[Skyblocker] ...` no longer trigger risk checks.
- Invalid player-like formats such as `[VIP] Sam: hi` or `From: Server Team: ...` are ignored instead of analyzed.
- Public player chat parsing now accepts an optional emblem token between level/rank metadata and the player name.
- Reduced false positives from bare `call` and standalone urgency signals in the rule/similarity stack.

### Changed
- Default visible alert threshold is now `MEDIUM` instead of `LOW`.
- Config schema versions are now managed centrally via `ConfigSchema`.
- Older or unversioned v2 config files are replaced with the current default config on load.

### Migration Notes
- Legacy v1 whitelist/blacklist import still runs once before normal config loading.
- Imported v1 whitelist/blacklist data is written as current-version v2 config, so it survives the strict version gate.
- Existing old or unversioned v2 files do not merge forward; they are replaced by current defaults.

## 2.0.1 - 2026-03-09

### Fixed
- Hardened chat classification so system-style messages are ignored correctly and no longer produce false alerts.

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
