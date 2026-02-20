# ScamScreener 1.3.1

This version keeps the whitelist/review improvements and switches training-data upload to a manual Discord handoff flow.

The whitelisted users are ignored by ScamScreener.

## Added

- Added whitelist management in-game with both commands and GUI (`/scamscreener whitelist`, `add`, `remove`, `list`, plus `/ss` aliases).
- Added a dedicated whitelist config file: `config/scamscreener/scam-screener-whitelist.json`.
- Added direct review shortcut commands: `/scamscreener review <playerName>` and `/ss review <playerName>`.
- Added a manual upload handoff message that asks to join Discord and includes clickable invite/folder links.

## Changed

- Detection now skips whitelist players early in the pipeline (right after mute filtering), before scoring and warnings.
- Review auto-marking now follows `AI Auto-Capture` level (`OFF` disables automatic `[S]`; `LOW|MEDIUM|HIGH|CRITICAL` auto-mark only matching-or-higher risk rows).
- Whitelist display names are now refreshed toward canonical Mojang names when lookup succeeds.
- `/scamscreener upload` now archives training data and prompts manual Discord upload instead of webhook upload.
- Upload ToS and README upload/network documentation now reflect player-controlled manual upload behavior.

## Fixed

- Fixed review row merge behavior so scored flagged/context rows keep their score signal reliably during review assembly.
- Fixed upload status messaging so it no longer implies automatic webhook submission.

## Removed

- Removed Discord webhook uploader code and related Discord webhook integration workflows/tests
