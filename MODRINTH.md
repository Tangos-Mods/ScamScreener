# ScamScreener 1.3.0

This release adds a full trusted-player whitelist workflow and makes review preselection match your Auto-Capture level.

## Added

- Added whitelist management in-game with both commands and GUI (`/scamscreener whitelist`, `add`, `remove`, `list`, plus `/ss` aliases).
- Added a dedicated whitelist config file: `config/scamscreener/scam-screener-whitelist.json`.
- Added direct review shortcut commands: `/scamscreener review <playerName>` and `/ss review <playerName>`.

## Changed

- Detection now skips whitelist players early in the pipeline (right after mute filtering), before scoring and warnings.
- Review auto-marking now follows `AI Auto-Capture` level (`OFF` disables automatic `[S]`; `LOW|MEDIUM|HIGH|CRITICAL` auto-mark only matching-or-higher risk rows).
- Whitelist display names are now refreshed toward canonical Mojang names when lookup succeeds.

## Fixed

- Fixed review row merge behavior so scored flagged/context rows keep their score signal reliably during review assembly.

## Removed

- None.
