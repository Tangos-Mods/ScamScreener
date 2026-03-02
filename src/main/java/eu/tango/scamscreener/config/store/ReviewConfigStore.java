package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.ReviewConfig;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewStore;
import eu.tango.scamscreener.review.ReviewVerdict;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JSON-backed store for the persisted review queue.
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

    private static ReviewEntry toRuntimeEntry(ReviewConfig.ReviewConfigEntry storedEntry) {
        if (storedEntry == null || storedEntry.getId() == null || storedEntry.getId().isBlank()) {
            return null;
        }

        UUID senderUuid = parseUuid(storedEntry.getSenderUuid());
        List<StageResult> stageResults = new ArrayList<>();
        for (ReviewConfig.ReviewStageResult storedStageResult : storedEntry.stageResults()) {
            if (storedStageResult == null || storedStageResult.getStageName() == null || storedStageResult.getStageName().isBlank()) {
                continue;
            }

            stageResults.add(
                StageResult.of(
                    storedStageResult.getStageName(),
                    storedStageResult.getDecision(),
                    storedStageResult.getScoreDelta(),
                    storedStageResult.getReason()
                )
            );
        }

        ReviewEntry entry = new ReviewEntry(
            storedEntry.getId(),
            senderUuid,
            storedEntry.getSenderName(),
            storedEntry.getMessage(),
            storedEntry.getScore(),
            storedEntry.getDecidedByStage(),
            storedEntry.getCapturedAtMs(),
            storedEntry.reasons(),
            stageResults
        );
        entry.setVerdict(storedEntry.getVerdict() == null ? ReviewVerdict.PENDING : storedEntry.getVerdict());
        return entry;
    }

    private static ReviewConfig.ReviewConfigEntry toStoredEntry(ReviewEntry entry) {
        if (entry == null || entry.getId().isBlank()) {
            return null;
        }

        ReviewConfig.ReviewConfigEntry storedEntry = new ReviewConfig.ReviewConfigEntry();
        storedEntry.setId(entry.getId());
        storedEntry.setSenderUuid(entry.getSenderUuid() == null ? "" : entry.getSenderUuid().toString());
        storedEntry.setSenderName(entry.getSenderName());
        storedEntry.setMessage(entry.getMessage());
        storedEntry.setScore(entry.getScore());
        storedEntry.setDecidedByStage(entry.getDecidedByStage());
        storedEntry.setCapturedAtMs(entry.getCapturedAtMs());
        storedEntry.setVerdict(entry.getVerdict());
        storedEntry.getReasons().addAll(entry.getReasons());

        for (StageResult stageResult : entry.getStageResults()) {
            if (stageResult == null) {
                continue;
            }

            ReviewConfig.ReviewStageResult storedStageResult = new ReviewConfig.ReviewStageResult();
            storedStageResult.setStageName(stageResult.getStageName());
            storedStageResult.setDecision(stageResult.getDecision());
            storedStageResult.setScoreDelta(stageResult.getScoreDelta());
            storedStageResult.setReason(stageResult.getReason());
            storedEntry.getStageResults().add(storedStageResult);
        }

        return storedEntry;
    }

    private static UUID parseUuid(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawUuid.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
