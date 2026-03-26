package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps a short global message window for cross-sender trend analysis.
 */
public final class TrendStore {
    private static final long DEFAULT_WINDOW_MS = 120_000L;
    private static final int DEFAULT_MAX_HISTORY = 200;

    private long windowMs;
    private int maxHistory;
    private final Deque<TrendRecord> recentMessages = new ArrayDeque<>();
    private final Map<String, Deque<TrendRecord>> recordsByFingerprint = new LinkedHashMap<>();

    /**
     * Creates a trend store with default history bounds.
     */
    public TrendStore() {
        this(DEFAULT_WINDOW_MS, DEFAULT_MAX_HISTORY);
    }

    /**
     * Creates a trend store with explicit history bounds.
     *
     * @param windowMs the maximum age of stored messages
     * @param maxHistory the maximum number of stored messages
     */
    public TrendStore(long windowMs, int maxHistory) {
        configure(windowMs, maxHistory);
    }

    /**
     * Reconfigures the history bounds used by this store.
     *
     * @param windowMs the maximum age of stored messages
     * @param maxHistory the maximum number of stored messages
     */
    public synchronized void configure(long windowMs, int maxHistory) {
        this.windowMs = Math.max(1L, windowMs);
        this.maxHistory = Math.max(1, maxHistory);
        trimHistory();
    }

    /**
     * Returns the current cross-sender snapshot for the given event without mutating history.
     *
     * @param chatEvent the event to inspect
     * @return the current trend snapshot
     */
    public synchronized TrendSnapshot snapshotFor(ChatEvent chatEvent) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        String normalizedMessage = StateStoreSupport.normalizedMessage(chatEvent);
        String fingerprint = StateStoreSupport.messageFingerprint(chatEvent);
        if (senderKey.isBlank() || normalizedMessage.isBlank() || fingerprint.isBlank()) {
            return TrendSnapshot.empty();
        }

        long nowMs = StateStoreSupport.timestamp(chatEvent);
        prune(nowMs);

        int matchingMessageCount = 0;
        Set<String> matchingSenderKeys = new LinkedHashSet<>();
        Deque<TrendRecord> matchingRecords = recordsByFingerprint.get(fingerprint);
        if (matchingRecords == null || matchingRecords.isEmpty()) {
            return TrendSnapshot.empty();
        }

        for (TrendRecord record : matchingRecords) {
            if (!fingerprint.equals(record.fingerprint())) {
                continue;
            }
            if (senderKey.equals(record.senderKey())) {
                continue;
            }

            matchingMessageCount++;
            matchingSenderKeys.add(record.senderKey());
        }

        return new TrendSnapshot(
            normalizedMessage,
            matchingMessageCount,
            matchingSenderKeys.size(),
            List.copyOf(matchingSenderKeys)
        );
    }

    /**
     * Records one event into the global trend history.
     *
     * @param chatEvent the event to append
     */
    public synchronized void record(ChatEvent chatEvent) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        String normalizedMessage = StateStoreSupport.normalizedMessage(chatEvent);
        String fingerprint = StateStoreSupport.messageFingerprint(chatEvent);
        if (senderKey.isBlank() || normalizedMessage.isBlank() || fingerprint.isBlank()) {
            return;
        }

        long nowMs = StateStoreSupport.timestamp(chatEvent);
        prune(nowMs);
        TrendRecord record = new TrendRecord(nowMs, senderKey, normalizedMessage, fingerprint);
        recentMessages.addLast(record);
        recordsByFingerprint.computeIfAbsent(fingerprint, ignored -> new ArrayDeque<>()).addLast(record);
        trimHistory();
    }

    /**
     * Clears all stored trend history.
     */
    public synchronized void reset() {
        recentMessages.clear();
        recordsByFingerprint.clear();
    }

    /**
     * Returns the number of buffered global trend messages.
     *
     * @return the total buffered trend messages
     */
    public synchronized int trackedMessageCount() {
        return recentMessages.size();
    }

    private void prune(long nowMs) {
        while (!recentMessages.isEmpty() && nowMs - recentMessages.peekFirst().timestampMs() > windowMs) {
            removeOldestRecord();
        }

        trimHistory();
    }

    private void trimHistory() {
        while (recentMessages.size() > maxHistory) {
            removeOldestRecord();
        }
    }

    private void removeOldestRecord() {
        TrendRecord oldestRecord = recentMessages.pollFirst();
        if (oldestRecord == null) {
            return;
        }

        Deque<TrendRecord> fingerprintRecords = recordsByFingerprint.get(oldestRecord.fingerprint());
        if (fingerprintRecords == null || fingerprintRecords.isEmpty()) {
            return;
        }
        if (oldestRecord.equals(fingerprintRecords.peekFirst())) {
            fingerprintRecords.removeFirst();
        } else {
            fingerprintRecords.remove(oldestRecord);
        }
        if (fingerprintRecords.isEmpty()) {
            recordsByFingerprint.remove(oldestRecord.fingerprint());
        }
    }

    private record TrendRecord(long timestampMs, String senderKey, String normalizedMessage, String fingerprint) {
    }

    /**
     * Immutable view of one cross-sender trend snapshot.
     */
    public record TrendSnapshot(
        String normalizedMessage,
        int matchingMessageCount,
        int distinctSenderCount,
        List<String> matchingSenderKeys
    ) {
        /**
         * Creates an empty trend snapshot.
         *
         * @return the empty trend snapshot
         */
        public static TrendSnapshot empty() {
            return new TrendSnapshot("", 0, 0, List.of());
        }

        /**
         * Indicates whether the snapshot contains any cross-sender matches.
         *
         * @return {@code true} when one or more matching senders were found
         */
        public boolean hasTrend() {
            return distinctSenderCount > 0;
        }
    }
}
