package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.StageResult;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Stored review entry captured from a pipeline review outcome.
 */
@Getter
public final class ReviewEntry {
    private final String id;
    private final UUID senderUuid;
    private final String senderName;
    private final String message;
    private final int score;
    private final String decidedByStage;
    private final long capturedAtMs;
    private final List<String> reasons;
    private final List<StageResult> stageResults;
    private ReviewVerdict verdict;

    /**
     * Creates a stored review entry.
     *
     * @param id stable review entry id
     * @param senderUuid the sender UUID, if available
     * @param senderName the sender name, if available
     * @param message the reviewed message content
     * @param score the pipeline score attached to the review
     * @param decidedByStage the stage that led to the review outcome
     * @param capturedAtMs the capture timestamp in epoch milliseconds
     * @param reasons the collected review reasons from the pipeline
     * @param stageResults the ordered stage trace captured with this review
     */
    public ReviewEntry(
        String id,
        UUID senderUuid,
        String senderName,
        String message,
        int score,
        String decidedByStage,
        long capturedAtMs,
        List<String> reasons,
        List<StageResult> stageResults
    ) {
        this.id = id == null ? "" : id.trim();
        this.senderUuid = senderUuid;
        this.senderName = senderName == null ? "" : senderName.trim();
        this.message = message == null ? "" : message.trim();
        this.score = Math.max(0, score);
        this.decidedByStage = decidedByStage == null ? "" : decidedByStage.trim();
        this.capturedAtMs = capturedAtMs;
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
        this.stageResults = stageResults == null ? List.of() : List.copyOf(stageResults);
        this.verdict = ReviewVerdict.PENDING;
    }

    /**
     * Updates the review verdict.
     *
     * @param verdict the new verdict
     */
    public void setVerdict(ReviewVerdict verdict) {
        this.verdict = verdict == null ? ReviewVerdict.PENDING : verdict;
    }

    /**
     * Indicates whether this entry contains pipeline reasons.
     *
     * @return {@code true} when at least one reason is present
     */
    public boolean hasReasons() {
        return !reasons.isEmpty();
    }

    /**
     * Indicates whether this entry contains a captured stage trace.
     *
     * @return {@code true} when at least one stage result is present
     */
    public boolean hasStageResults() {
        return !stageResults.isEmpty();
    }
}
