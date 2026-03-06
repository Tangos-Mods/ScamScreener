# ScamScreener

ScamScreener is a Fabric mod that flags risky chat behavior, helps players review suspicious cases, and exports clean training data for pipeline iteration.

## Release Status

- Current line: `v2` (`mod.version=2.0.0`)
- Training Hub status: not live yet
- The `Contribute Training Data` buttons are intentionally disabled until the hub relaunches
- Export still works via command: `/scamscreener review export`

## Core Features

- Real-time scam-signal pipeline with stage-based scoring
- Manual review queue with context-aware case tooling
- Canonical training export: `training-cases-v2.jsonl`
- Cross-version build setup through Stonecutter
- Public API entrypoint for integrations: `scamscreener-api`

## Player Workflow

1. Review suspicious entries in-game (`/scamscreener review` or GUI).
2. Mark outcomes and annotate context/signals.
3. Export reviewed cases with `/scamscreener review export`.
4. Find the export under `config/scamscreener/training-cases-v2.jsonl`.

## Build and Release

1. Select the active Minecraft target with the Stonecutter task `Set active project to ...`.
2. Run local checks:
   - `.\gradlew.bat test`
   - `.\gradlew.bat build`
3. Build distributable jars:
   - `.\gradlew.bat buildAndCollect`
4. Publish to Modrinth/CurseForge:
   - set `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` (for example in `.env`)
   - update `MODRINTH.md` (this file is used as upload changelog text)
   - run `.\gradlew.bat publishMods`

Artifacts are collected in `build/libs/<mod.version>/`.

## Developer Docs

- [Training Case v2](docs/training_case_v2.md)
- [Case Review Player Guide](docs/case_review_player_guide.md)
- Auto-tuning script:
  - `.\scripts\auto_tune_pipeline.ps1 -TrainingDataDir trainingdata -RulesFile run/config/scamscreener/rules.json -Apply`

## API Integration

Discover the API via the Fabric entrypoint:

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

Read or mutate shared player lists:

```java
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.lists.BlacklistSource;

WhitelistAccess whitelist = api.whitelist();
BlacklistAccess blacklist = api.blacklist();

whitelist.add(playerUuid, "TrustedPlayer");
blacklist.add(playerUuid, "BlockedPlayer", 50, "manual review", BlacklistSource.API);
```

Subscribe to decision and list change events:

```java
import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.api.event.WhitelistEvent;

PipelineDecisionEvent.EVENT.register((chatEvent, decision) -> {
    if (decision.isTerminal()) {
        // React to the final ScamScreener decision.
    }
});

WhitelistEvent.EVENT.register((changeType, entry) -> {
    // entry is null when the whitelist was cleared.
});

BlacklistEvent.EVENT.register((changeType, entry) -> {
    // entry is null when the blacklist was cleared.
});
```

## Useful Links

- [Stonecutter beginner's guide](https://stonecutter.kikugie.dev/wiki/start/)
- [Fabric Discord server](https://discord.gg/v6v4pMv)
- [Stonecutter Discord server](https://discord.kikugie.dev/)
- [How To Ask Questions - the guide](http://www.catb.org/esr/faqs/smart-questions.html)
