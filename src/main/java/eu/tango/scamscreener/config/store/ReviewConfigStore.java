package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.ReviewConfig;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewPersistenceSanitizer;
import eu.tango.scamscreener.review.ReviewStore;
import eu.tango.scamscreener.review.ReviewVerdict;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-backed store for the persisted anonymized review queue.
 */
public final class ReviewConfigStore extends BaseConfig<ReviewConfig> {
    /**
     * Creates the review config store bound to {@code review.json}.
     */
    public ReviewConfigStore() {
        super(ConfigPaths.reviewFile(), ReviewConfig.class);
    }

    /**
     * Loads the persisted review queue into the provided runtime store.
     *
     * @param reviewStore the runtime review store to populate
     */
    public void loadInto(ReviewStore reviewStore) {
        ReviewConfig config = loadOrCreate();
        List<ReviewEntry> entries = new ArrayList<>();
        for (ReviewConfig.ReviewConfigEntry storedEntry : config.entries()) {
            ReviewEntry runtimeEntry = toRuntimeEntry(storedEntry);
            if (runtimeEntry != null) {
                entries.add(runtimeEntry);
            }
        }

        reviewStore.replaceAll(entries);
        saveFrom(reviewStore);
    }

    /**
     * Saves the provided review store to disk.
     *
     * @param reviewStore the runtime review store to persist
     */
    public void saveFrom(ReviewStore reviewStore) {
        ReviewConfig config = new ReviewConfig();
        for (ReviewEntry entry : reviewStore.entries()) {
            ReviewConfig.ReviewConfigEntry storedEntry = toStoredEntry(entry);
            if (storedEntry != null) {
                config.getEntries().add(storedEntry);
            }
        }

        save(config);
    }

    @Override
    protected ReviewConfig createDefault() {
        return new ReviewConfig();
    }

    static ReviewEntry toRuntimeEntry(ReviewConfig.ReviewConfigEntry storedEntry) {
        if (storedEntry == null || storedEntry.getId() == null || storedEntry.getId().isBlank()) {
            return null;
        }

        String legacySenderName = storedEntry.getSenderName();
        List<StageResult> stageResults = new ArrayList<>();
        for (ReviewConfig.ReviewStageResult storedStageResult : storedEntry.stageResults()) {
            if (storedStageResult == null || storedStageResult.getStageName() == null || storedStageResult.getStageName().isBlank()) {
                continue;
            }

            stageResults.add(
                StageResult.of(
                    storedStageResult.getStageName(),
                    storedStageResult.getStageId(),
                    storedStageResult.getDecision(),
                    storedStageResult.getScoreDelta(),
                    ReviewPersistenceSanitizer.sanitizePersistedText(storedStageResult.getReason(), legacySenderName),
                    storedStageResult.reasonIds()
                )
            );
        }

        List<ReviewCaseMessage> caseMessages = new ArrayList<>();
        for (ReviewConfig.ReviewCaseMessageConfig storedCaseMessage : storedEntry.caseMessages()) {
            if (storedCaseMessage == null) {
                continue;
            }

            String cleanText = ReviewPersistenceSanitizer.sanitizePersistedText(storedCaseMessage.getCleanText(), legacySenderName);
            if (cleanText.isBlank()) {
                continue;
            }

            caseMessages.add(new ReviewCaseMessage(
                storedCaseMessage.getMessageIndex(),
                storedCaseMessage.getSpeakerRole(),
                storedCaseMessage.getMessageSourceType(),
                cleanText,
                storedCaseMessage.isTriggerMessage(),
                storedCaseMessage.getCaseRole(),
                storedCaseMessage.signalTagIds(),
                ReviewPersistenceSanitizer.sanitizePersistedTextList(storedCaseMessage.advancedRuleSelections(), legacySenderName)
            ));
        }

        ReviewEntry entry = new ReviewEntry(
            storedEntry.getId(),
            null,
            "",
            ReviewPersistenceSanitizer.sanitizePersistedMessage(storedEntry.getMessage(), legacySenderName),
            storedEntry.getScore(),
            storedEntry.getDecidedByStage(),
            storedEntry.getCapturedAtMs(),
            ReviewPersistenceSanitizer.sanitizePersistedTextList(storedEntry.reasons(), legacySenderName),
            stageResults,
            caseMessages
        );
        entry.setVerdict(storedEntry.getVerdict() == null ? ReviewVerdict.PENDING : storedEntry.getVerdict());
        return entry;
    }

    static ReviewConfig.ReviewConfigEntry toStoredEntry(ReviewEntry entry) {
        if (entry == null || entry.getId().isBlank()) {
            return null;
        }

        ReviewConfig.ReviewConfigEntry storedEntry = new ReviewConfig.ReviewConfigEntry();
        storedEntry.setId(entry.getId());
        storedEntry.setMessage(ReviewPersistenceSanitizer.sanitizePersistedMessage(entry.getMessage(), entry.getSenderName()));
        storedEntry.setScore(entry.getScore());
        storedEntry.setDecidedByStage(entry.getDecidedByStage());
        storedEntry.setCapturedAtMs(entry.getCapturedAtMs());
        storedEntry.setVerdict(entry.getVerdict());
        storedEntry.getReasons().addAll(ReviewPersistenceSanitizer.sanitizePersistedTextList(entry.getReasons(), entry.getSenderName()));

        for (StageResult stageResult : entry.getStageResults()) {
            if (stageResult == null) {
                continue;
            }

            ReviewConfig.ReviewStageResult storedStageResult = new ReviewConfig.ReviewStageResult();
            storedStageResult.setStageName(stageResult.getStageName());
            storedStageResult.setStageId(stageResult.getStageId());
            storedStageResult.setDecision(stageResult.getDecision());
            storedStageResult.setScoreDelta(stageResult.getScoreDelta());
            storedStageResult.setReason(ReviewPersistenceSanitizer.sanitizePersistedText(stageResult.getReason(), entry.getSenderName()));
            storedStageResult.getReasonIds().addAll(stageResult.getReasonIds());
            storedEntry.getStageResults().add(storedStageResult);
        }

        for (ReviewCaseMessage caseMessage : entry.getCaseMessages()) {
            if (caseMessage == null) {
                continue;
            }

            String cleanText = ReviewPersistenceSanitizer.sanitizePersistedText(caseMessage.getCleanText(), entry.getSenderName());
            if (cleanText.isBlank()) {
                continue;
            }

            ReviewConfig.ReviewCaseMessageConfig storedCaseMessage = new ReviewConfig.ReviewCaseMessageConfig();
            storedCaseMessage.setMessageIndex(caseMessage.getMessageIndex());
            storedCaseMessage.setSpeakerRole(caseMessage.getSpeakerRole());
            storedCaseMessage.setMessageSourceType(caseMessage.getMessageSourceType());
            storedCaseMessage.setCleanText(cleanText);
            storedCaseMessage.setTriggerMessage(caseMessage.isTriggerMessage());
            storedCaseMessage.setCaseRole(caseMessage.getCaseRole());
            storedCaseMessage.getSignalTagIds().addAll(caseMessage.getSignalTagIds());
            storedCaseMessage.getAdvancedRuleSelections().addAll(
                ReviewPersistenceSanitizer.sanitizePersistedTextList(caseMessage.getAdvancedRuleSelections(), entry.getSenderName())
            );
            storedEntry.getCaseMessages().add(storedCaseMessage);
        }

        return storedEntry;
    }
}
