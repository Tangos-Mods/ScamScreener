# ScamScreener 1.2.1

ScamScreener 1.2.1 mainly expands the in-game review and training-data workflow.

## Highlights

### Added

- Added `/ss` as a full alias for `/scamscreener`.
- Added direct menu opening on `/scamscreener` and `/ss` (help now via `/scamscreener help` or `/ss help`).
- Added `Review Training CSV` in the mod menu next to `Upload Training Data`.
- Added full CSV review flow with `Save`, `Save & Upload`, and `Open File`.
- Added logged-chat review via `/scamscreener review` and `/ss review` for not-yet-saved chat lines.
- Added autocomplete for `/scamscreener review player <playerName>` and `/ss review player <playerName>` with online players that have logged chat entries.
- Added automatic training-data size reminders from 500+ entries (initial warning, then every 5 minutes) with clickable upload action.

### Changed

- Changed review row state display to native colored `[I]`, `[S]`, and `[L]` markers.
- Changed review help routing to `/scamscreener review help` and `/ss review help`.
- Changed CSV review editing to row-based handling so duplicate messages can be reviewed independently.

### Fixed

- Fixed CSV label save behavior so message quoting (`"..."`) is preserved.
- Fixed CSV review ignore behavior so ignored rows are removed on save.

## Compatibility

- Minecraft `1.21.10` and `1.21.11`.
- Fabric (client-side).

## Note

ScamScreener remains client-side. Detection, review, and local training data stay on your client unless you explicitly upload training data.
