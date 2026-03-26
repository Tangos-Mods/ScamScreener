# ScamScreener API

This document is for developers who want to integrate with ScamScreener from another Fabric mod.

The API is intentionally narrow:

- stable integration points instead of direct access to internal runtime classes
- access to settings, shared lists, and events
- no need to depend on the internal config-file structure
- safe optional integration when ScamScreener is not installed

## Overview

ScamScreener currently exposes two relevant entrypoints:

- `scamscreener-api`
  - the stable runtime API for other mods
- `scamscreener-pipeline`
  - a prepared contribution contract for custom stages

Important: the `scamscreener-pipeline` contract already exists as an API type, but it is not currently wired into the runtime. For production integrations, you should treat `scamscreener-api` as the supported surface.

## What The API Currently Exposes

Through `ScamScreenerApi`, you can access:

- `pipeline()`
  - the publicly exposed core stage order
- `settings()`
  - stable getters and setters for selected runtime settings
- `schemas()`
  - the current version numbers of ScamScreener config files
- `whitelist()`
  - read/write access to the shared whitelist
- `blacklist()`
  - read/write access to the shared blacklist
- `reload()`
  - reloads config files and persisted list state from disk

## 1. Getting The API Safely

If ScamScreener is optional for your mod, do not assume the API is always present. Resolve it through the Fabric entrypoint and handle the empty case.

```java
import eu.tango.scamscreener.api.ScamScreenerApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Optional;

public final class ScamScreenerCompat {
    private ScamScreenerCompat() {
    }

    public static Optional<ScamScreenerApi> findApi() {
        List<ScamScreenerApi> apis = FabricLoader.getInstance()
            .getEntrypoints(ScamScreenerApi.ENTRYPOINT_KEY, ScamScreenerApi.class);

        if (apis.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(apis.get(0));
    }
}
```

Example usage:

```java
ScamScreenerCompat.findApi().ifPresent(api -> {
    boolean supported = api.pipeline().supports(eu.tango.scamscreener.api.StageSlot.RULE);
    System.out.println("ScamScreener found, RULE slot present: " + supported);
});
```

### Recommended Pattern For Optional Integrations

- Resolve the API once during client startup or when initializing your compat layer.
- Store it locally as `Optional<ScamScreenerApi>` or a nullable reference.
- Do not call `FabricLoader.getEntrypoints(...)` repeatedly in hot paths.
- Keep ScamScreener-specific code in a dedicated compat class when possible, so missing mods do not accidentally cause hard references during class loading.

## 2. Reading And Writing Settings

Stable runtime settings are exposed through `api.settings()`. The currently public settings are:

- `pingOnRiskWarning()`
- `setPingOnRiskWarning(boolean enabled)`
- `pingOnBlacklistWarning()`
- `setPingOnBlacklistWarning(boolean enabled)`
- `alertMinimumRiskLevel()`
- `setAlertMinimumRiskLevel(ScamScreenerAlertLevel level)`

The important point is that your mod does not need to know the structure of `runtime.json`. That is exactly what this API is meant to hide.

### Example: A Generic Welcome Wizard

```java
import eu.tango.scamscreener.api.ScamScreenerAlertLevel;
import eu.tango.scamscreener.api.ScamScreenerApi;

public final class WelcomeWizardIntegration {
    private final ScamScreenerApi api;

    public WelcomeWizardIntegration(ScamScreenerApi api) {
        this.api = api;
    }

    public WizardState loadInitialState() {
        return new WizardState(
            api.settings().pingOnRiskWarning(),
            api.settings().pingOnBlacklistWarning(),
            api.settings().alertMinimumRiskLevel()
        );
    }

    public void applyUserSelection(boolean pingRisk, boolean pingBlacklist, ScamScreenerAlertLevel level) {
        api.settings().setPingOnRiskWarning(pingRisk);
        api.settings().setPingOnBlacklistWarning(pingBlacklist);
        api.settings().setAlertMinimumRiskLevel(level);
    }

    public record WizardState(
        boolean pingRisk,
        boolean pingBlacklist,
        ScamScreenerAlertLevel minimumRiskLevel
    ) {
    }
}
```

### Setter Behavior

- Changes are persisted by ScamScreener back to `runtime.json`.
- Saving is asynchronous.
- Setting the same value again does not trigger unnecessary writes.
- You do not need to call `reload()` after a normal setter call.

### What You Should Avoid

- writing `runtime.json` yourself when a public setter already exists
- calling the setters every tick or every frame
- bypassing the API with raw strings for alert levels

## 3. Reading Config Schema Versions

Through `api.schemas()`, you can access the current versions of the config files written by ScamScreener:

- `runtimeConfigVersion()`
- `rulesConfigVersion()`
- `whitelistConfigVersion()`
- `blacklistConfigVersion()`
- `reviewConfigVersion()`

This is useful for:

- diagnostics
- migration UIs
- support or debug screens
- external tools that only need to display version info

### Example: Showing Schema Versions In A Debug Screen

```java
ScamScreenerApi api = ScamScreenerCompat.findApi().orElse(null);
if (api != null) {
    int runtimeVersion = api.schemas().runtimeConfigVersion();
    int rulesVersion = api.schemas().rulesConfigVersion();
    int reviewVersion = api.schemas().reviewConfigVersion();

    System.out.println("Runtime schema: " + runtimeVersion);
    System.out.println("Rules schema: " + rulesVersion);
    System.out.println("Review schema: " + reviewVersion);
}
```

### Important

Schema versions are file versions, not feature flags. Do not use them as a substitute for API detection.

Correct:

- use API types and methods directly
- use schema versions for display, diagnostics, or file-related decisions

Incorrect:

- "if `runtimeConfigVersion() >= 3`, method X must exist"

## 4. Using The Whitelist

`api.whitelist()` returns a `WhitelistAccess`.

Core operations:

- `add(UUID playerUuid, String playerName)`
- `get(UUID playerUuid)`
- `findByName(String playerName)`
- `allEntries()`
- `contains(UUID playerUuid)`
- `containsName(String playerName)`
- `remove(UUID playerUuid)`
- `removeByName(String playerName)`
- `clear()`
- `isEmpty()`

### Example: Adding A Player To The Whitelist

```java
import eu.tango.scamscreener.api.WhitelistAccess;

import java.util.UUID;

WhitelistAccess whitelist = api.whitelist();

UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
boolean stored = whitelist.add(uuid, "TrustedTrader");

if (stored) {
    System.out.println("Player was written to the whitelist.");
}
```

### Example: Lookup With UUID To Name Fallback

```java
import eu.tango.scamscreener.lists.WhitelistEntry;

WhitelistEntry entry = api.whitelist().get(playerUuid)
    .or(() -> api.whitelist().findByName(playerName))
    .orElse(null);

if (entry != null) {
    System.out.println("Whitelist hit: " + entry.playerName());
}
```

### Notes

- `playerUuid` and `playerName` can be used individually or together.
- If both are available, that is better for stable identification.
- `allEntries()` returns immutable entry objects, not the internal list itself.

## 5. Using The Blacklist

`api.blacklist()` returns a `BlacklistAccess`.

Core operations:

- `add(UUID playerUuid, String playerName, int score, String reason)`
- `add(UUID playerUuid, String playerName, int score, String reason, BlacklistSource source)`
- `get(UUID playerUuid)`
- `findByName(String playerName)`
- `allEntries()`
- `contains(UUID playerUuid)`
- `containsName(String playerName)`
- `remove(UUID playerUuid)`
- `removeByName(String playerName)`
- `clear()`
- `isEmpty()`

### Example: Creating A Blacklist Entry With API Source

If your mod created the entry itself, prefer `BlacklistSource.API` instead of using the default source.

```java
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.lists.BlacklistSource;

boolean stored = api.blacklist().add(
    suspectUuid,
    suspectName,
    100,
    "Imported by companion mod",
    BlacklistSource.API
);
```

### Example: Checking Before Adding

```java
BlacklistAccess blacklist = api.blacklist();

if (!blacklist.containsName("ScamAccount123")) {
    blacklist.add(null, "ScamAccount123", 90, "Known scam alias");
}
```

### What `BlacklistEntry` Contains

A `BlacklistEntry` contains:

- `playerUuid()`
- `playerName()`
- `score()`
- `reason()`
- `source()`

## 6. Reacting To Events

ScamScreener exposes three main events:

- `PipelineDecisionEvent.EVENT`
- `WhitelistEvent.EVENT`
- `BlacklistEvent.EVENT`

These are standard Fabric events.

### Example: Reacting To Final Pipeline Decisions

```java
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

PipelineDecisionEvent.EVENT.register((chatEvent, decision) -> {
    if (decision.getOutcome() == PipelineDecision.Outcome.REVIEW
        || decision.getOutcome() == PipelineDecision.Outcome.BLOCK
        || decision.getOutcome() == PipelineDecision.Outcome.BLACKLISTED) {

        System.out.println("ScamScreener produced a high-risk result:");
        System.out.println("Sender: " + chatEvent.getSenderName());
        System.out.println("Message: " + chatEvent.getRawMessage());
        System.out.println("Outcome: " + decision.getOutcome());
        System.out.println("Score: " + decision.getTotalScore());
        System.out.println("Reasons: " + decision.getReasons());
    }
});
```

### Useful Data On `PipelineDecision`

Commonly useful fields and methods:

- `getOutcome()`
- `getTotalScore()`
- `getDecidedByStage()`
- `getStageResults()`
- `getReasons()`
- `isTerminal()`

Possible outcomes:

- `IGNORE`
- `MUTED`
- `WHITELISTED`
- `BLACKLISTED`
- `ALLOW`
- `REVIEW`
- `BLOCK`

### Example: Reacting To List Changes

```java
import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PlayerListChangeType;
import eu.tango.scamscreener.api.event.WhitelistEvent;

WhitelistEvent.EVENT.register((changeType, entry) -> {
    if (changeType == PlayerListChangeType.ADDED && entry != null) {
        System.out.println("Whitelist entry added: " + entry.playerName());
    }
});

BlacklistEvent.EVENT.register((changeType, entry) -> {
    if (changeType == PlayerListChangeType.REMOVED && entry != null) {
        System.out.println("Blacklist entry removed: " + entry.playerName());
    }
});
```

### Special Case For List Events

For `CLEARED` and `RELOADED`, `entry == null`. That is intentional.

For example:

```java
BlacklistEvent.EVENT.register((changeType, entry) -> {
    if (changeType == PlayerListChangeType.CLEARED) {
        System.out.println("Blacklist was fully cleared.");
        return;
    }

    if (changeType == PlayerListChangeType.RELOADED) {
        System.out.println("Blacklist was reloaded from disk.");
        return;
    }

    if (entry != null) {
        System.out.println("Affected player: " + entry.playerName());
    }
});
```

## 7. Inspecting The Pipeline Structure

`api.pipeline()` returns a `ScamScreenerPipelineApi`.

At the moment, you can:

- read `coreStageOrder()`
- call `supports(StageSlot slot)`

The currently exposed core slots are:

- `MUTE`
- `PLAYER_LIST`
- `RULE`
- `LEVENSHTEIN`
- `BEHAVIOR`
- `TREND`
- `FUNNEL`
- `MODEL`
  The current built-in stage on this slot is the context-aware final stage.

### Example: Listing The Core Order

```java
api.pipeline().coreStageOrder().forEach(slot -> {
    System.out.println("Stage slot: " + slot.name());
});
```

### Typical Use Cases

This is useful if your mod:

- shows a debug screen for installed integrations
- wants to visualize capabilities
- wants to describe future contributions relative to stable slots

## 8. When `reload()` Makes Sense

`api.reload()` reloads runtime config and persisted list contents from disk.

This is useful when:

- an external process changed config files outside the API
- you intentionally modified raw files and want ScamScreener to re-read them

This is not useful when:

- you only used `api.settings().set...(...)`
- you only used `whitelist().add(...)` or `blacklist().add(...)`

### Example: Reloading After An External File Change

```java
ScamScreenerCompat.findApi().ifPresent(api -> {
    api.reload();
    System.out.println("ScamScreener was reloaded from disk.");
});
```

## 9. Complete Integration Example

The following example shows a small bridge for a generic companion mod. It:

- resolves ScamScreener optionally
- reads settings for a setup wizard
- writes updated wizard values back
- reacts to high-risk pipeline outcomes

```java
import eu.tango.scamscreener.api.ScamScreenerAlertLevel;
import eu.tango.scamscreener.api.ScamScreenerApi;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Optional;

public final class ExampleScamScreenerBridge {
    private final ScamScreenerApi api;

    // Keep the API handle inside one bridge object.
    private ExampleScamScreenerBridge(ScamScreenerApi api) {
        this.api = api;
    }

    // Resolve ScamScreener only when it is actually installed.
    public static Optional<ExampleScamScreenerBridge> create() {
        List<ScamScreenerApi> apis = FabricLoader.getInstance()
            .getEntrypoints(ScamScreenerApi.ENTRYPOINT_KEY, ScamScreenerApi.class);

        if (apis.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ExampleScamScreenerBridge(apis.get(0)));
    }

    // Read current defaults for your setup UI.
    public WizardSnapshot readWizardDefaults() {
        return new WizardSnapshot(
            api.settings().pingOnRiskWarning(),
            api.settings().pingOnBlacklistWarning(),
            api.settings().alertMinimumRiskLevel(),
            api.schemas().runtimeConfigVersion()
        );
    }

    // Push the user's choices back into ScamScreener.
    public void applyWizardChoices(boolean pingRisk, boolean pingBlacklist, ScamScreenerAlertLevel level) {
        api.settings().setPingOnRiskWarning(pingRisk);
        api.settings().setPingOnBlacklistWarning(pingBlacklist);
        api.settings().setAlertMinimumRiskLevel(level);
    }

    // Listen for hard hits and react inside your own mod.
    public void registerListeners() {
        PipelineDecisionEvent.EVENT.register((chatEvent, decision) -> {
            if (decision.getOutcome() == PipelineDecision.Outcome.BLOCK
                || decision.getOutcome() == PipelineDecision.Outcome.BLACKLISTED) {
                System.out.println("Companion mod notice: ScamScreener detected a hard hit.");
            }
        });
    }

    public record WizardSnapshot(
        boolean pingOnRiskWarning,
        boolean pingOnBlacklistWarning,
        ScamScreenerAlertLevel alertMinimumRiskLevel,
        int runtimeSchemaVersion
    ) {
    }
}
```

## 10. Current Limits Of The API

Do not currently plan around these capabilities:

- direct access to `RuntimeConfig`
- direct manipulation of the internal `PipelineEngine`
- executing custom stages through `scamscreener-api`
- mutating the built-in core stage order directly; external stages are attached relative to the exposed slots

## 11. Best Practices

- Treat ScamScreener as an optional dependency if your mod should also work without it.
- Use the public getters and setters instead of parsing config files yourself.
- Use `BlacklistSource.API` when a blacklist entry truly comes from your mod.
- Always handle the possibility that `entry` is `null` in list events.
- Use `reload()` only for actual disk synchronization, not as a general refresh button.
- Use schema versions for config diagnostics, not for method detection.

## 12. Quick Reference

### Getting The API

```java
ScamScreenerApi api = FabricLoader.getInstance()
    .getEntrypoints(ScamScreenerApi.ENTRYPOINT_KEY, ScamScreenerApi.class)
    .stream()
    .findFirst()
    .orElse(null);
```

### Reading Settings

```java
boolean riskPing = api.settings().pingOnRiskWarning();
boolean blacklistPing = api.settings().pingOnBlacklistWarning();
ScamScreenerAlertLevel level = api.settings().alertMinimumRiskLevel();
```

### Writing Settings

```java
api.settings().setPingOnRiskWarning(true);
api.settings().setPingOnBlacklistWarning(false);
api.settings().setAlertMinimumRiskLevel(ScamScreenerAlertLevel.HIGH);
```

### Reading Schema Versions

```java
int runtimeVersion = api.schemas().runtimeConfigVersion();
int rulesVersion = api.schemas().rulesConfigVersion();
```

### Using The Lists

```java
api.whitelist().add(playerUuid, playerName);
api.blacklist().add(playerUuid, playerName, 100, "Imported", eu.tango.scamscreener.lists.BlacklistSource.API);
```

### Reloading

```java
api.reload();
```
