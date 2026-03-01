package eu.tango.scamscreener.pipeline.data;

import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 * Immutable final result returned by the full pipeline execution.
 *
 * <p>This is the single object the caller receives after all stages have run
 * or after a stage triggered an early terminal decision.
 */
@Getter
public final class PipelineDecision {
    private final Outcome outcome;
    private final int totalScore;
    private final String decidedByStage;
    private final List<StageResult> stageResults;
    private final List<String> reasons;

    /**
     * Creates a final pipeline decision.
     *
     * @param outcome the final outcome of the pipeline
     * @param totalScore the accumulated score across all executed stages
     * @param decidedByStage the stage that produced the final outcome
     * @param stageResults the ordered results returned by executed stages
     * @param reasons the collected non-empty reasons returned by stages
     */
    public PipelineDecision(
        @NonNull Outcome outcome,
        int totalScore,
        String decidedByStage,
        List<StageResult> stageResults,
        List<String> reasons
    ) {
        this.outcome = outcome;
        this.totalScore = totalScore;
        // Normalize missing stage names so callers never need null checks.
        this.decidedByStage = decidedByStage == null ? "" : decidedByStage.trim();
        // Freeze lists at the boundary so the result stays immutable.
        this.stageResults = stageResults == null ? List.of() : List.copyOf(stageResults);
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    /**
     * Indicates whether the pipeline reached a terminal outcome.
     *
     * @return {@code true} when the final outcome is no longer neutral
     */
    public boolean isTerminal() {
        // Anything except IGNORE is an explicit final outcome.
        return outcome != Outcome.IGNORE;
    }

    /**
     * The final high-level outcome of the pipeline.
     */
    public enum Outcome {
        /**
         * The message should be ignored without any further action.
         */
        IGNORE,
        /**
         * The message was suppressed by mute logic before further analysis.
         */
        MUTED,
        /**
         * The message bypassed further analysis because it matched whitelist logic.
         */
        WHITELISTED,
        /**
         * The message matched an explicit blacklist entry.
         */
        BLACKLISTED,
        /**
         * The message is allowed as safe.
         */
        ALLOW,
        /**
         * The message is suspicious enough for review, but not a hard block.
         */
        REVIEW,
        /**
         * The message is unsafe and should be blocked.
         */
        BLOCK
    }
}
