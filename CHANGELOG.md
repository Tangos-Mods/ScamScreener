# Changelog
All notable changes to this project are documented in this file.

## [0.16.7] - 2026-02-11

### Added
- Added funnel coverage for an upfront-payment variant (`OFFER -> PAYMENT`) with automated tests.
- Added a ready-to-train `funnel.csv` sample in the current training-data CSV schema.

### Changed
- `IntentTagger` now recognizes upfront-payment phrasing directly from message text (not only from upstream signals).
- Model release metadata bumped to version `14` with refreshed SHA-256 in `scripts/model-version.json`.

### Fixed
- Funnel detection and AI funnel context now also handle the upfront-payment funnel path (`OFFER -> PAYMENT`).

## [0.16.6] - 2026-02-11

### Added
- Added funnel behavior/regression tests in `FunnelSignalStageTest` for no-funnel, partial-chain, and full-chain scenarios.
- Added updater version-comparison test coverage in `ModelUpdateServiceHashTest`.

### Changed
- Local AI trainer now preserves the current model version when retraining instead of forcing a static version value.
- Updated Funnel TODO and test documentation to match the current funnel architecture and new automated coverage.

### Fixed
- Model updater now treats remote models with equal or older model version as up-to-date and skips unnecessary update prompts.

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
