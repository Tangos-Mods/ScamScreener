# ScamScreener

[![Build](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/build.yml/badge.svg)](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/build.yml) [![Modrinth Downloads](https://img.shields.io/modrinth/dt/scamscreener?logo=modrinth&label=Modrinth%20downloads)](https://modrinth.com/mod/scamscreener) [![Version](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/.github/badges/version.json)](gradle.properties) [![AI Model](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/.github/badges/ai-model.json)](scripts/model-version.json)

Client-side Fabric mod for **Minecraft 1.21.10 / 1.21.11** that analyzes Hypixel SkyBlock chat for scam risk.

Includes a UUID-backed whitelist flow (`/scamscreener whitelist`) and review preselection that follows the configured AI auto-capture level.

**ScamScreener combines:**

- manual blacklist alerts
- rule-based detection (regex + behavior signals)
- Levenshtein similarity matching (rule phrases + training samples)
- local AI scoring (no required cloud AI service) with updateable model and community training-data upload flow
- message muting with custom patterns
- outgoing safety guard for email addresses and Discord invite links

## Why this mod?

Scams in trade/party contexts often use pressure tactics, trust manipulation, external platform pushes, or repeated contact attempts. ScamScreener scores these signals in real time and shows explainable warnings with hoverable rule details.

## What gets processed

- Incoming player chat lines (for local scam-risk detection).
- Relevant in-game interaction messages (trade, party, co-op context lines).
- Outgoing messages/commands for safety guard checks (email, Discord invites, `/coopadd` confirmation).
- Local mod config and model/training files in `config/scamscreener/`.
- Optional online checks only when related features are used (Mojang name lookup, model update check/download, optional Discord invite open).

## Features

### 1) Blacklist alerts

- Add players manually to a local blacklist (name/UUID + score + reason).
- Triggers warnings in relevant contexts:
  - incoming trade request
  - outgoing trade request
  - active trade session
  - party confirmation (`You'll be partying with: <Name>`)
  - party finder dungeon join (`Party Finder > <Name> joined the dungeon group...`)
  - co-op join request (`<Name> ... join your SkyBlock Co-op`)
  - co-op invite sent (`You invited <Name> to your co-op!`)
  - co-op member joined (`<Name> joined your SkyBlock Co-op!`)
- Optional auto `/p leave` on blacklist hit.
- Warning output can show player, score, reason, timestamp, and trigger context.

### 2) Live chat detection pipeline

Every parsed player chat line is processed through:

- `MuteStage` (early filter)
- `WhitelistStage` (early bypass for trusted players)
- `RuleSignalStage` (pattern/rule scoring)
- `LevenshteinSignalStage` (similarity signals)
- `BehaviorSignalStage` (behavior flags)
- `TrendSignalStage` (multi-message trend bonus)
- `FunnelSignalStage` (intent sequence funnel bonus)
- `AiSignalStage` (local model probability-based signal)
- `ScoringStage`, `DecisionStage` (threshold + dedupe), `OutputStage`

When thresholds are reached:

- risk warning in chat
- explainable rule list with hover details (`Why triggered`)
- 3x short warning tone (configurable)
- optional auto-capture into training data (by minimum alert level)
- quick action tags inside warnings (`manage`, `info`)
- optional follow-up education message for external-platform redirect risk (with one-click disable action)

### 3) Training data capture and upload lifecycle

- Training samples are stored in CSV.
- `/scamscreener upload` archives the active training CSV (after ToS consent), opens the archive folder, and asks whether you want to join the Discord server for manual upload.
- Existing training/model files are archived under `old/`.
- Supports sample labeling via message id (`ai flag`) and via review flows (`review`, `review player`, `review manage`).
- Reviewer auto `[S]` preselection follows the configured AI auto-capture threshold (`OFF|LOW|MEDIUM|HIGH|CRITICAL`).
- Includes training data migration (`/scamscreener ai migrate`) for older CSV headers.

### 4) AI update workflow (optional online check)

- Automatic update check once per session when connected.
- Manual checks via command or GUI (`AI Update` screen).
- Supports update download and actions: `accept`, `merge`, `ignore`.
- Uses SHA-256 verification before applying downloaded model payload.

### 5) Mute filter with custom patterns

- Add/remove mute patterns via command.
- Case-insensitive matching.
- Regex supported.
- If no regex metacharacters are present, matching defaults to whole word/phrase boundaries (not partial substrings).
- Periodic summary for blocked message counts.

### 6) Outgoing safety guard

- Blocks outgoing chat/commands containing:
  - email addresses
  - Discord invite links (`discord.gg`, `discord.com/invite`)
- Blocks `/coopadd <player>` once and requires `[BYPASS]` confirmation
- Shows a clickable `[BYPASS]` action that allows one resend for the blocked content.

### 7) In-game settings GUI

Open with `/scamscreener settings`.
If `ModMenu` is installed, the same settings screen is available via the mod menu config button.

Screens available:

- main settings (alert threshold, auto-capture level, auto-leave, mute filter, local AI signal)
- rule settings
- debug settings
- message settings
- blacklist management
- whitelist management
- AI update controls

## Requirements

- Minecraft `1.21.10` or `1.21.11`
- Fabric Loader `>= 0.18.4`
- Fabric API matching your MC version
- Java `21+`
- Optional: ModMenu (`>= 16.0.0`) for config access from mod list

## Installation

Place the mod JAR in your `mods/` folder and start the game.

## Build (Developer)

```powershell
.\gradlew.bat build
```

Artifact output: `build/libs/`

## Commands

### General

- `/scamscreener` (help)
- `/ss` (short alias for the same command tree)
- `/scamscreener add <player|uuid> [score] [reason]`
- `/scamscreener remove <player|uuid>`
- `/scamscreener list`
- `/scamscreener whitelist`
- `/scamscreener whitelist add <player>`
- `/scamscreener whitelist remove <player>`
- `/scamscreener whitelist list`
- `/scamscreener upload`
- `/scamscreener rules <list|disable|enable> [rule]`
- `/scamscreener alertlevel [low|medium|high|critical]`
- `/scamscreener autoleave [on|off]` (no args = status)
- `/scamscreener review` (open recent logged chat review)
- `/scamscreener review <playerName>`
- `/scamscreener review player <playerName>`
- `/scamscreener review <manage|info> <alertId>`
- `/scamscreener edu disable <messageId>`
- `/scamscreener settings`
- `/scamscreener debug`
- `/scamscreener debug <true|false> [updater|trade|mute|chatcolor]`
- `/scamscreener version`
- `/scamscreener preview` (dry-run preview output)

### AI

- `/scamscreener ai` (help)
- `/scamscreener ai flag <messageId> <legit|scam>`
- `/scamscreener ai migrate`
- `/scamscreener ai update`
- `/scamscreener ai update force`
- `/scamscreener ai update notify [on|off]` (no args = status)
- `/scamscreener ai model <download|accept|merge|ignore> <id>`
- `/scamscreener ai reset`
- `/scamscreener ai metrics [reset]`
- `/scamscreener ai autocapture [off|low|medium|high|critical]`

### Mute and safety

- `/scamscreener mute` (enable mute filter)
- `/scamscreener mute <pattern>`
- `/scamscreener unmute` (disable mute filter)
- `/scamscreener unmute <pattern>`
- `/scamscreener bypass <id>` (send blocked email/Discord/`/coopadd` content once)

## Configuration files

All mod files are stored under:

- `config/scamscreener/`

Important files:

- `scam-screener-blacklist.json`
- `scam-screener-whitelist.json`
- `scam-screener-rules.json`
- `scam-screener-local-ai-model.json`
- `scam-screener-training-data.csv`
- `scam-screener-mute.json`
- `scam-screener-debug.json`
- `scamscreener-edu.json`

Archive folders:

- `config/scamscreener/old/training-data/`
- `config/scamscreener/old/models/`

## Training quick guide

1. Collect labeled samples with `ai flag` and in-game review flows (`review`, `review player`, `review manage`).
2. (Optional) Refine labels in the reviewer and save selected scam/legit lines.
3. Run `/scamscreener upload`.
4. Training CSV is archived and the mod asks whether you want to join Discord to upload the file manually.
5. Observe warning quality and false positives over time.

Tips:

- Use both scam and legit examples.
- Keep labels clean; avoid system/mod status lines.
- Fewer high-quality samples are better than noisy bulk labels.

## Privacy and network behavior

- Core detection and scoring run locally on the client.
- Training and model files stay in `config/scamscreener/`.
- Optional network calls are used for:
  - Mojang profile lookup (name/UUID resolution and whitelist name refresh)
  - opening the Discord invite link for manual training-data upload (`/scamscreener upload`)
  - AI model update check/download (GitHub raw URL)

## Limitations

- Rule/keyword/similarity systems have limited semantic understanding.
- Sarcasm and ambiguous context can cause false positives.
- Aggressive mute patterns can hide legitimate chat.
- Local AI quality depends strongly on training data quality.

## License

See `LICENSE` (MIT).
