# ScamScreener 1.0.1

ScamScreener 1.0.1 improves the training-data upload flow and keeps the command/menu behavior consistent.

## Highlights In 1.0.1

- Full support for Minecraft `1.21.11` and `1.21.10`.
- `/scamscreener ai train` was replaced by `/scamscreener upload`.
- Upload now requires the active file `config/scamscreener/scam-screener-training-data.csv`.
- Upload no longer falls back to `config/scamscreener/old/training-data/*.old.*`.
- Upload is gated behind a one-time ToS consent screen.
- Training capture now also includes outgoing chat command text (`/msg`, `/r`, party, guild, `ac`).

## What You Get As A Player

- Live scam warnings in chat with risk levels and clear trigger details.
- Local blacklist warnings in relevant contexts (for example trade, party, and co-op).
- Quick action buttons in warnings (`legit`, `scam`, `blacklist`, `block`).
- Optional auto-leave from parties when a blacklist hit is detected.
- Custom mute filter (including regex) to hide scam spam immediately.
- Outgoing safety guard for risky content (email, Discord invites, `/coopadd`) with controlled bypass.
- In-game settings GUI (`/scamscreener settings`) for fast configuration.
- Local AI model with update workflow (download/accept/merge/ignore), including SHA-256 integrity checks.

## Quick Commands

- `/scamscreener settings` Open Settings Menu
- `/scamscreener add <player> [score] [reason]` Add Player to Blacklist
- `/scamscreener ai update` Check for Model Update
- `/scamscreener upload` Upload your Training Data
- `/scamscreener mute <pattern>` Mute messages with patterns

## Note

ScamScreener is client-side. Core detection, scoring, and local data stay on your client.
