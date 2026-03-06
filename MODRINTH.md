# ScamScreener 2.0.0

ScamScreener v2 is a full rewrite of the old v1 line.
The release focuses on better scam-signal detection, stronger review workflows, and cleaner training exports.

## Highlights

- New modular detection pipeline (rule, similarity, behavior, trend, funnel, context).
- New review queue flow with better case handling in GUI.
- Canonical export format for training: `training-cases-v2.jsonl`.
- Improved command and settings structure under one unified `/scamscreener` command tree.
- Broader automated test coverage and release hardening.

## Important For Players Updating From v1

- Legacy v1 whitelist/blacklist is migrated automatically on first v2 startup.
- Old v1 config artifacts are cleaned up after migration.
- Training Hub is not live yet; `Contribute Training Data` buttons are intentionally disabled.
- Case export still works via command: `/scamscreener review export`.

## Command Surface (v2)

- `/scamscreener settings`
- `/scamscreener review`
- `/scamscreener review export`
- `/scamscreener whitelist ...`
- `/scamscreener blacklist ...`
- `/scamscreener rules`
- `/scamscreener runtime`
- `/scamscreener metrics`
- `/scamscreener messages`
- `/scamscreener debug ...`

## Notes

- This release replaces major parts of v1 internals.
- If you run custom tooling around old v1 scripts/paths, update those integrations to the v2 layout.
