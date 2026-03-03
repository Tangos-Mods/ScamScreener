package eu.tango.scamscreener;

import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PlayerListChangeType;
import eu.tango.scamscreener.api.event.WhitelistEvent;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.config.store.BlacklistConfigStore;
import eu.tango.scamscreener.config.store.ReviewConfigStore;
import eu.tango.scamscreener.config.store.RulesConfigStore;
import eu.tango.scamscreener.config.store.RuntimeConfigStore;
import eu.tango.scamscreener.config.store.WhitelistConfigStore;
import eu.tango.scamscreener.pipeline.core.PipelineEngine;
import eu.tango.scamscreener.pipeline.core.ScamScreenerPipelineFactory;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;
import eu.tango.scamscreener.pipeline.state.FunnelStore;
import eu.tango.scamscreener.pipeline.state.TrendStore;
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
    private final RulesConfigStore rulesConfigStore;
    private final ReviewConfigStore reviewConfigStore;
    @Getter
    @Accessors(fluent = true)
    private final Whitelist whitelist;
    @Getter
    @Accessors(fluent = true)
    private final Blacklist blacklist;
    @Getter
    @Accessors(fluent = true)
    private final ReviewStore reviewStore;
    @Getter
    @Accessors(fluent = true)
    private final BehaviorStore behaviorStore;
    @Getter
    @Accessors(fluent = true)
    private final TrendStore trendStore;
    @Getter
    @Accessors(fluent = true)
    private final FunnelStore funnelStore;
    @Getter
    @Accessors(fluent = true)
    private final MutePatternManager mutePatternManager;
    private volatile RuntimeConfig runtimeConfig;
    private volatile RulesConfig rulesConfig;
    @Getter
    @Accessors(fluent = true)
    private volatile PipelineEngine pipelineEngine;

    private ScamScreenerRuntime() {
        whitelistConfigStore = new WhitelistConfigStore();
        blacklistConfigStore = new BlacklistConfigStore();
        runtimeConfigStore = new RuntimeConfigStore();
        rulesConfigStore = new RulesConfigStore();
        reviewConfigStore = new ReviewConfigStore();
        runtimeConfig = runtimeConfigStore.loadOrCreate();
        rulesConfig = rulesConfigStore.loadOrCreate();
        whitelist = new Whitelist(this::saveWhitelist);
        blacklist = new Blacklist(this::saveBlacklist);
        reviewStore = new ReviewStore(this::saveReviewStore);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        behaviorStore = new BehaviorStore();
        trendStore = new TrendStore();
        funnelStore = new FunnelStore();
        mutePatternManager = new MutePatternManager();
        mutePatternManager.reloadFromConfig(runtimeConfig);
        applyRuleStoreSettings();
        whitelistConfigStore.loadInto(whitelist);
        blacklistConfigStore.loadInto(blacklist);
        reviewConfigStore.loadInto(reviewStore);
        pipelineEngine = ScamScreenerPipelineFactory.createDefaultEngine(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
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
     * Returns the shared rule config.
     *
     * @return the loaded deterministic and similarity rule config
     */
    public RulesConfig rules() {
        return rulesConfig;
    }

    /**
     * Reloads runtime config and persisted list contents from disk.
     */
    public synchronized void reload() {
        runtimeConfig = runtimeConfigStore.reload();
        rulesConfig = rulesConfigStore.reload();
        applyRuleStoreSettings();
        resetDetectionState();
        whitelistConfigStore.reload();
        whitelistConfigStore.loadInto(whitelist);
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(PlayerListChangeType.RELOADED, null);

        blacklistConfigStore.reload();
        blacklistConfigStore.loadInto(blacklist);
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(PlayerListChangeType.RELOADED, null);

        reviewConfigStore.reload();
        reviewConfigStore.loadInto(reviewStore);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        mutePatternManager.reloadFromConfig(runtimeConfig);

        rebuildPipelineEngine();
    }

    /**
     * Saves the current in-memory runtime config and reapplies derived runtime state.
     */
    public synchronized void saveConfig() {
        runtimeConfigStore.save(runtimeConfig);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        rebuildPipelineEngine();
    }

    /**
     * Saves the current in-memory rules config and reapplies the pipeline.
     */
    public synchronized void saveRules() {
        rulesConfigStore.save(rulesConfig);
        applyRuleStoreSettings();
        resetDetectionState();
        rebuildPipelineEngine();
    }

    /**
     * Clears the shared state used by the stateful detection stages.
     */
    public synchronized void resetDetectionState() {
        behaviorStore.reset();
        trendStore.reset();
        funnelStore.reset();
    }

    private void saveWhitelist() {
        whitelistConfigStore.saveFrom(whitelist);
    }

    private void saveBlacklist() {
        blacklistConfigStore.saveFrom(blacklist);
    }

    private void saveReviewStore() {
        reviewConfigStore.saveFrom(reviewStore);
    }

    private void applyRuleStoreSettings() {
        RuleCatalog ruleCatalog = new RuleCatalog(rulesConfig);
        behaviorStore.configure(ruleCatalog.behavior().windowMs(), ruleCatalog.behavior().maxHistory());
        trendStore.configure(ruleCatalog.trend().windowMs(), ruleCatalog.trend().maxHistory());
        funnelStore.configure(ruleCatalog.funnel().windowMs(), ruleCatalog.funnel().maxHistory());
    }

    private void rebuildPipelineEngine() {
        pipelineEngine = ScamScreenerPipelineFactory.createDefaultEngine(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            runtimeConfig.pipeline().reviewThreshold()
        );
    }
}
