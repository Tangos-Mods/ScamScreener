package eu.tango.scamscreener;

import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PlayerListChangeType;
import eu.tango.scamscreener.api.event.WhitelistEvent;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.config.store.BlacklistConfigStore;
import eu.tango.scamscreener.config.store.RuntimeConfigStore;
import eu.tango.scamscreener.config.store.WhitelistConfigStore;
import eu.tango.scamscreener.pipeline.core.PipelineEngine;
import eu.tango.scamscreener.pipeline.core.ScamScreenerPipelineFactory;
import eu.tango.scamscreener.review.ReviewStore;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Central runtime container for shared ScamScreener services.
 *
 * <p>This keeps the built-in lists and the default pipeline engine wired from
 * one place, so API consumers and internal listeners use the same instances.
 */
public final class ScamScreenerRuntime {
    private static final ScamScreenerRuntime INSTANCE = new ScamScreenerRuntime();

    private final WhitelistConfigStore whitelistConfigStore;
    private final BlacklistConfigStore blacklistConfigStore;
    private final RuntimeConfigStore runtimeConfigStore;
    @Getter
    @Accessors(fluent = true)
    private final Whitelist whitelist;
    @Getter
    @Accessors(fluent = true)
    private final Blacklist blacklist;
    @Getter
    @Accessors(fluent = true)
    private final ReviewStore reviewStore;
    private volatile RuntimeConfig runtimeConfig;
    @Getter
    @Accessors(fluent = true)
    private volatile PipelineEngine pipelineEngine;

    private ScamScreenerRuntime() {
        whitelistConfigStore = new WhitelistConfigStore();
        blacklistConfigStore = new BlacklistConfigStore();
        runtimeConfigStore = new RuntimeConfigStore();
        runtimeConfig = runtimeConfigStore.loadOrCreate();
        whitelist = new Whitelist(this::saveWhitelist);
        blacklist = new Blacklist(this::saveBlacklist);
        reviewStore = new ReviewStore();
        whitelistConfigStore.loadInto(whitelist);
        blacklistConfigStore.loadInto(blacklist);
        pipelineEngine = ScamScreenerPipelineFactory.createDefaultEngine(
            whitelist,
            blacklist,
            runtimeConfig.pipeline().reviewThreshold()
        );
    }

    /**
     * Returns the shared ScamScreener runtime.
     *
     * @return the singleton runtime container
     */
    public static ScamScreenerRuntime getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the shared runtime config.
     *
     * @return the loaded runtime config
     */
    public RuntimeConfig config() {
        return runtimeConfig;
    }

    /**
     * Reloads runtime config and persisted list contents from disk.
     */
    public synchronized void reload() {
        runtimeConfig = runtimeConfigStore.reload();
        whitelistConfigStore.reload();
        whitelistConfigStore.loadInto(whitelist);
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(PlayerListChangeType.RELOADED, null);

        blacklistConfigStore.reload();
        blacklistConfigStore.loadInto(blacklist);
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(PlayerListChangeType.RELOADED, null);

        rebuildPipelineEngine();
    }

    /**
     * Saves the current in-memory runtime config and reapplies derived runtime state.
     */
    public synchronized void saveConfig() {
        runtimeConfigStore.save(runtimeConfig);
        rebuildPipelineEngine();
    }

    private void saveWhitelist() {
        whitelistConfigStore.saveFrom(whitelist);
    }

    private void saveBlacklist() {
        blacklistConfigStore.saveFrom(blacklist);
    }

    private void rebuildPipelineEngine() {
        pipelineEngine = ScamScreenerPipelineFactory.createDefaultEngine(
            whitelist,
            blacklist,
            runtimeConfig.pipeline().reviewThreshold()
        );
    }
}
