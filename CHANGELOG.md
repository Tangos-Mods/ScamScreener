# Changelog
All notable changes to this project are documented in this file.

## [1.2.2] - 2026-02-17

### Added
- Added a centralized async utility `AsyncDispatcher` with managed lifecycle hooks, dedicated background/io/scheduled pools, and client-thread handoff helpers.
- Added configuration key `capturedChatCacheSize` in `scam-screener-rules.json` to control the in-memory captured-message cache size (default `1000`).
- Added `AsyncDispatcherTest` coverage for idempotent init/shutdown, background result handling, io task execution, and scheduling.

### Changed
- Unified async execution paths in review loading, upload ToS screen opening, training upload background work, model update check/download, and warning-tone scheduling to use `AsyncDispatcher`.
- Review cache usage now reads the configured `capturedChatCacheSize` value instead of a hardcoded limit.

### Fixed
- Reduced risk of client tick/render blocking during review-related loading by consistently moving heavy preparation and CSV review loading into managed background execution.

## [1.2.1] - 2026-02-16

### Added
- Added a dedicated player tutorial in `docs/TUTORIAL.md` with a full English usage guide.
- Added automatic AI model badge payload generation in the badge sync workflow (`.github/badges/ai-model.json`).

### Changed
- Bumped mod version from `1.2.0` to `1.2.1`.
- Unified version badge and AI badge updates in one workflow run so GitHub Actions creates a single badge-sync commit.
- Updated README AI badge to endpoint-based rendering from `.github/badges/ai-model.json`.

### Fixed
- Fixed `Open File` in Training CSV review to open the CSV with the system default app (with OS fallback when Java Desktop OPEN is unavailable).

## [1.2.0] - 2026-02-16

### Added
- Added the `/ss` command alias for the full ScamScreener command tree.
- Added direct menu opening on `/scamscreener` and `/ss`; help is now available via `/scamscreener help` and `/ss help`.
- Added `Review Training CSV` next to `Upload Training Data` in the mod menu, including a full-row CSV review flow (Save / Save & Upload).
- Added an `Open File` action in the CSV review screen to open the active training CSV directly.
- Added periodic large-dataset reminders when training data reaches at least 500 entries (initial hint + every 5 minutes) with a clickable upload action.
- Added `/scamscreener review` and `/ss review` flow for reviewing logged, not-yet-saved chat messages without blacklist/block actions.
- Added autocomplete for `/scamscreener review player <playerName>` and `/ss review player <playerName>`, filtered to online players with logged chat entries.

### Changed
- Bumped mod version from `1.1.0` to `1.2.0`.
- Moved the old review help output behind `/scamscreener review help` and `/ss review help`.
- Updated review row state rendering to use native `[I] / [S] / [L]` markers and color states directly.
- CSV review save now targets row-based updates so duplicate message texts are handled independently.

### Fixed
- Fixed CSV label updates to preserve CSV quoting/escaping behavior when saving reviewed labels.
- Fixed CSV review ignore handling so rows set to ignored are removed on save.

## [1.1.0] - 2026-02-15

### Added
- Added alert review commands: `/scamscreener review manage <alertId>`, `/scamscreener review info <alertId>`, and `/scamscreener review player <playerName>`.
- Added a dedicated review workflow with `AlertManageScreen` (message labeling + save/upload + optional blacklist/block actions).
- Added a dedicated alert details screen (`AlertInfoScreen`) with grouped trigger breakdown and captured chat context.
- Added expanded scam education follow-up messages for covered scam types (external redirect, suspicious links, upfront payment, account-data requests, fake middleman claims, urgency pressure, trust manipulation, too-good-to-be-true offers, Discord handle redirects, funnel sequence patterns).
- Added education follow-up command support with suggestions: `/scamscreener edu disable <messageId>`.

### Changed
- Bumped mod version from `1.0.1` to `1.1.0`.
- Removed deprecated training-capture commands `/1`, `/0`, `/scamscreener ai capture`, and `/scamscreener ai capturebulk`.
- Updated AI command help to point to `/scamscreener ai flag <messageId> <legit|scam>` for manual labeling.
- Scam warning output now includes centered review action tags (`[manage] [info]`) and improved warning layout alignment.
- Alert review info now groups triggered rules by detection stage (Rule, Behavior, Similarity, Trend, Funnel, AI).
- Captured message collection now merges recent captured chat with scored pipeline messages (deduplicated) for review/info screens.
- Alert-threshold migration now enforces a one-time default minimum of `MEDIUM` for existing configs that were not migrated.
- `preview` output now prioritizes current review/education messages and excludes obsolete capture/sample preview entries.

### Fixed
- Fixed review/info UI text visibility issues caused by non-opaque text color rendering.
- Fixed alert info context so scored messages from pipeline evaluation consistently appear in the captured-messages section.
- Fixed `/scamscreener review player <playerName>` not opening the review screen reliably in some cases.
- Fixed noisy review context lists by filtering empty/duplicate lines from merged message sources.

## [1.0.1] - 2026-02-15

### Added
- Added upload ToS consent flow with a dedicated `UploadTosScreen` and persisted consent flag in rules config.
- Added outgoing chat command parsing (`/msg`, `/r`, party chat, guild chat, `ac`) plus parser tests to capture local outgoing text context.
- Added normalized temporary upload file generation (`message,label`) before Discord training-data upload.
- Added `scripts/publish-modrinth-from-env.ps1` to load `.env` and publish all StoneCutter `publishModrinth` targets.

### Changed
- Bumped mod version from `1.0.0` to `1.0.1`.
- Discord upload embed now uses player header/avatar metadata, SHA-256 in a code block, and a single `Version | AI` line.
- Replaced `/scamscreener ai train` with `/scamscreener upload`; the menu `Upload Training Data` action now uses the same upload handler.
- Training upload now gates execution behind ToS acceptance and archives only the active training CSV before webhook submission.
- Training capture now records outgoing chat and supported outgoing chat commands into the recent training context buffer.
- `/scamscreener version` now reads both mod and AI version through shared `VersionInfo`.

### Fixed
- Upload no longer falls back to `config/scamscreener/old/training-data/*.old.*` when `config/scamscreener/scam-screener-training-data.csv` is missing.
- Menu-triggered upload follows the same active-file requirement as `/scamscreener upload` and therefore cannot upload stale archived files.

## [1.0.0] - 2026-02-13

### Added
- Added `scripts/train-model.ps1` as a local model-training wrapper for `train_local_ai.py` with Python auto-detection, `scikit-learn` precheck, configurable paths, and optional `-BumpModel`.
- Added `.env.example` with a `MODRINTH_TOKEN` template.
- Added an `Upload Training Data` action in `MainSettingsScreen` and wired it through `ScamScreenerClient` to `TrainingCommandHandler`.
- Added upload guidance messaging in `Messages.trainingUploadToDiscord(...)` with clickable local folder path and clickable SkyblockEnhanced Discord invite.
- Added click helper methods in `MessageBuilder` for containing-folder file links and URL links.
- Added UI test coverage for training upload messaging and generalized click-event value extraction in `MessagesTest`.

### Changed
- Bumped mod version from `0.16.7` to `1.0.0`.
- Local `/scamscreener ai train` flow now defaults to archiving local training CSV and prompting Discord upload guidance (instead of directly training on-client).
- Modrinth publish changelog source now reads from `MODRINTH.md` instead of `CHANGELOG.md`.
- Stonecutter version order now prioritizes `1.21.11` as first listed target in `settings.gradle.kts`.
- Updated `.gitignore` with local env/model entries (`.env`, `funnel.csv`, `/.gradle-user`).

### Fixed
- Blacklist warning banner header now correctly shows `BLACKLIST WARNING` (instead of `SCAM WARNING`).

## [0.16.7] - 2026-02-11

### Added
- Added funnel behavior/regression tests in `FunnelSignalStageTest` for no-funnel, partial-chain, and full-chain scenarios.
- Added updater version-comparison test coverage in `ModelUpdateServiceHashTest`.
- Added funnel coverage for an upfront-payment variant (`OFFER -> PAYMENT`) with automated tests.
- Added a ready-to-train `funnel.csv` sample in the current training-data CSV schema.

### Changed
- Local AI trainer now preserves the current model version when retraining instead of forcing a static version value.
- Updated Funnel TODO and test documentation to match the current funnel architecture and new automated coverage.
- `IntentTagger` now recognizes upfront-payment phrasing directly from message text (not only from upstream signals).
- Model release metadata bumped to version `14` with refreshed SHA-256 in `scripts/model-version.json`.

### Fixed
- Model updater now treats remote models with equal or older model version as up-to-date and skips unnecessary update prompts.
- Funnel detection and AI funnel context now also handle the upfront-payment funnel path (`OFFER -> PAYMENT`).

## [0.16.5] - 2026-02-11 (HOTFIX)

### Fixed
- Fixed false `sha256 mismatch` errors when downloading AI model updates by tolerating equivalent UTF-8 payload variants (LF/CRLF, BOM/no BOM).
- Fixed model release hash generation so `scripts/model-version.json` SHA-256 matches GitHub raw model payload bytes.

## [0.16.x] - 2026-02-10 to 2026-02-11

### Added
- New `FunnelSignalStage` with per-player sequence tracking (ring buffer + TTL) and funnel evidence in warnings.
- Intent tagging layer (`IntentTagger`) with tags for offer/rep/redirect/instruction/payment/community anchor.
- Obfuscation-aware redirect detection for variants like spaced or leetspeak platform mentions.
- Configurable funnel settings and intent patterns in rules config (window size/time, weights, negative-intent patterns).
- Channel-aware message context parsing (`pm`, `party`, `team`, `public`) for pipeline and training features.
- New AI feature stack with dense feature space + funnel context tracker (`AiFeatureSpace`, `AiFunnelContextTracker`).
- Expanded training CSV schema with conversation/window and funnel-aware fields (`window_id`, intent fields, stage hits, funnel metrics, hard negatives, sample weights).
- Additional AI rule signal: `LOCAL_AI_FUNNEL_SIGNAL` plus dedicated funnel tuning config (`localAiFunnelMaxScore`, `localAiFunnelThresholdBonus`).
- Warning action tag `[block]` to run `/block <player>` from risk messages.
- Model update metadata now includes model schema version in `scripts/model-version.json`.

### Changed
- Detection pipeline order updated so Funnel runs before AI scoring, allowing the model to consume live funnel context.
- Local AI model format and training were reworked to support dense+token scoring and a dedicated funnel head (`funnelHead`) in the same model file.
- Local AI trainer now trains main and funnel heads from the same CSV (funnel labels inferred from funnel markers).
- Auto-captured training samples now persist full detection-aware feature rows instead of plain text-only rows.
- Local AI scoring can emit multiple AI signals in one pass (general + funnel-focused).
- Funnel-AI trigger threshold and max score are sourced from `ScamRulesConfig` (no hardcoded trigger values).
- Model updater validates schema and preserves/normalizes funnel-head weights on apply/merge.
- Model version update script validates schema before calculating SHA and publishing metadata.
- Local AI model release metadata was bumped to version `13` (schema `10`) with refreshed SHA-256 in `scripts/model-version.json`.
- Warning action tags use direct one-click command execution (`[legit]`, `[scam]`, `[blacklist]`, `[block]`).
- Internal player tracking/training/debug flow now uses anonymized speaker keys and anonymized training capture context.

### Removed
- Removed legacy training flag pipeline (`TrainingFlags`, `TrainingTypeAiClassifier`).
- Removed legacy local-model migration path from root config for AI model loading.

### Fixed
- System-message filtering was tightened for party/trade/co-op lines so they stay out of detection/training.
- Chat lines starting with `[NPC]` are treated as system messages and excluded from player detection/training capture.

### Kept
- Risk warning messages still show the real player name for in-game risk message context.

## [0.15.3] - 2026-02-09

### Added
- AI updater join-notify controls via `/scamscreener ai update notify [on|off]` (no args = status) and the AI update menu toggle (`Join Up-to-Date Message`).
- Coop blacklist warning coverage expanded for co-op join request, `You invited <name> to your co-op!`, and `<name> joined your SkyBlock Co-op!`.
- Coop safety flow for `/coopadd <player>` with `[BYPASS]` confirmation.
- Location backbone service for scoreboard-based island detection.
- `SkyblockIsland` enum with island names and alias matching.
- ModMenu integration as soft dependency with config screen support from mod list.

### Changed
- `autoleave` command behavior updated: no args now shows status; explicit toggle remains `on|off`.
- `ai update notify` behavior updated: no args now shows status; explicit toggle remains `on|off`.
- `/coopadd` is now always blocked first and requires explicit `[BYPASS]`.
- Mod contact metadata now defines an issues URL so the ModMenu Issues button opens https://github.com/Tangos-Mods/ScamScreener/issues/new.

### Fixed
- Blacklist warning lookup now falls back to name-based entries when UUID lookup is unavailable.
- Coop blacklist bypass message formatting now uses the shared `MessageBuilder` style.
- ModMenu config-screen implementation fixed (correct `Screen` return type in the config factory).

## [0.14.7] - 2026-02-09

### Added
- New GUI system exposed via `/scamscreener settings` (`SettingsCommand` + `MainSettingsScreen`).
- Dedicated submenus for `Rules`, `Debug`, `Messages`, `Blacklist`, and `AI Update`.
- New shared GUI base class (`GUI.java`) to centralize common screen behavior.
- AI update menu actions: `Accept`, `Merge`, `Ignore`, plus `Force Check`.
- New `MessageBuilder` base class for centralized message/text styling helpers.

### Changed
- Blacklist GUI expanded with paging, selection, score `+10/-10`, remove, reason editing, rule dropdown, and save action.
- Blacklist GUI row styling aligned with chat blacklist display colors.
- Colored ON/OFF display in settings (`ON` light green, `OFF` light red).
- Scam warning and blacklist warning output/sound are now independently configurable via rules config.
- Auto-leave info message can now be toggled independently.
- `ModelUpdateService` now exposes a pending snapshot for GUI status (`latestPendingSnapshot`).

### Removed
- Keybind system fully removed (`Keybinds`, `FlaggingController`, legacy keybind language keys).
- `ErrorMessages` removed; error construction consolidated into `MessageBuilder`.

### Fixed
- Screen rendering stabilized (prevents blur crash: "Can only blur once per frame" via custom GUI background rendering).
- Improved null/state handling in AI update and blacklist UI flows.

### Refactor
- Shared screen logic centralized (layout, footer buttons, toggle line formatting, screen navigation).
- `Messages` and `DebugMessages` migrated to shared builder helpers (less duplication, no bridges).
- `ClientTickController` simplified and moved to a settings-open request flow.
