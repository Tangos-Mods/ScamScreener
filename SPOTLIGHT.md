# ScamScreener Spotlight

ScamScreener is a client-side Fabric mod for Hypixel SkyBlock that warns you about likely scam behavior in chat and interaction contexts before you commit to risky actions.

## What it does for players

- Detects scam patterns in live chat with explainable warnings.
- Highlights risky players from your local blacklist in trade/party/co-op related events.
- Lets you whitelist trusted players so they are skipped by detection.
- Blocks risky outgoing content by default (email addresses, Discord invites, first `/coopadd`) and offers a one-time bypass.
- Supports optional local AI scoring to improve detection signals over time.

## How it works (short version)

- Each relevant chat message is scored through multiple local signals:
  - rules/keywords
  - similarity matching
  - behavior and trend signals
  - optional local AI probability
- If your configured threshold is reached, ScamScreener shows a warning with "why it triggered" details.
- No cloud AI is required for normal use.

## What gets processed

- Incoming player chat lines.
- Relevant game messages around trade, party, and co-op actions.
- Outgoing messages/commands for safety checks.
- Local config/model/training files under `config/scamscreener/`.

## Optional online actions

- Mojang profile lookup for name/UUID resolution and whitelist name refresh.
- AI model update checks/downloads.
- Opening a Discord invite link for manual training-data upload handoff.

## What is not uploaded automatically

- Your normal chat is processed locally for detection.
- Training data is only prepared locally and uploaded manually by you if you choose to share it.

## Quick start

- Open settings with `/scamscreener settings`.
- Review warnings in chat and use the hover details.
- Manage trusted players with `/scamscreener whitelist`.
- Use `/scamscreener review` to label and improve local AI training data.
