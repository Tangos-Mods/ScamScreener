package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps short sender-local message history for behavior analysis.
 */
public final class BehaviorStore {
    private static final long DEFAULT_WINDOW_MS = 90_000L;
    private static final int DEFAULT_MAX_HISTORY = 8;

    private long windowMs;
    private int maxHistory;
    private final Map<String, Deque<MessageRecord>> messagesBySender = new LinkedHashMap<>();

    /**
     * Creates a behavior store with default history bounds.
     */
    public BehaviorStore() {
        this(DEFAULT_WINDOW_MS, DEFAULT_MAX_HISTORY);
    }

    /**
     * Creates a behavior store with explicit history bounds.
     *
     * @param windowMs the maximum age of stored messages
     * @param maxHistory the maximum number of messages stored per sender
     */
    public BehaviorStore(long windowMs, int maxHistory) {
        configure(windowMs, maxHistory);
    }

    /**
     * Reconfigures the history bounds used by this store.
     *
     * @param windowMs the maximum age of stored messages
     * @param maxHistory the maximum number of messages stored per sender
     */
    public synchronized void configure(long windowMs, int maxHistory) {
        this.windowMs = Math.max(1L, windowMs);
        this.maxHistory = Math.max(1, maxHistory);

        for (String senderKey : List.copyOf(messagesBySender.keySet())) {
            trimHistory(senderKey);
        }
    }

    /**
     * Returns the current sender-local snapshot without mutating history.
     *
     * @param chatEvent the event to inspect
     * @return the current behavior snapshot for the sender
     */
    public synchronized BehaviorSnapshot snapshotFor(ChatEvent chatEvent) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        if (senderKey.isBlank()) {
            return BehaviorSnapshot.empty();
        }

        long timestampMs = StateStoreSupport.timestamp(chatEvent);
        prune(senderKey, timestampMs);
        Deque<MessageRecord> records = messagesBySender.get(senderKey);
        if (records == null || records.isEmpty()) {
            return new BehaviorSnapshot(senderKey, 0, 0, List.of());
        }

        String normalizedMessage = StateStoreSupport.normalizedMessage(chatEvent);
        int sameMessageCount = 0;
        List<String> recentMessages = new ArrayList<>(records.size());
        for (MessageRecord record : records) {
            recentMessages.add(record.rawMessage());
            if (!normalizedMessage.isBlank() && normalizedMessage.equals(record.normalizedMessage())) {
                sameMessageCount++;
            }
        }

        return new BehaviorSnapshot(senderKey, records.size(), sameMessageCount, List.copyOf(recentMessages));
    }

    /**
     * Records one event into the sender-local history.
     *
     * @param chatEvent the event to append
     */
    public synchronized void record(ChatEvent chatEvent) {
        String senderKey = StateStoreSupport.senderKey(chatEvent);
        if (senderKey.isBlank()) {
            return;
        }

        long timestampMs = StateStoreSupport.timestamp(chatEvent);
        prune(senderKey, timestampMs);

        Deque<MessageRecord> records = messagesBySender.computeIfAbsent(senderKey, ignored -> new ArrayDeque<>());
        records.addLast(new MessageRecord(
            timestampMs,
            StateStoreSupport.normalizedMessage(chatEvent),
            StateStoreSupport.rawMessage(chatEvent)
        ));
        while (records.size() > maxHistory) {
            records.removeFirst();
        }
    }

    /**
     * Clears all stored behavior history.
     */
    public synchronized void reset() {
        messagesBySender.clear();
    }

    /**
     * Returns the number of senders currently tracked in behavior history.
     *
     * @return the number of tracked senders
     */
    public synchronized int trackedSenderCount() {
        return messagesBySender.size();
    }

    /**
     * Returns the total number of buffered behavior messages across all senders.
     *
     * @return the total buffered behavior messages
     */
    public synchronized int trackedMessageCount() {
        int total = 0;
        for (Deque<MessageRecord> records : messagesBySender.values()) {
            if (records != null) {
                total += records.size();
            }
        }

        return total;
    }

    private void prune(String senderKey, long nowMs) {
        Deque<MessageRecord> records = messagesBySender.get(senderKey);
        if (records == null) {
            return;
        }

        while (!records.isEmpty() && nowMs - records.peekFirst().timestampMs() > windowMs) {
            records.removeFirst();
        }

        if (records.isEmpty()) {
            messagesBySender.remove(senderKey);
            return;
        }

        while (records.size() > maxHistory) {
            records.removeFirst();
        }
    }

    private void trimHistory(String senderKey) {
        Deque<MessageRecord> records = messagesBySender.get(senderKey);
        if (records == null) {
            return;
        }

        while (records.size() > maxHistory) {
            records.removeFirst();
        }

        if (records.isEmpty()) {
            messagesBySender.remove(senderKey);
        }
    }

    private record MessageRecord(long timestampMs, String normalizedMessage, String rawMessage) {
    }

    /**
     * Immutable view of one sender-local behavior snapshot.
     */
    public record BehaviorSnapshot(
        String senderKey,
        int recentMessageCount,
        int sameMessageCount,
        List<String> recentMessages
    ) {
        /**
         * Creates an empty behavior snapshot.
         *
         * @return the empty behavior snapshot
         */
        public static BehaviorSnapshot empty() {
            return new BehaviorSnapshot("", 0, 0, List.of());
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
