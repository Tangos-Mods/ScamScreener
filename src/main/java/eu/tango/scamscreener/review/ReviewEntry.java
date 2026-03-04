package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.StageResult;
import lombok.Getter;

import java.util.ArrayList;
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
    private final List<ReviewCaseMessage> caseMessages;
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
        this(
            id,
            senderUuid,
            senderName,
            message,
            score,
            decidedByStage,
            capturedAtMs,
            reasons,
            stageResults,
            List.of()
        );
    }

    /**
     * Creates a stored review entry with case-level message context.
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
     * @param caseMessages the normalized case messages captured with this review
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
        List<StageResult> stageResults,
        List<ReviewCaseMessage> caseMessages
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
        this.caseMessages = normalizeCaseMessages(caseMessages, this.message);
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

    /**
     * Indicates whether this entry contains case-level message context.
     *
     * @return {@code true} when at least one case message is present
     */
    public boolean hasCaseMessages() {
        return !caseMessages.isEmpty();
    }

    /**
     * Replaces the captured case-level review messages.
     *
     * @param caseMessages the new case messages
     */
    public void replaceCaseMessages(List<ReviewCaseMessage> caseMessages) {
        this.caseMessages.clear();
        this.caseMessages.addAll(normalizeCaseMessages(caseMessages, message));
    }

    private static List<ReviewCaseMessage> normalizeCaseMessages(List<ReviewCaseMessage> caseMessages, String message) {
        List<ReviewCaseMessage> normalizedCaseMessages = new ArrayList<>();
        if (caseMessages != null) {
            for (ReviewCaseMessage caseMessage : caseMessages) {
                if (caseMessage != null) {
                    normalizedCaseMessages.add(caseMessage);
                }
            }
        }
        if (normalizedCaseMessages.isEmpty()) {
            normalizedCaseMessages.add(new ReviewCaseMessage(
                0,
                "other",
                "player",
                message,
                true,
                ReviewCaseRole.SIGNAL,
                List.of(),
                List.of()
            ));
        }

        return normalizedCaseMessages;
    }
}
