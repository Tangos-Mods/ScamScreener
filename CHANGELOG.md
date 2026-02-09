# Changelog
All notable changes to this project are documented in this file.

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

