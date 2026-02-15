# ScamScreener 1.1.0

ScamScreener 1.1.0 improves in-game review workflows and expands scam education coverage.

## Highlights

### Added

- Added review commands: `/scamscreener review manage <alertId>`, `/scamscreener review info <alertId>`, `/scamscreener review player <playerName>`.
- Added a dedicated Manage Alert workflow with multi-line labeling (`Ignore -> Scam -> Legit`), save/upload flow, and optional blacklist/block actions.
- Added a dedicated Alert Rule Details screen with grouped trigger stages (`Rule`, `Behavior`, `Similarity`, `Trend`, `Funnel`, `AI`) and captured context.
- Added expanded education follow-up messages for external redirects, suspicious links, upfront payment requests, account-data requests, fake middleman claims, urgency pressure, trust manipulation, too-good-to-be-true offers, Discord redirects, and funnel patterns.
- Added education message control command: `/scamscreener edu disable <messageId>`.
- Added Metrics in Menu for some information

### Changed

- Changed scam warning actions to centered review tags (`[manage] [info]`) and improved warning layout alignment.
- Changed review/info context assembly to merge recent captured lines with evaluated pipeline lines (normalized and deduplicated).
- Changed default alert-threshold migration behavior to enforce a one-time minimum of `MEDIUM` for older unmigrated configs.

### Removed

- Removed deprecated training-capture commands `/1`, `/0`, `/scamscreener ai capture`, and `/scamscreener ai capturebulk`.

### Fixes

- Fixed alert info context so scored pipeline messages are consistently shown in captured-message context.

## Compatibility

- Minecraft `1.21.11` and `1.21.10`.

## Note

ScamScreener is client-side. Detection, scoring, and local review/training data stay on your client unless you explicitly upload training data.
