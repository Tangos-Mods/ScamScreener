# ScamScreener 1.0.0

ScamScreener 1.0.0 is the first major stable release for Hypixel SkyBlock players.
It helps you detect scam patterns in chat early and react quickly.

## What You Get As A Player

- Live scam warnings in chat with risk levels and clear trigger details.
- Local blacklist warnings in relevant contexts (for example trade, party, and co-op).
- Quick action buttons in warnings (`legit`, `scam`, `blacklist`, `block`).
- Optional auto-leave from parties when a blacklist hit is detected.
- Custom mute filter (including regex) to hide scam spam immediately.
- Outgoing safety guard for risky content (email, Discord invites, `/coopadd`) with controlled bypass.
- In-game settings GUI (`/scamscreener settings`) for fast configuration.
- Local AI model with update workflow (download/accept/merge/ignore), including SHA-256 integrity checks.

## New In 1.0.0

- Full support for Minecraft `1.21.11` and `1.21.10`.
- New community training flow: `/scamscreener ai train` archives your local training data and guides you to upload it for community model training.
- New `Upload Training Data` button in the settings menu.
- Upload guidance now includes a clickable local folder path and clickable SkyblockEnhanced Discord link.
- Blacklist warning header was fixed (`BLACKLIST WARNING`).

## Quick Commands

- `/scamscreener settings`
- `/scamscreener add <player> [score] [reason]`
- `/scamscreener ai update`
- `/scamscreener ai train`
- `/scamscreener mute <pattern>`

## Note

ScamScreener is client-side. Core detection, scoring, and local data stay on your client.
