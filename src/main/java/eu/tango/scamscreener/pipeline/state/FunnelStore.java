package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps a short sender-local step history for future funnel analysis.
 */
public final class FunnelStore implements PipelineStateStore<FunnelStore.FunnelSnapshot> {
    private static final long DEFAULT_WINDOW_MS = 300_000L;
    private static final int DEFAULT_MAX_HISTORY = 8;

    private long windowMs;
    private int maxHistory;
    private final Map<String, Deque<StepRecord>> stepsBySender = new LinkedHashMap<>();

    /**
     * Creates a funnel store with default history bounds.
     */
    public FunnelStore() {
        this(DEFAULT_WINDOW_MS, DEFAULT_MAX_HISTORY);
    }

    /**
     * Creates a funnel store with explicit history bounds.
     *
     * @param windowMs the maximum age of stored steps
     * @param maxHistory the maximum number of stored steps per sender
     */
    public FunnelStore(long windowMs, int maxHistory) {
        configure(windowMs, maxHistory);
    }

    /**
     * Reconfigures the history bounds used by this store.
     *
     * @param windowMs the maximum age of stored steps
     * @param maxHistory the maximum number of stored steps per sender
     */
    public synchronized void configure(long windowMs, int maxHistory) {
        this.windowMs = Math.max(1L, windowMs);
        this.maxHistory = Math.max(1, maxHistory);

        for (String senderKey : List.copyOf(stepsBySender.keySet())) {
            trimHistory(senderKey);
        }
    }

    /**
     * Returns the current sender-local funnel snapshot without mutating history.
     *
     * @param chatEvent the event to inspect
     * @return the current funnel snapshot
     */
    @Override
    public synchronized FunnelSnapshot snapshotFor(ChatEvent chatEvent) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        if (senderKey.isBlank()) {
            return FunnelSnapshot.empty();
        }

        long nowMs = StateStoreSupport.timestamp(chatEvent);
        prune(senderKey, nowMs);
        Deque<StepRecord> records = stepsBySender.get(senderKey);
        if (records == null || records.isEmpty()) {
            return new FunnelSnapshot(senderKey, List.of(), List.of());
        }

        List<FunnelStep> recentSteps = new ArrayList<>(records.size());
        List<String> evidences = new ArrayList<>(records.size());
        for (StepRecord record : records) {
            recentSteps.add(record.step());
            evidences.add(record.evidence());
        }

        return new FunnelSnapshot(senderKey, List.copyOf(recentSteps), List.copyOf(evidences));
    }

    /**
     * Records a generic message step for the sender.
     *
     * @param chatEvent the event to append
     */
    @Override
    public synchronized void record(ChatEvent chatEvent) {
        recordStep(chatEvent, FunnelStep.MESSAGE, StateStoreSupport.normalizedMessage(chatEvent));
    }

    /**
     * Records an explicit funnel step for the sender.
     *
     * @param chatEvent the source event
     * @param step the classified funnel step
     * @param evidence optional short evidence text
     */
    public synchronized void recordStep(ChatEvent chatEvent, FunnelStep step, String evidence) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        if (senderKey.isBlank()) {
            return;
        }

        long nowMs = StateStoreSupport.timestamp(chatEvent);
        prune(senderKey, nowMs);
        Deque<StepRecord> records = stepsBySender.computeIfAbsent(senderKey, ignored -> new ArrayDeque<>());
        records.addLast(new StepRecord(nowMs, step == null ? FunnelStep.MESSAGE : step, normalizeEvidence(evidence)));
        while (records.size() > maxHistory) {
            records.removeFirst();
        }
    }

    /**
     * Clears all stored funnel history.
     */
    @Override
    public synchronized void reset() {
        stepsBySender.clear();
    }

    /**
     * Returns the number of senders currently tracked in funnel history.
     *
     * @return the number of tracked senders
     */
    public synchronized int trackedSenderCount() {
        return stepsBySender.size();
    }

    /**
     * Returns the total number of buffered funnel steps across all senders.
     *
     * @return the total buffered funnel steps
     */
    public synchronized int trackedStepCount() {
        int total = 0;
        for (Deque<StepRecord> records : stepsBySender.values()) {
            if (records != null) {
                total += records.size();
            }
        }

        return total;
    }

    private void prune(String senderKey, long nowMs) {
        Deque<StepRecord> records = stepsBySender.get(senderKey);
        if (records == null) {
            return;
        }

        while (!records.isEmpty() && nowMs - records.peekFirst().timestampMs() > windowMs) {
            records.removeFirst();
        }

        if (records.isEmpty()) {
            stepsBySender.remove(senderKey);
            return;
        }

        while (records.size() > maxHistory) {
            records.removeFirst();
        }
    }

    private void trimHistory(String senderKey) {
        Deque<StepRecord> records = stepsBySender.get(senderKey);
        if (records == null) {
            return;
        }

        while (records.size() > maxHistory) {
            records.removeFirst();
        }

        if (records.isEmpty()) {
            stepsBySender.remove(senderKey);
        }
    }

    private static String normalizeEvidence(String evidence) {
        if (evidence == null || evidence.isBlank()) {
            return "";
        }

        String trimmedEvidence = evidence.trim();
        if (trimmedEvidence.length() <= 64) {
            return trimmedEvidence;
        }

        return trimmedEvidence.substring(0, 61) + "...";
    }

    private record StepRecord(long timestampMs, FunnelStep step, String evidence) {
    }

    /**
     * Simple funnel steps used by the future funnel stage.
     */
    public enum FunnelStep {
        /**
         * Generic message contact.
         */
        MESSAGE,
        /**
         * Trust or reputation framing.
         */
        TRUST,
        /**
         * Redirect to an external platform.
         */
        EXTERNAL_PLATFORM,
        /**
         * Payment-first or money transfer request.
         */
        PAYMENT,
        /**
         * Sensitive account or login request.
         */
        ACCOUNT_DATA
    }

    /**
     * Immutable view of one sender-local funnel snapshot.
     */
    public record FunnelSnapshot(
        String senderKey,
        List<FunnelStep> recentSteps,
        List<String> evidences
    ) {
        /**
         * Creates an empty funnel snapshot.
         *
         * @return the empty funnel snapshot
         */
        public static FunnelSnapshot empty() {
            return new FunnelSnapshot("", List.of(), List.of());
        }

        /**
         * Indicates whether this snapshot belongs to a known sender.
         *
         * @return {@code true} when sender information is present
         */
        public boolean hasSender() {
            return senderKey != null && !senderKey.isBlank();
        }
    }
}
