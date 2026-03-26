package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.api.StageContribution;
import eu.tango.scamscreener.api.StageSlot;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the default ScamScreener v2 pipeline layout.
 *
 * <p>This centralizes the core stage order so the rest of the codebase does not
 * hardcode the pipeline sequence in multiple places.
 */
@UtilityClass
public class ScamScreenerPipelineFactory {
    private static final List<StageSlot> CORE_STAGE_ORDER = List.of(
        StageSlot.MUTE,
        StageSlot.PLAYER_LIST,
        StageSlot.RULE,
        StageSlot.LEVENSHTEIN,
        StageSlot.BEHAVIOR,
        StageSlot.TREND,
        StageSlot.FUNNEL,
        StageSlot.MODEL
    );

    public List<StageSlot> coreStageOrder() {
        return CORE_STAGE_ORDER;
    }

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
        return createDefaultStages(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache,
            List.of()
        );
    }

    public List<Stage> createDefaultStages(
        Whitelist whitelist,
        Blacklist blacklist,
        RulesConfig rulesConfig,
        BehaviorStore behaviorStore,
        TrendStore trendStore,
        FunnelStore funnelStore,
        RecentChatCache recentChatCache,
        Iterable<StageContribution> stageContributions
    ) {
        RuleCatalog ruleCatalog = new RuleCatalog(rulesConfig);
        Map<StageSlot, Stage> coreStages = new EnumMap<>(StageSlot.class);
        coreStages.put(StageSlot.MUTE, new MuteStage(ruleCatalog));
        coreStages.put(StageSlot.PLAYER_LIST, new PlayerListStage(whitelist, blacklist));
        coreStages.put(StageSlot.RULE, new RuleStage(ruleCatalog));
        coreStages.put(StageSlot.LEVENSHTEIN, new LevenshteinStage(ruleCatalog));
        coreStages.put(StageSlot.BEHAVIOR, new BehaviorStage(behaviorStore, ruleCatalog));
        coreStages.put(StageSlot.TREND, new TrendStage(trendStore, ruleCatalog));
        coreStages.put(StageSlot.FUNNEL, new FunnelStage(funnelStore, ruleCatalog));
        // The public MODEL slot currently maps to the final context-aware stage.
        coreStages.put(StageSlot.MODEL, new ContextStage(recentChatCache, ruleCatalog));
        return orderedStages(
            coreStages,
            stageContributions
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
        return createDefaultEngine(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache,
            1,
            List.of()
        );
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
        return createDefaultEngine(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache,
            reviewThreshold,
            List.of()
        );
    }

    public PipelineEngine createDefaultEngine(
        Whitelist whitelist,
        Blacklist blacklist,
        RulesConfig rulesConfig,
        BehaviorStore behaviorStore,
        TrendStore trendStore,
        FunnelStore funnelStore,
        RecentChatCache recentChatCache,
        int reviewThreshold,
        Iterable<StageContribution> stageContributions
    ) {
        return new PipelineEngine(createDefaultStages(
            whitelist,
            blacklist,
            rulesConfig,
            behaviorStore,
            trendStore,
            funnelStore,
            recentChatCache,
            stageContributions
        ), reviewThreshold);
    }

    static List<Stage> orderedStages(Map<StageSlot, Stage> coreStages, Iterable<StageContribution> stageContributions) {
        Map<StageSlot, List<Stage>> beforeStages = new EnumMap<>(StageSlot.class);
        Map<StageSlot, List<Stage>> afterStages = new EnumMap<>(StageSlot.class);
        if (stageContributions != null) {
            for (StageContribution stageContribution : stageContributions) {
                if (stageContribution == null || stageContribution.getStage() == null || stageContribution.getSlot() == null
                    || stageContribution.getPosition() == null) {
                    continue;
                }

                Map<StageSlot, List<Stage>> targetStages = stageContribution.getPosition() == StageContribution.Position.BEFORE
                    ? beforeStages
                    : afterStages;
                targetStages.computeIfAbsent(stageContribution.getSlot(), ignored -> new ArrayList<>()).add(stageContribution.getStage());
            }
        }

        List<Stage> orderedStages = new ArrayList<>();
        for (StageSlot stageSlot : CORE_STAGE_ORDER) {
            orderedStages.addAll(beforeStages.getOrDefault(stageSlot, List.of()));
            Stage coreStage = coreStages == null ? null : coreStages.get(stageSlot);
            if (coreStage != null) {
                orderedStages.add(coreStage);
            }
            orderedStages.addAll(afterStages.getOrDefault(stageSlot, List.of()));
        }
        return List.copyOf(orderedStages);
    }
}
