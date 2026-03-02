package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory queue of review entries.
 */
public final class ReviewStore {
    private static final int DEFAULT_MAX_ENTRIES = 200;

    private final List<ReviewEntry> entries = new ArrayList<>();
    private final AtomicLong nextId = new AtomicLong();
    private final Runnable saveHook;
    private int maxEntries;

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
        this(maxEntries, () -> {
        });
    }

    /**
     * Creates a review store with the default capacity and a persistence hook.
     *
     * @param saveHook callback triggered after mutating changes
     */
    public ReviewStore(Runnable saveHook) {
        this(DEFAULT_MAX_ENTRIES, saveHook);
    }

    /**
     * Creates a review store with a bounded capacity and persistence hook.
     *
     * @param maxEntries the maximum number of stored entries
     * @param saveHook callback triggered after mutating changes
     */
    public ReviewStore(int maxEntries, Runnable saveHook) {
        this.maxEntries = Math.max(1, maxEntries);
        this.saveHook = saveHook == null ? () -> {
        } : saveHook;
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
            safeEvent.getTimestampMs(),
            decision.getReasons(),
            decision.getStageResults()
        );

        entries.add(0, entry);
        trimToCapacity();
        saveHook.run();
        return Optional.of(entry);
    }

    /**
     * Returns the current queue capacity.
     *
     * @return the configured maximum number of entries
     */
    public synchronized int maxEntries() {
        return maxEntries;
    }

    /**
     * Updates the queue capacity and trims old entries when needed.
     *
     * @param maxEntries the new maximum number of entries
     */
    public synchronized void setMaxEntries(int maxEntries) {
        int normalizedMaxEntries = Math.max(1, maxEntries);
        if (this.maxEntries == normalizedMaxEntries) {
            return;
        }

        this.maxEntries = normalizedMaxEntries;
        int previousSize = entries.size();
        trimToCapacity();
        if (entries.size() != previousSize) {
            saveHook.run();
        }
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
     * Returns a filtered snapshot of the current review entries.
     *
     * @param verdictFilter the verdict to include, or {@code null} for all
     * @param searchTerm free-text search across sender, message and stage
     * @return the matching review entries, newest first
     */
    public synchronized List<ReviewEntry> entries(ReviewVerdict verdictFilter, String searchTerm) {
        String normalizedSearchTerm = normalizeSearch(searchTerm);
        if (verdictFilter == null && normalizedSearchTerm.isEmpty()) {
            return entries();
        }

        List<ReviewEntry> matches = new ArrayList<>();
        for (ReviewEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (verdictFilter != null && entry.getVerdict() != verdictFilter) {
                continue;
            }
            if (!normalizedSearchTerm.isEmpty() && !matchesSearch(entry, normalizedSearchTerm)) {
                continue;
            }

            matches.add(entry);
        }

        return List.copyOf(matches);
    }

    /**
     * Looks up one stored review entry by id.
     *
     * @param entryId the target entry id
     * @return the matching review entry, when present
     */
    public synchronized Optional<ReviewEntry> find(String entryId) {
        return Optional.ofNullable(findInternal(entryId));
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
        saveHook.run();
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

        boolean removed = entries.remove(entry);
        if (removed) {
            saveHook.run();
        }

        return removed;
    }

    /**
     * Clears all stored review entries.
     */
    public synchronized void clear() {
        if (entries.isEmpty()) {
            return;
        }

        entries.clear();
        saveHook.run();
    }

    /**
     * Replaces the current review contents without triggering persistence.
     *
     * @param entries the review entries to load
     */
    public synchronized void replaceAll(Iterable<ReviewEntry> entries) {
        this.entries.clear();
        nextId.set(0L);
        if (entries == null) {
            return;
        }

        for (ReviewEntry entry : entries) {
            if (entry == null || entry.getId().isBlank()) {
                continue;
            }

            this.entries.add(entry);
            nextId.set(Math.max(nextId.get(), parseNumericId(entry.getId())));
            trimToCapacity();
        }
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

    private static String normalizeSearch(String searchTerm) {
        if (searchTerm == null) {
            return "";
        }

        return searchTerm.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesSearch(ReviewEntry entry, String normalizedSearchTerm) {
        if (entry == null || normalizedSearchTerm.isEmpty()) {
            return false;
        }

        return contains(entry.getSenderName(), normalizedSearchTerm)
            || contains(entry.getMessage(), normalizedSearchTerm)
            || contains(entry.getDecidedByStage(), normalizedSearchTerm)
            || contains(entry.getReasons(), normalizedSearchTerm);
    }

    private static boolean contains(String value, String normalizedSearchTerm) {
        if (value == null || value.isBlank() || normalizedSearchTerm.isEmpty()) {
            return false;
        }

        return value.toLowerCase(Locale.ROOT).contains(normalizedSearchTerm);
    }

    private static boolean contains(Iterable<String> values, String normalizedSearchTerm) {
        if (values == null || normalizedSearchTerm.isEmpty()) {
            return false;
        }

        for (String value : values) {
            if (contains(value, normalizedSearchTerm)) {
                return true;
            }
        }

        return false;
    }

    private static long parseNumericId(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return 0L;
        }

        String normalizedId = entryId.trim();
        int separatorIndex = normalizedId.lastIndexOf('-');
        String numericPortion = separatorIndex < 0 ? normalizedId : normalizedId.substring(separatorIndex + 1);
        try {
            return Long.parseLong(numericPortion);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
