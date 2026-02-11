# TODOs

## Done

### Messages in AI Updater
- [x] Command to activate/deactivate notification on server join that AI model is up to date
- [x] Status shown when no argument is provided
- [x] Added a button in AI menu to toggle ON/OFF

### Coop Join Warning
- [x] Add blacklist warning when a player asks to join your coop
- [x] Add security like for Email and Discord with bypass block for `/coopadd`
- [x] Send warning when you invite a blacklisted player (`You invited <name> to your co-op!`)
- [x] Send additional warning when blacklisted player joined (`<name> joined your SkyBlock Co-op!`)
- [x] `/coopadd` is always blocked first and requires `[BYPASS]`

### Location Backbone
- [x] Grab the information of the current location via the scoreboard
- [x] Added `LocationService` and `SkyblockIsland` enum with case-insensitive matching

### ModMenu https://modrinth.com/mod/modmenu
- [x] Implemented as soft dependency
- [x] Added ModMenu config entrypoint to open ScamScreener settings

### FunnelSignalStage (Context via Sequences)
- [x] Add `FunnelSignalStage` as a stateful, per-player stage (ringbuffer + TTL)
- [x] Run it late in the pipeline (after normalization/regex/similarity, before scoring/decision)
- [x] Store per-player last N messages (e.g. 20) with timestamps + channel (PM/Public/Party)
- [x] Derive `IntentTags` per message using existing signals (regex + similarity):
  - SERVICE_OFFER / FREE_OFFER
  - REP_REQUEST
  - PLATFORM_REDIRECT (Discord/VC/links)
  - INSTRUCTION_INJECTION (go to #, type, do rep @, etc.)
  - PAYMENT_UPFRONT (optional)
  - COMMUNITY_ANCHOR (sbz/hsb/sbm etc., optional)
- [x] Implement sequence scoring (time-bounded):
  - OFFER -> REP -> REDIRECT -> INSTRUCTION
  - Partial funnels (REP+REDIRECT, REDIRECT+INSTRUCTION) should still raise risk
- [x] Emit funnel evidence for UI:
  - steps + minimal snippets (only messages that contributed)
  - funnelScoreDelta + funnelLevel mapping (optional)

### Intent Tagging (Reuse Existing Stages)
- [x] Implement `IntentTagger` that consumes existing stage outputs (no duplicate heavy parsing)
- [x] Add obfuscation folding (e.g. `d i s c o r d`, `disc0rd`, `d!scord`) using your similarity/levenshtein utilities
- [x] Move intent keywords/patterns into config (easy tuning without code changes)
- [x] Add negative intent patterns to reduce false positives (guild recruiting, legit service ads)

### Local Training Improvements (Keywords -> Better Model, Less Noise)
- [x] Expand training data format to support:
  - label per message AND label per conversation window
  - SAFE / BENIGN examples (critical for reducing false positives)
- [x] Save additional features per training row:
  - intentTags
  - stageHits (which regex/similarity matched)
  - channel + time delta since last message
  - current funnel step index (if any)
- [x] Add "hard negative" training:
  - messages that look suspicious but are confirmed benign (e.g., guild recruiting with discord)
- [x] Add "dedupe & weighting":
  - downweight repeated spam lines
  - avoid overfitting to one player's phrasing

### Performance & Storage
- [x] Ensure per-message complexity stays O(window_size)
- [x] Implement TTL cleanup for player contexts (memory cap)
- [x] Add config toggles: enable/disable FunnelStage, set window size, set time limits

## Open

### Karma System
- [ ] implement a Karma System, reward players that are being nice to you
> Check for Hypixel Rules to use such a system as it might be against the rules to keep track of players behavior

### Dungeon Death Tracker
- [ ] Keep track of People often dying in Dungeons. store them in a file with reason and count it up everytime they die
- [ ] Also fetch from Hypixel API the number of runs the players in party did to calculate a Ratio
- [ ] send warning if a Player that dies often is in your dungeon party. optional auto-leave
- [ ] add this festure to the Settings Menu

# Funnel Stage

## Evaluation & Regression Safety
- [x] Add behavior tests for funnel detection (FunnelStore + IntentTagger + pipeline integration):
  - benign service offer only => low/no funnel
  - discord mention only => low
  - rep request + discord + instruction => high
  - full chain => critical
- [x] Add regression tests for common false positives:
  - guild recruiting posts (negative intent should suppress offer/free funnel steps)
  - legit carry ads without redirect/instruction
- [ ] Add local funnel metrics aggregation (beyond per-message evidence strings):
  - false positive rate (user-marked)
  - funnel detection rate
  - threshold boundary cases (uncertain decisions)

## Performance & Storage
- [ ] Harden stored training logs for privacy:
  - keep hashed speaker keys (already done)
  - redact/hash training message text before storage (currently only normalized)
