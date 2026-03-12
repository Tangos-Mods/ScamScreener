package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import eu.tango.scamscreener.training.TrainingCaseMappings;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes the configured pipeline stages in order and produces a final decision.
 *
 * <p>The engine is intentionally small: it runs each stage, accumulates score,
 * collects reasons, and stops early when a stage returns a terminal decision.
 */
@Getter
public final class PipelineEngine {
    private final List<Stage> stages;
    private final int reviewThreshold;

    /**
     * Creates a pipeline engine with the default review threshold.
     *
     * @param stages the ordered stage list to execute
     */
    public PipelineEngine(@NonNull List<Stage> stages) {
        this(stages, 1);
    }

    /**
     * Creates a pipeline engine with an explicit review threshold.
     *
     * @param stages the ordered stage list to execute
     * @param reviewThreshold the minimum total score that upgrades the result to review
     */
    public PipelineEngine(@NonNull List<Stage> stages, int reviewThreshold) {
        // Freeze the stage order at construction time so runtime execution stays deterministic.
        this.stages = List.copyOf(stages);
        // Negative thresholds do not make semantic sense, so clamp them to zero.
        this.reviewThreshold = Math.max(0, reviewThreshold);
    }

    /**
     * Runs the full stage list for a single chat event.
     *
     * @param chatEvent the chat event received from the client
     * @return the final decision produced by the pipeline
     */
    public PipelineDecision evaluate(ChatEvent chatEvent) {
        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("pipeline.total", "Pipeline")) {
            List<StageResult> stageResults = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            int totalScore = 0;

            for (Stage stage : stages) {
                if (stage == null) {
                    // Ignore missing stage entries instead of failing the whole pipeline.
                    continue;
                }

                StageResult result;
                try (ScamScreenerProfiler.Scope stageScope = ScamScreenerProfiler.getInstance().scope(
                    stagePhaseKey(stage),
                    stagePhaseLabel(stage)
                )) {
                    result = stage.apply(chatEvent);
                }
                if (result == null) {
                    // Treat null as a neutral no-op so buggy stages fail soft by default.
                    result = StageResult.pass(stage.name());
                }

                stageResults.add(result);
                totalScore += result.getScoreDelta();
                if (result.hasReason()) {
                    reasons.add(result.getReason());
                }

                if (result.getDecision() == Stage.Decision.MUTE) {
                    return new PipelineDecision(
                        PipelineDecision.Outcome.MUTED,
                        totalScore,
                        result.getStageName(),
                        stageResults,
                        reasons
                    );
                }

                if (result.getDecision() == Stage.Decision.WHITELIST) {
                    return new PipelineDecision(
                        PipelineDecision.Outcome.WHITELISTED,
                        totalScore,
                        result.getStageName(),
                        stageResults,
                        reasons
                    );
                }

                if (result.getDecision() == Stage.Decision.BLACKLIST) {
                    return new PipelineDecision(
                        PipelineDecision.Outcome.BLACKLISTED,
                        totalScore,
                        result.getStageName(),
                        stageResults,
                        reasons
                    );
                }

                if (result.getDecision() == Stage.Decision.ALLOW) {
                    return new PipelineDecision(
                        PipelineDecision.Outcome.ALLOW,
                        totalScore,
                        result.getStageName(),
                        stageResults,
                        reasons
                    );
                }

                if (result.getDecision() == Stage.Decision.BLOCK) {
                    return new PipelineDecision(
                        PipelineDecision.Outcome.BLOCK,
                        totalScore,
                        result.getStageName(),
                        stageResults,
                        reasons
                    );
                }
            }

            if (totalScore >= reviewThreshold) {
                // Aggregated score crossed the configured threshold, so review is warranted.
                return new PipelineDecision(
                    PipelineDecision.Outcome.REVIEW,
                    totalScore,
                    "",
                    stageResults,
                    reasons
                );
            }

            // No stage made a hard decision and the score stayed below the review threshold.
            return new PipelineDecision(
                PipelineDecision.Outcome.IGNORE,
                totalScore,
                "",
                stageResults,
                reasons
            );
        }
    }

    /**
     * Runs the full stage list for a single raw chat message.
     *
     * @param chatMessage the raw chat message text received from the client
     * @return the final decision produced by the pipeline
     */
    public PipelineDecision evaluate(String chatMessage) {
        // Preserve the low-friction string API while moving the real contract to ChatEvent.
        return evaluate(ChatEvent.messageOnly(chatMessage));
    }

    private static String stagePhaseKey(Stage stage) {
        return "pipeline." + TrainingCaseMappings.stageId(stage == null ? "" : stage.name());
    }

    private static String stagePhaseLabel(Stage stage) {
        return "  " + TrainingCaseMappings.stageLabel(stage == null ? "" : stage.name());
    }
}
