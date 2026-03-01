# ScamScreener Player Tutorial

This tutorial explains how to use ScamScreener in normal gameplay, step by step.

It is written for players (not developers) and focuses on practical in-game usage:

- how to react to warnings
- how to review and label chat lines
- how to manage your blacklist
- how to upload training data correctly
- how to configure the mod for your playstyle

---

## 1. What ScamScreener Does

ScamScreener is a client-side Fabric mod for Hypixel SkyBlock chat safety.

It can:

- detect suspicious chat patterns
- warn you when risk is high
- help you review messages manually
- maintain a local blacklist
- collect local training data for model improvements

Important:

- It runs locally on your client.
- It does not auto-ban players.
- It does not replace your own judgment.

---

## 2. First Start (2 Minutes)

1. Join a server where ScamScreener is active (for example Hypixel SkyBlock).
2. Open chat and run:
   - `/ss`
3. The main ScamScreener settings menu opens.

`/ss` is a full alias of `/scamscreener`.

If you prefer the long form, all commands also work with `/scamscreener`.

---

## 3. Quick Start Setup (Recommended)

Open `/ss` and configure these first:

1. `Alert Threshold`
   - Start with `MEDIUM` for balanced warnings.
2. `Review Auto-Capture`
   - Start with `OFF` or `HIGH` if you want conservative automatic sample capture.
3. `Auto /p leave on blacklist`
   - Enable if you want instant party leave when a blacklisted player is detected.
4. `Mute Filter`
   - Enable only if you already use mute patterns.
5. `Local Model Signal`
   - Keep enabled unless you are troubleshooting.

---

## 4. Understanding Warning Messages

When ScamScreener detects risk, you will see a warning message in chat.

Typical warning content:

- player name
- risk level/score
- action tags: `[manage]` and `[info]`

What to do:

1. Click `[info]` to inspect why the alert triggered.
2. Click `[manage]` to review and label relevant lines.

You may also see a `[BYPASS]` action when your own outgoing message is blocked for safety reasons (email, Discord invite, risky coop add flow).

---

## 5. Review Workflows (Core Feature)

ScamScreener has multiple review modes.

### 5.1 Alert Review (`[manage]`)

Use this after a live warning.

In the review screen:

- click a row to cycle its state:
  - `[I]` = ignored
  - `[S]` = scam
  - `[L]` = legit
- top summary shows selected counts
- `Save` writes selected labels
- `Save & Upload` writes labels, then runs upload flow

In this alert-based mode, two optional checkboxes are available:

- add player to ScamScreener blacklist
- add player to Hypixel `/block` list

### 5.2 Logged Chat Review (`/ss review`)

Run:

- `/ss review`

This opens a review screen for logged chat lines that are not saved yet.

Use it to label chat even if no alert tag is currently visible.

Behavior:

- same `[I]/[S]/[L]` row interaction
- no blacklist/block checkboxes (no player-name action mode)
- `Save` / `Save & Upload` available

Help for legacy review command usage:

- `/ss review help`

### 5.3 Player-Specific Review (`/ss review player <playerName>`)

Run:

- `/ss review player <playerName>`

Autocomplete is available and filtered:

- player must be online
- player must have logged chat entries

Use this when you want to focus on one specific player's recent conversation context.

### 5.4 Training CSV Review (Menu)

Open:

1. `/ss`
2. click `Review Training CSV`

This screen shows existing rows from your training CSV directly.

Actions:

- `Open File` opens the CSV file in your operating system
- `Save` applies selected changes
- `Save & Upload` applies changes and starts upload flow

Important CSV behavior:

- duplicate messages are handled row-by-row
- if a row is set to ignored and saved, that row is removed

---

## 6. Training Data and Upload

### 6.1 Where training data is stored

Main file:

- `config/scamscreener/scam-screener-training-data.csv`

### 6.2 Labeling paths

You can create labels through:

- alert manage review
- `/ss review` (logged unsaved lines)
- training CSV review screen
- advanced command path: `/ss model flag <messageId> <legit|scam>`

### 6.3 Upload flow

Run:

- `/ss upload`

Or use:

- `Upload Training Data` button in `/ss` menu
- `Save & Upload` in review screens

Upload behavior:

- first upload asks for ToS confirmation
- data is archived before upload
- webhook upload starts after local preparation

### 6.4 Large file reminder

If your active training CSV reaches at least 500 entries:

- you get a reminder immediately
- then every 5 minutes while size stays >= 500
- reminder includes a clickable upload action

---

## 7. Settings Menu Guide

Open with:

- `/ss`

### 7.1 Main buttons

- `Alert Threshold`
- `Review Auto-Capture`
- `Auto /p leave on blacklist`
- `Mute Filter`
- `Local Model Signal`

### 7.2 Secondary screens

- `Rule Settings`: enable/disable individual detection rules
- `Message Settings`: toggle warning messages/sounds
- `Blacklist`: edit entries, score, reason, remove, paging
- `Model Update`: check/download and apply model update (`Accept`, `Merge`, `Ignore`)
- `Metrics`: local funnel metrics summary + copy button
- `Debug Settings`: advanced diagnostics (usually keep off)

### 7.3 Training actions in menu

- `Review Training CSV`
- `Upload Training Data`

---

## 8. Command Reference (Player-Focused)

You can use either `/ss ...` or `/scamscreener ...`.

### 8.1 Daily commands

- `/ss` -> open settings menu
- `/ss help` -> command overview
- `/ss review` -> review logged unsaved chat lines
- `/ss review help` -> review command help
- `/ss upload` -> archive + upload training data
- `/ss list` -> show blacklist entries
- `/ss add <player> [score] [reason]` -> add/update blacklist entry
- `/ss remove <player>` -> remove blacklist entry

### 8.2 Review commands

- `/ss review manage <alertId>`
- `/ss review info <alertId>`
- `/ss review player <playerName>`

### 8.3 Protection and behavior commands

- `/ss alertlevel [low|medium|high|critical]`
- `/ss autoleave [on|off]`
- `/ss mute [pattern]`
- `/ss unmute [pattern]`
- `/ss bypass <id>`

### 8.4 Model and maintenance commands

- `/ss model`
- `/ss model flag <messageId> <legit|scam>`
- `/ss model migrate`
- `/ss model update`
- `/ss model update force`
- `/ss model update notify [on|off]`
- `/ss model <download|accept|merge|ignore> <id>`
- `/ss model metrics [reset]`
- `/ss model reset`
- `/ss model autocapture [off|low|medium|high|critical]`

### 8.5 Utility commands

- `/ss settings`
- `/ss version`
- `/ss preview`

---

## 9. Recommended Daily Routine

1. Play normally.
2. When a warning appears, inspect `[info]`.
3. Label with `[manage]` if you can classify confidently.
4. Use `/ss review` between sessions to clear pending logged lines.
5. Use `Review Training CSV` occasionally for cleanup.
6. Upload when dataset gets large (or when reminder appears).

---

## 10. Troubleshooting

### "No review lines appear"

- Wait for actual player chat lines (system lines are filtered).
- Use `/ss review` after chat activity.
- For `/ss review player`, target must be online and have logged entries.

### "Upload does not start"

- Confirm upload ToS when prompted.
- Check that `scam-screener-training-data.csv` exists.
- Read chat error code/details if upload failed.

### "Too many warnings"

- Raise alert threshold (`/ss alertlevel high`).
- Disable noisy rules in `Rule Settings`.
- Review and clean training data quality.

### "Not enough warnings"

- Lower alert threshold (`/ss alertlevel low` or `medium`).
- Ensure `Local Model Signal` is enabled in settings.
- Confirm message warnings are enabled in `Message Settings`.

---

## 11. Privacy and Network Behavior

ScamScreener is client-side first.

Local by default:

- detection
- review
- blacklist management
- training data storage

Network is only used for optional features:

- Mojang name/UUID lookups
- Model update check/download
- training data upload when you explicitly trigger it

---

## 12. Final Tips

- Label only what you are confident about.
- Mixed, high-quality scam + legit labels improve model quality more than bulk noisy labels.
- Use `ignored` when context is unclear.
- Review often, upload periodically.

