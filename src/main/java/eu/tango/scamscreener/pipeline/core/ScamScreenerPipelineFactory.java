package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;
import eu.tango.scamscreener.pipeline.state.FunnelStore;
import eu.tango.scamscreener.pipeline.state.TrendStore;
import eu.tango.scamscreener.pipeline.stage.BehaviorStage;
import eu.tango.scamscreener.pipeline.stage.ContextStage;
import eu.tango.scamscreener.pipeline.stage.FunnelStage;
import eu.tango.scamscreener.pipeline.stage.LevenshteinStage;
import eu.tango.scamscreener.pipeline.stage.MuteStage;
import eu.tango.scamscreener.pipeline.stage.PlayerListStage;
import eu.tango.scamscreener.pipeline.stage.RuleStage;
import eu.tango.scamscreener.pipeline.stage.TrendStage;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Builds the default ScamScreener v2 pipeline layout.
 *
 * <p>This centralizes the core stage order so the rest of the codebase does not
 * hardcode the pipeline sequence in multiple places.
 */
@UtilityClass
public class ScamScreenerPipelineFactory {
    /**
     * Creates the default ordered core stage list.
     *
     * @param whitelist the runtime whitelist shared by the pipeline
     * @param blacklist the runtime blacklist shared by the pipeline
     * @param rulesConfig the loaded rule configuration shared by rule-driven stages
     * @param behaviorStore the shared sender-local behavior store
     * @param trendStore the shared cross-sender trend store
     * @param funnelStore the shared sender-local funnel store
     * @param recentChatCache the shared recent-chat cache
     * @return the built-in ScamScreener v2 stage order
     */
    public List<Stage> createDefaultStages(
        Whitelist whitelist,
        Blacklist blacklist,
        RulesConfig rulesConfig,
        BehaviorStore behaviorStore,
        TrendStore trendStore,
        FunnelStore funnelStore,
        RecentChatCache recentChatCache
    ) {
        RuleCatalog ruleCatalog = new RuleCatalog(rulesConfig);

        // Keep the order aligned with the public API contract and design docs.
        return List.of(
            new MuteStage(ruleCatalog),
            new PlayerListStage(whitelist, blacklist),
            new RuleStage(ruleCatalog),
            new LevenshteinStage(ruleCatalog),
            new BehaviorStage(behaviorStore, ruleCatalog),
            new TrendStage(trendStore, ruleCatalog),
            new FunnelStage(funnelStore, ruleCatalog),
            new ContextStage(recentChatCache, ruleCatalog)
        );
    }

    /**
     * Creates the default engine using the built-in core stage order.
     *
     * @param whitelist the runtime whitelist shared by the pipeline
     * @param blacklist the runtime blacklist shared by the pipeline
     * @param rulesConfig the loaded rule configuration shared by rule-driven stages
     * @param behaviorStore the shared sender-local behavior store
     * @param trendStore the shared cross-sender trend store
     * @param funnelStore the shared sender-local funnel store
     * @param recentChatCache the shared recent-chat cache
     * @return a pipeline engine ready to execute the core stage sequence
     */
    public PipelineEngine createDefaultEngine(
        Whitelist whitelist,
        Blacklist blacklist,
        RulesConfig rulesConfig,
        BehaviorStore behaviorStore,
        TrendStore trendStore,
        FunnelStore funnelStore,
        RecentChatCache recentChatCache
    ) {
        // Use the engine defaults until the dedicated v2 config object exists.
        return new PipelineEngine(createDefaultStages(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache
        ));
    }

    /**
     * Creates the default engine with an explicit review threshold.
     *
     * @param whitelist the runtime whitelist shared by the pipeline
     * @param blacklist the runtime blacklist shared by the pipeline
     * @param rulesConfig the loaded rule configuration shared by rule-driven stages
     * @param behaviorStore the shared sender-local behavior store
     * @param trendStore the shared cross-sender trend store
     * @param funnelStore the shared sender-local funnel store
     * @param recentChatCache the shared recent-chat cache
     * @param reviewThreshold the score needed for a review outcome
     * @return a pipeline engine with the configured review threshold
     */
    public PipelineEngine createDefaultEngine(
        Whitelist whitelist,
        Blacklist blacklist,
        RulesConfig rulesConfig,
        BehaviorStore behaviorStore,
        TrendStore trendStore,
        FunnelStore funnelStore,
        RecentChatCache recentChatCache,
        int reviewThreshold
    ) {
        // Allow callers to override the default threshold without rebuilding the stage list.
        return new PipelineEngine(createDefaultStages(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache
        ), reviewThreshold);
    }
}
