package eu.tango.scamscreener;

import eu.tango.scamscreener.api.PipelineContributor;
import eu.tango.scamscreener.api.StageContribution;
import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PlayerListChangeType;
import eu.tango.scamscreener.api.event.WhitelistEvent;
import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.PlayerUuidLookup;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.config.store.BlacklistConfigStore;
import eu.tango.scamscreener.config.migration.LegacyV1ConfigMigration;
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
import eu.tango.scamscreener.training.ScamScreenerClientSession;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import eu.tango.scamscreener.training.TrainingHubUploadWorker;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final RecentChatCache recentChatCache;
    @Getter
    @Accessors(fluent = true)
    private final MutePatternManager mutePatternManager;
    @Getter
    @Accessors(fluent = true)
    private final TrainingCaseExportService trainingCaseExportService;
    @Getter
    @Accessors(fluent = true)
    private final TrainingHubUploadWorker trainingHubUploadWorker;
    private final List<StageContribution> stageContributions;
    private final Set<String> blacklistUuidLookupsInFlight;
    private volatile RuntimeConfig runtimeConfig;
    private volatile RulesConfig rulesConfig;
    private volatile ScamScreenerClientSession trainingHubSession;
    @Getter
    @Accessors(fluent = true)
    private volatile PipelineEngine pipelineEngine;

    private ScamScreenerRuntime() {
        LegacyV1ConfigMigration.runDefaultOnce();
        whitelistConfigStore = new WhitelistConfigStore();
        blacklistConfigStore = new BlacklistConfigStore();
        runtimeConfigStore = new RuntimeConfigStore();
        rulesConfigStore = new RulesConfigStore();
        reviewConfigStore = new ReviewConfigStore();
        runtimeConfig = runtimeConfigStore.loadOrCreate();
        rulesConfig = rulesConfigStore.loadOrCreate();
        String trainingClientId = ensureTrainingClientId();
        blacklistUuidLookupsInFlight = ConcurrentHashMap.newKeySet();
        whitelist = new Whitelist(this::saveWhitelist);
        blacklist = new Blacklist(this::saveBlacklist, this::queueBlacklistUuidLookup);
        reviewStore = new ReviewStore(this::saveReviewStore);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        behaviorStore = new BehaviorStore();
        trendStore = new TrendStore();
        funnelStore = new FunnelStore();
        recentChatCache = new RecentChatCache();
        mutePatternManager = new MutePatternManager();
        trainingCaseExportService = new TrainingCaseExportService(trainingClientId);
        trainingHubUploadWorker = new TrainingHubUploadWorker(
            trainingCaseExportService,
            reviewStore::entries,
            this::trainingHubSession,
            this::clearTrainingHubSession,
            this::config
        );
        stageContributions = loadStageContributions();
        mutePatternManager.reloadFromConfig(runtimeConfig);
        applyRuleStoreSettings();
        whitelistConfigStore.loadInto(whitelist);
        blacklistConfigStore.loadInto(blacklist);
        queueBlacklistUuidLookupsForMissingEntries();
        reviewConfigStore.loadInto(reviewStore);
        pipelineEngine = ScamScreenerPipelineFactory.createDefaultEngine(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache,
            runtimeConfig.pipeline().reviewThreshold(),
            stageContributions
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
     * Indicates whether ScamScreener processing is currently enabled.
     *
     * @return {@code true} when ScamScreener should process inbound chat
     */
    public boolean isEnabled() {
        return runtimeConfig != null && runtimeConfig.isEnabled();
    }

    /**
     * Updates the global ScamScreener enabled state and reapplies derived runtime state.
     *
     * @param enabled the new enabled flag
     */
    public synchronized void setEnabled(boolean enabled) {
        runtimeConfig.setEnabled(enabled);
        runtimeConfigStore.saveAsync(runtimeConfig);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        resetDetectionState();
        rebuildPipelineEngine();
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
     * Returns the active in-memory Training Hub session, clearing it when expired.
     *
     * @return the active authenticated upload session, when still valid
     */
    public synchronized ScamScreenerClientSession trainingHubSession() {
        ScamScreenerClientSession currentSession = trainingHubSession;
        if (currentSession != null && currentSession.isExpired()) {
            trainingHubSession = null;
            return null;
        }

        return currentSession;
    }

    /**
     * Stores the current in-memory Training Hub session.
     *
     * @param trainingHubSession the authenticated upload session to reuse
     */
    public synchronized void setTrainingHubSession(ScamScreenerClientSession trainingHubSession) {
        this.trainingHubSession = trainingHubSession;
    }

    /**
     * Clears the current in-memory Training Hub session.
     */
    public synchronized void clearTrainingHubSession() {
        trainingHubSession = null;
    }

    /**
     * Reloads runtime config and persisted list contents from disk.
     */
    public synchronized void reload() {
        runtimeConfig = runtimeConfigStore.reload();
        trainingCaseExportService.setTrainingClientId(ensureTrainingClientId());
        rulesConfig = rulesConfigStore.reload();
        applyRuleStoreSettings();
        resetDetectionState();
        whitelistConfigStore.reload();
        whitelistConfigStore.loadInto(whitelist);
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(PlayerListChangeType.RELOADED, null);

        blacklistConfigStore.reload();
        blacklistConfigStore.loadInto(blacklist);
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(PlayerListChangeType.RELOADED, null);
        queueBlacklistUuidLookupsForMissingEntries();

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
        runtimeConfigStore.saveAsync(runtimeConfig);
        reviewStore.setMaxEntries(runtimeConfig.review().maxEntries());
        rebuildPipelineEngine();
    }

    /**
     * Saves the current in-memory rules config and reapplies the pipeline.
     */
    public synchronized void saveRules() {
        rulesConfigStore.saveAsync(rulesConfig);
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
        recentChatCache.clear();
    }

    private void saveWhitelist() {
        whitelistConfigStore.saveFromAsync(whitelist);
    }

    private void saveBlacklist() {
        blacklistConfigStore.saveFromAsync(blacklist);
    }

    private void saveReviewStore() {
        reviewConfigStore.saveFromAsync(reviewStore);
    }

    private void queueBlacklistUuidLookupsForMissingEntries() {
        for (BlacklistEntry entry : blacklist.allEntries()) {
            if (entry != null && entry.playerUuid() == null && !entry.playerName().isBlank()) {
                queueBlacklistUuidLookup(entry.playerName());
            }
        }
    }

    private void queueBlacklistUuidLookup(String playerName) {
        String normalizedName = normalizeLookupName(playerName);
        if (normalizedName.isEmpty() || !blacklistUuidLookupsInFlight.add(normalizedName)) {
            return;
        }

        PlayerUuidLookup.resolveByNameAsync(playerName).whenComplete((playerUuid, throwable) -> {
            blacklistUuidLookupsInFlight.remove(normalizedName);
            if (throwable != null || playerUuid == null) {
                return;
            }

            Minecraft client = Minecraft.getInstance();
            Runnable updateAction = () -> applyResolvedBlacklistUuid(playerName, playerUuid);
            if (client != null) {
                client.execute(updateAction);
                return;
            }

            updateAction.run();
        });
    }

    private void applyResolvedBlacklistUuid(String playerName, UUID playerUuid) {
        blacklist.findByName(playerName)
            .filter(entry -> entry.playerUuid() == null)
            .ifPresent(entry -> blacklist.add(playerUuid, entry.playerName(), entry.score(), entry.reason(), entry.source()));
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
            recentChatCache,
            runtimeConfig.pipeline().reviewThreshold(),
            stageContributions
        );
    }

    private String ensureTrainingClientId() {
        String trainingClientId = runtimeConfig.trainingClientId();
        if (!trainingClientId.isBlank()) {
            return trainingClientId;
        }

        trainingClientId = UUID.randomUUID().toString();
        runtimeConfig.setTrainingClientId(trainingClientId);
        runtimeConfigStore.saveAsync(runtimeConfig);
        return trainingClientId;
    }

    private static List<StageContribution> loadStageContributions() {
        List<StageContribution> contributions = new ArrayList<>();
        for (PipelineContributor contributor : FabricLoader.getInstance().getEntrypoints(PipelineContributor.ENTRYPOINT_KEY, PipelineContributor.class)) {
            if (contributor == null) {
                continue;
            }

            try {
                Collection<StageContribution> contributedStages = contributor.stageContributions();
                if (contributedStages == null) {
                    continue;
                }

                for (StageContribution stageContribution : contributedStages) {
                    if (stageContribution != null) {
                        contributions.add(stageContribution);
                    }
                }
            } catch (LinkageError | RuntimeException exception) {
                ScamScreenerMod.LOGGER.warn("Failed to load ScamScreener pipeline contributions from {}.", contributor.getClass().getName(), exception);
            }
        }

        if (!contributions.isEmpty()) {
            ScamScreenerMod.LOGGER.info("Loaded {} external ScamScreener pipeline contributions.", contributions.size());
        }
        return List.copyOf(contributions);
    }

    private static String normalizeLookupName(String playerName) {
        if (playerName == null) {
            return "";
        }

        return playerName.trim().toLowerCase(Locale.ROOT);
    }
}
