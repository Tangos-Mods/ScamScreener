# ScamScreener

[![Build](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/build.yml/badge.svg?branch=v2)](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/build.yml)
[![Tests](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/tests-per-class.yml/badge.svg?branch=v2)](https://github.com/Tangos-Mods/ScamScreener/actions/workflows/tests-per-class.yml)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/scamscreener?logo=modrinth&label=Modrinth%20downloads)](https://modrinth.com/mod/scamscreener)

ScamScreener is a client-side Fabric mod that analyzes incoming chat, highlights scam-like behavior patterns, and gives you a structured review workflow for safer decisions.

## At A Glance

- Realtime chat risk analysis with a multi-stage detection pipeline
- In-game review queue with case-level message tagging
- Whitelist and blacklist tools with command + GUI support
- Optional auto `/p leave` on blacklist alerts
- Built-in live profiler HUD for tick cost and phase timing
- Optional web profiler via Tango Web API with `.sspp` export for debugging
- Canonical training export (`training-cases-v2.jsonl`) for later tuning
- Public API entrypoint for integrations (`scamscreener-api`)

## Supported Versions And Requirements

- Minecraft: `1.21.10`, `1.21.11`
- Mod loader: Fabric Loader `>= 0.17`
- Dependency: Fabric API
- Optional dependency: Tango Web API for `/ss profiler open`
- Environment: client-only

## Installation

1. Install Fabric Loader for your Minecraft version.
2. Put `ScamScreener` and `Fabric API` into your `mods` folder.
3. Optional: add `Tango Web API` if you want the browser profiler.
4. Launch the game.
5. Open settings with `/scamscreener settings` (or `/ss settings`).

## Quick Start

1. Set your risk threshold (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Note that `LOW` might warn you way often when on large public islands!
2. Play as usual and let ScamScreener monitor inbound chat.
3. Open review queue (`/scamscreener review`) for flagged cases.
4. Mark cases as `RISK`, `SAFE`, or `DISMISSED`.
5. Use `/ss profiler on` when you want live timing data for lag hunting.
6. Open `/ss profiler open` if Tango Web API is installed.
7. Export reviewed data with `/scamscreener review export` if needed.

## How Detection Works

ScamScreener evaluates each inbound message through a staged pipeline:

- Player list stage (whitelist/blacklist hard matches)
- Deterministic rule stage (links, urgency, platform redirects, payment/account signals)
- Similarity stage (fuzzy phrase matching)
- Behavior stage (sender-local repetition/burst patterns)
- Trend stage (cross-sender wave detection)
- Funnel stage (multi-step trust -> redirect -> payment/account patterns)
- Context stage (signal blend across recent same-sender messages)
- Mute stage / bypass logic

Pipeline outcomes can include `IGNORE`, `REVIEW`, `BLOCK`, `WHITELISTED`, `BLACKLISTED`, or `MUTED`.

## In-Game Commands

Root commands:

- `/scamscreener ...`
- `/ss ...` (alias)

Main command groups:

- `open`, `enable`, `disable`
- `settings`, `rules`, `runtime`, `messages`, `metrics`
- `whitelist add/remove/clear`
- `blacklist add/remove/clear`
- `review`, `review player`, `review manage`, `review info`, `review export`
- `alertlevel`
- `autoleave on/off`
- `mute`, `mute <regex>`, `unmute`, `unmute <regex>`
- `profiler`, `profiler on/off/open`
- `debug`
- `version`, `help`

## Profiler And Performance Workflow

Profiler commands:

- `/ss profiler` shows whether the profiler is currently enabled
- `/ss profiler on` enables the live in-game profiler HUD
- `/ss profiler off` disables the profiler and clears retained samples
- `/ss profiler open` opens the browser profiler when Tango Web API is installed

Profiler surfaces:

- HUD overlay for live mod tick cost and hot phases
- Browser dashboard for rolling metrics, lifetime averages, phase breakdown, and recent event log
- `.sspp` export from the web profiler so developers can inspect one captured profile offline

Notes:

- The profiler is fully off when disabled and does not keep recording in the background.
- If Tango Web API is missing, `/ss profiler open` shows a clickable download message instead of failing silently.

## GUI Overview

Main settings screen includes:

- Alert threshold and auto-capture settings
- Auto-leave and mute filter toggles
- Whitelist / blacklist management
- Review queue and review settings
- Rules, runtime, metrics, and message settings

## Case Review And Training Export

Review flow:

1. Open queue and pick a case.
2. Tag each message as `Exclude`, `Context`, or `Signal`.
3. Save verdict as `Risk`, `Safe`, or `Dismiss`.

Export:

- Command: `/scamscreener review export`
- Output file: `config/scamscreener/training-cases-v2.jsonl`
- Runs in the background and reports completion back in chat
- Format docs: [training_case_v2](docs/training_case_v2.md)

Important status:

- Training Hub is currently not live.
- `Contribute Training Data` buttons are intentionally disabled in GUI for now.
- Local export is still available and stable.

## Configuration Files

ScamScreener stores its data in `config/scamscreener/`:

- `runtime.json` (runtime and output behavior)
- `rules.json` (pipeline rule configuration)
- `review.json` (review queue state)
- `whitelist.json` (trusted players)
- `blacklist.json` (blocked players)
- `training-cases-v2.jsonl` (manual export artifact)

## Privacy And Data Handling

- ScamScreener runs client-side.
- No automatic training upload is performed by the mod.
- Review/training payloads are sanitized and do not persist sender UUID identity.
- UUID-based persistence is intentionally limited to whitelist/blacklist management.

## v1 To v2 Migration

On first start of v2, ScamScreener performs a one-time migration:

- Migrates legacy whitelist/blacklist files into v2 config files
- Writes migration marker `.v1-to-v2-migration.done`
- Cleans up legacy v1 config folder artifacts after migration

Existing v2 target files are never overwritten.

## API For Other Mods

Get the API through the Fabric entrypoint:

```java
import eu.tango.scamscreener.api.ScamScreenerApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

List<ScamScreenerApi> apis = FabricLoader.getInstance()
    .getEntrypoints(ScamScreenerApi.ENTRYPOINT_KEY, ScamScreenerApi.class);

if (!apis.isEmpty()) {
    ScamScreenerApi api = apis.get(0);
}
```

Then use shared list access and events:

```java
import eu.tango.scamscreener.api.ScamScreenerAlertLevel;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.api.event.WhitelistEvent;

WhitelistAccess whitelist = api.whitelist();
BlacklistAccess blacklist = api.blacklist();

PipelineDecisionEvent.EVENT.register((chatEvent, decision) -> {
    // react to pipeline decisions
});

WhitelistEvent.EVENT.register((changeType, entry) -> {
    // entry is null when the list is cleared
});

BlacklistEvent.EVENT.register((changeType, entry) -> {
    // entry is null when the list is cleared
});
```

Stable settings and config schema versions are also exposed:

```java
boolean pingOnRiskWarning = api.settings().pingOnRiskWarning();
int runtimeConfigVersion = api.schemas().runtimeConfigVersion();

api.settings().setPingOnBlacklistWarning(false);
api.settings().setAlertMinimumRiskLevel(ScamScreenerAlertLevel.HIGH);
```

Settings updates are saved back to `runtime.json` by ScamScreener.

## Build And Release (Maintainers)

1. Run checks:
   - `.\gradlew.bat test`
   - `.\gradlew.bat build`
2. Build artifacts:
   - `.\gradlew.bat buildAndCollect`
3. Prepare upload changelog:
   - update `MODRINTH.md`
   - update `CHANGELOG.md`
4. Publish:
   - set `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN`
   - run `.\gradlew.bat publishMods`

Publish uploads use `MODRINTH.md` as changelog text.

## Additional Documentation

- [Developer API guide](API.md)
- [Case Review Guide (players)](docs/case_review_player_guide.md)
- [Training Case v2 format](docs/training_case_v2.md)
- [Full engineering changelog](CHANGELOG.md)

## License

The source code is licensed under `GPL-3.0-only`.
Branding, name, icons, and other visual assets remain excluded from the GPL and are covered separately as described in [LICENSE](LICENSE).
