# ScamScreener
Fabric mod scaffold based on Stonecutter.

## Development
1. Review the supported Minecraft versions in `settings.gradle.kts`.
   For new entries, add `versions/.../gradle.properties` with the same keys as other versions.
2. Review the `LICENSE` file.
   See the [license decision diagram](https://docs.codeberg.org/getting-started/licensing/#license-decision-diagram) for common options.
3. Keep `src/main/resources/fabric.mod.json` up to date.

## Usage
- Use `"Set active project to ..."` Gradle tasks to update the Minecraft version
  available in `src/` classes.
- Use `buildAndCollect` Gradle task to store mod releases in `build/libs/`.
- Publishing to Modrinth/CurseForge is configured via `mod-publish-plugin`.
  Provide `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` (for example in `.env`) before running publish tasks.
- Enable `maven-publish` in `build.gradle.kts` and the corresponding code block
  to publish releases to a personal maven repository.

## Case Review and Training
- Players do case annotation in-game and export reviewed cases.
- Players upload the exported JSONL file in the Training Hub (`server/`).
- Model/stage training is triggered by the developer from the Training Hub admin area.
- Export file is written to `config/scamscreener/`:
  - `training-cases-v2.jsonl`

Detailed docs:
- [training_case_v2](docs/training_case_v2.md)
- [case_review_player_guide](docs/case_review_player_guide.md)
- [training_hub_server](server/README.md)

Pipeline auto-tuning script:
- `.\scripts\auto_tune_pipeline.ps1 -TrainingDataDir trainingdata -RulesFile run/config/scamscreener/rules.json -Apply`

## API

Other mods can access ScamScreener through the Fabric entrypoint key
`scamscreener-api`.

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

Use the access interfaces to read or mutate the shared player lists.

```java
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.lists.BlacklistSource;

WhitelistAccess whitelist = api.whitelist();
BlacklistAccess blacklist = api.blacklist();

whitelist.add(playerUuid, "TrustedPlayer");
blacklist.add(playerUuid, "BlockedPlayer", 50, "manual review", BlacklistSource.API);
```

Register events if your mod should react to pipeline decisions or list changes.

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

## Useful links
- [Stonecutter beginner's guide](https://stonecutter.kikugie.dev/wiki/start/): *spoiler: you* ***need*** *to understand how it works!*
- [Fabric Discord server](https://discord.gg/v6v4pMv): for mod development help.
- [Stonecutter Discord server](https://discord.kikugie.dev/): for Stonecutter and Gradle help.
- [How To Ask Questions - the guide](http://www.catb.org/esr/faqs/smart-questions.html): also in [video form](https://www.youtube.com/results?search_query=How+To+Ask+Questions+The+Smart+Way).
