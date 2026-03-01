package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.pipeline.stage.BehaviorStage;
import eu.tango.scamscreener.pipeline.stage.FunnelStage;
import eu.tango.scamscreener.pipeline.stage.LevenshteinStage;
import eu.tango.scamscreener.pipeline.stage.ModelStage;
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
     * @return the built-in ScamScreener v2 stage order
     */
    public List<Stage> createDefaultStages(Whitelist whitelist, Blacklist blacklist) {
        // Keep the order aligned with the public API contract and design docs.
        return List.of(
            new MuteStage(),
            new PlayerListStage(whitelist, blacklist),
            new RuleStage(),
            new LevenshteinStage(),
            new BehaviorStage(),
            new TrendStage(),
            new FunnelStage(),
            new ModelStage()
        );
    }

    /**
     * Creates the default engine using the built-in core stage order.
     *
     * @param whitelist the runtime whitelist shared by the pipeline
     * @param blacklist the runtime blacklist shared by the pipeline
     * @return a pipeline engine ready to execute the core stage sequence
     */
    public PipelineEngine createDefaultEngine(Whitelist whitelist, Blacklist blacklist) {
        // Use the engine defaults until the dedicated v2 config object exists.
        return new PipelineEngine(createDefaultStages(whitelist, blacklist));
    }

    /**
     * Creates the default engine with an explicit review threshold.
     *
     * @param whitelist the runtime whitelist shared by the pipeline
     * @param blacklist the runtime blacklist shared by the pipeline
     * @param reviewThreshold the score needed for a review outcome
     * @return a pipeline engine with the configured review threshold
     */
    public PipelineEngine createDefaultEngine(Whitelist whitelist, Blacklist blacklist, int reviewThreshold) {
        // Allow callers to override the default threshold without rebuilding the stage list.
        return new PipelineEngine(createDefaultStages(whitelist, blacklist), reviewThreshold);
    }
}
