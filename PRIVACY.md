# ScamScreener Privacy Overview

This document explains what data ScamScreener uses, where it is stored, and when data is sent over the network.

It is a technical transparency summary, not legal advice.

## Summary

- ScamScreener primarily runs locally on your client.
- There is no hidden telemetry or ad tracking in the mod.
- Network traffic only happens for specific features (Mojang lookup, model update checks, optional upload relay).
- Uploads are user-triggered (`/scamscreener upload` or `Save & Upload` in review UI).

## Data Processed Locally

ScamScreener processes these data types on-device:

- in-game chat lines (incoming and selected outgoing chat/commands)
- player identifiers visible in Minecraft context (for example usernames, UUID references)
- local rule/config settings
- local AI/training signals and scores

## Data Stored Locally

Files are stored under `config/scamscreener/` (profile-local):

- blacklist and whitelist entries (including player names/UUIDs you manage)
- rule and debug settings
- local AI model/config
- training dataset (`scam-screener-training-data.csv`)
- archived training/model files in `old/`
- upload relay config (`scam-screener-upload-relay.json`)

### Important note on training data

Training rows are normalized before saving (lowercased/cleaned message text + feature columns).
However, chat content can still contain sensitive information depending on what was written in chat.

## Data Sent Over Network

ScamScreener sends data only for specific features:

1. Mojang profile resolution (optional feature use)
- Endpoint family: Mojang APIs
- Purpose: resolve player name/UUID for commands and whitelist display refresh
- Sent data: target player name or UUID being resolved

2. AI model update check/download (optional feature use)
- Endpoint family: GitHub raw content
- Purpose: check/download new local model payload
- Sent data: standard HTTPS request metadata (no chat dataset)

3. Upload relay (optional, user-triggered upload flow)
- Endpoint family: your configured relay server
- Purpose: send training file to your relay, which forwards to Discord server-side
- Sent data includes:
  - relay auth/bootstrap fields (`installId`, `modVersion`, optionally invite code for redeem flow)
  - upload headers (`clientId`, `timestamp`, `nonce`, signature metadata)
  - metadata (`modVersion`, `aiModelVersion`, uploader playerName/playerUuid, client timestamp, file hash/size)
  - training CSV file content

## Security Model for Uploads

- The mod does not contain a Discord webhook.
- Uploads go through relay server endpoints.
- Requests are HMAC-signed per request (with timestamp + nonce).
- Client-side endpoint policy requires HTTPS for non-local endpoints.

## What ScamScreener Does Not Intentionally Collect

- Microsoft/Minecraft account passwords
- email inbox access or external account tokens
- payment/billing data
- background telemetry unrelated to mod features

## User Control

You can control data usage by:

- not using upload features
- clearing relay credentials via `Upload Auth -> Reset Credentials`
- deleting local mod data files in `config/scamscreener/` (including archives)

If you operate the relay server, your server-side retention and logging policies also apply to upload data.

## Clarification About "No Personal Data"

ScamScreener does not request real-world identity data by design.
But it does process game/chat data, and chat text can contain personal data if users type it.
So the accurate statement is: local-first processing with feature-limited network transfer, not "zero data processing."
