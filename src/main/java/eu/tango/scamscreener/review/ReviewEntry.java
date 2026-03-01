package eu.tango.scamscreener.review;

import lombok.Getter;

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
     */
    public ReviewEntry(
        String id,
        UUID senderUuid,
        String senderName,
        String message,
        int score,
        String decidedByStage,
        long capturedAtMs
    ) {
        this.id = id == null ? "" : id.trim();
        this.senderUuid = senderUuid;
        this.senderName = senderName == null ? "" : senderName.trim();
        this.message = message == null ? "" : message.trim();
        this.score = Math.max(0, score);
        this.decidedByStage = decidedByStage == null ? "" : decidedByStage.trim();
        this.capturedAtMs = capturedAtMs;
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
}
