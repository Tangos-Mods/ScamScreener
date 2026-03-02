package eu.tango.scamscreener.config.data;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.review.ReviewVerdict;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted review queue payload stored in {@code review.json}.
 */
@Getter
@Setter
@NoArgsConstructor
public final class ReviewConfig {
    private List<ReviewConfigEntry> entries = new ArrayList<>();

    /**
     * Returns the normalized persisted review entries.
     *
     * @return non-null review entries
     */
    public List<ReviewConfigEntry> entries() {
        return entries == null ? new ArrayList<>() : entries;
    }

    /**
     * One persisted review queue entry.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class ReviewConfigEntry {
        private String id = "";
        private String senderUuid = "";
        private String senderName = "";
        private String message = "";
        private int score;
        private String decidedByStage = "";
        private long capturedAtMs;
        private ReviewVerdict verdict = ReviewVerdict.PENDING;
        private List<String> reasons = new ArrayList<>();
        private List<ReviewStageResult> stageResults = new ArrayList<>();

        /**
         * Returns the normalized reasons.
         *
         * @return non-null review reasons
         */
        public List<String> reasons() {
            return reasons == null ? new ArrayList<>() : reasons;
        }

        /**
         * Returns the normalized persisted stage results.
         *
         * @return non-null stage trace entries
         */
        public List<ReviewStageResult> stageResults() {
            return stageResults == null ? new ArrayList<>() : stageResults;
        }
    }

    /**
     * Persisted stage trace entry stored alongside a review entry.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class ReviewStageResult {
        private String stageName = "";
        private Stage.Decision decision = Stage.Decision.PASS;
        private int scoreDelta;
        private String reason = "";
    }
}
