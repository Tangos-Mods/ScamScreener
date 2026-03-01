package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory queue of review entries.
 */
public final class ReviewStore {
    private static final int DEFAULT_MAX_ENTRIES = 200;

    private final List<ReviewEntry> entries = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong();
    private final int maxEntries;

    /**
     * Creates a review store with the default capacity.
     */
    public ReviewStore() {
        this(DEFAULT_MAX_ENTRIES);
    }

    /**
     * Creates a review store with a bounded capacity.
     *
     * @param maxEntries the maximum number of stored entries
     */
    public ReviewStore(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    /**
     * Captures a new entry from a pipeline review outcome.
     *
     * @param chatEvent the reviewed chat event
     * @param decision the final pipeline decision
     * @return the created review entry, when one was captured
     */
    public synchronized Optional<ReviewEntry> capture(ChatEvent chatEvent, PipelineDecision decision) {
        if (decision == null || decision.getOutcome() != PipelineDecision.Outcome.REVIEW) {
            return Optional.empty();
        }

        ChatEvent safeEvent = chatEvent == null ? ChatEvent.messageOnly("") : chatEvent;
        ReviewEntry entry = new ReviewEntry(
            "review-" + nextId.incrementAndGet(),
            safeEvent.getSenderUuid(),
            safeEvent.getSenderName(),
            safeEvent.getRawMessage(),
            decision.getTotalScore(),
            decision.getDecidedByStage(),
            safeEvent.getTimestampMs()
        );

        entries.add(0, entry);
        trimToCapacity();
        return Optional.of(entry);
    }

    /**
     * Returns a snapshot of the current review entries.
     *
     * @return the stored review entries, newest first
     */
    public synchronized List<ReviewEntry> entries() {
        return List.copyOf(entries);
    }

    /**
     * Updates the stored verdict of one entry.
     *
     * @param entryId the target entry id
     * @param verdict the new verdict
     * @return {@code true} when an entry was updated
     */
    public synchronized boolean setVerdict(String entryId, ReviewVerdict verdict) {
        ReviewEntry entry = findInternal(entryId);
        if (entry == null) {
            return false;
        }

        entry.setVerdict(verdict);
        return true;
    }

    /**
     * Removes one review entry.
     *
     * @param entryId the target entry id
     * @return {@code true} when an entry was removed
     */
    public synchronized boolean remove(String entryId) {
        ReviewEntry entry = findInternal(entryId);
        if (entry == null) {
            return false;
        }

        return entries.remove(entry);
    }

    /**
     * Clears all stored review entries.
     */
    public synchronized void clear() {
        entries.clear();
    }

    private ReviewEntry findInternal(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return null;
        }

        for (ReviewEntry entry : entries) {
            if (entryId.equals(entry.getId())) {
                return entry;
            }
        }

        return null;
    }

    private void trimToCapacity() {
        while (entries.size() > maxEntries) {
            entries.remove(entries.size() - 1);
        }
    }
}
