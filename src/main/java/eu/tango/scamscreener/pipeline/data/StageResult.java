package eu.tango.scamscreener.pipeline.data;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.training.TrainingCaseMappings;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 * Immutable result returned by a pipeline stage.
 *
 * <p>It combines control flow for the pipeline with a score contribution and
 * an optional reason that can be logged or displayed later.
 */
@Getter
public final class StageResult {
    private final String stageName;
    private final String stageId;
    private final Stage.Decision decision;
    private final int scoreDelta;
    private final String reason;
    private final List<String> reasonIds;

    private StageResult(
        @NonNull String stageName,
        @NonNull String stageId,
        @NonNull Stage.Decision decision,
        int scoreDelta,
        String reason,
        List<String> reasonIds
    ) {
        this.stageName = stageName;
        this.stageId = stageId;
        this.decision = decision;
        this.scoreDelta = scoreDelta;
        // Normalize missing reasons so callers never need null checks.
        this.reason = reason == null ? "" : reason.trim();
        this.reasonIds = reasonIds == null ? List.of() : List.copyOf(reasonIds);
    }

    /**
     * Creates a result with full control over all fields.
     *
     * @param stageName the stage that produced the result
     * @param decision the pipeline decision for this stage
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional reason code or human-readable note
     * @return a new immutable stage result
     */
    public static StageResult of(String stageName, Stage.Decision decision, int scoreDelta, String reason) {
        // Route all creation through the constructor so validation stays in one place.
        return of(
            stageName,
            TrainingCaseMappings.stageId(stageName),
            decision,
            scoreDelta,
            reason,
            TrainingCaseMappings.reasonIds(stageName, reason)
        );
    }

    /**
     * Creates a result with explicit stable identifiers.
     *
     * @param stageName the human-readable stage name
     * @param stageId the stable stage identifier
     * @param decision the pipeline decision for this stage
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional reason code or human-readable note
     * @param reasonIds the stable reason identifiers derived for this result
     * @return a new immutable stage result
     */
    public static StageResult of(
        String stageName,
        String stageId,
        Stage.Decision decision,
        int scoreDelta,
        String reason,
        List<String> reasonIds
    ) {
        return new StageResult(
            stageName == null ? "" : stageName.trim(),
            stageId == null || stageId.isBlank() ? TrainingCaseMappings.stageId(stageName) : TrainingCaseMappings.stageId(stageId),
            decision == null ? Stage.Decision.PASS : decision,
            scoreDelta,
            reason,
            reasonIds == null || reasonIds.isEmpty()
                ? TrainingCaseMappings.reasonIds(stageId == null || stageId.isBlank() ? stageName : stageId, reason)
                : reasonIds
        );
    }

    /**
     * Creates a neutral result that lets the pipeline continue unchanged.
     *
     * @param stageName the stage that produced the result
     * @return a pass result with no score contribution
     */
    public static StageResult pass(String stageName) {
        // A pass result does not change score and does not stop the pipeline.
        return of(stageName, Stage.Decision.PASS, 0, "");
    }

    /**
     * Creates a mute result that short-circuits the remaining stages.
     *
     * @param stageName the stage that produced the result
     * @param reason an optional explanation for the mute decision
     * @return a mute result with no score contribution
     */
    public static StageResult mute(String stageName, String reason) {
        // Mute is a hard suppression decision for early filtering stages.
        return of(stageName, Stage.Decision.MUTE, 0, reason);
    }

    /**
     * Creates a whitelist result that short-circuits the remaining stages.
     *
     * @param stageName the stage that produced the result
     * @param reason an optional explanation for the whitelist decision
     * @return a whitelist result with no score contribution
     */
    public static StageResult whitelist(String stageName, String reason) {
        // Whitelist is a hard trust decision for known-safe senders or messages.
        return of(stageName, Stage.Decision.WHITELIST, 0, reason);
    }

    /**
     * Creates a blacklist result that short-circuits the remaining stages.
     *
     * @param stageName the stage that produced the result
     * @param reason an optional explanation for the blacklist decision
     * @return a blacklist result with no score contribution
     */
    public static StageResult blacklist(String stageName, String reason) {
        // Blacklist is a hard deny decision for explicitly blocked players.
        return of(stageName, Stage.Decision.BLACKLIST, 0, reason);
    }

    /**
     * Creates a positive allow result that short-circuits the remaining stages.
     *
     * @param stageName the stage that produced the result
     * @param reason an optional explanation for the allow decision
     * @return an allow result with no score contribution
     */
    public static StageResult allow(String stageName, String reason) {
        // Allow is a hard decision, so later stages can be skipped.
        return of(stageName, Stage.Decision.ALLOW, 0, reason);
    }

    /**
     * Creates a blocking result that stops the pipeline immediately.
     *
     * @param stageName the stage that produced the result
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional explanation for the block decision
     * @return a block result
     */
    public static StageResult block(String stageName, int scoreDelta, String reason) {
        // Blocking can still carry a score so the final report keeps context.
        return of(stageName, Stage.Decision.BLOCK, scoreDelta, reason);
    }

    /**
     * Creates a scoring result without ending the pipeline.
     *
     * @param stageName the stage that produced the result
     * @param scoreDelta the score contribution of this stage
     * @param reason an optional explanation for the score change
     * @return a pass result with a score contribution
     */
    public static StageResult score(String stageName, int scoreDelta, String reason) {
        // Scoring keeps the pipeline running while recording suspicion changes.
        return of(stageName, Stage.Decision.PASS, scoreDelta, reason);
    }

    /**
     * Indicates whether this result contains a non-empty reason.
     *
     * @return {@code true} when a reason is present
     */
    public boolean hasReason() {
        // Empty string is our sentinel for "no reason supplied".
        return !reason.isEmpty();
    }

    /**
     * Indicates whether the result carries at least one stable reason id.
     *
     * @return {@code true} when stable reason ids are present
     */
    public boolean hasReasonIds() {
        return !reasonIds.isEmpty();
    }
}
