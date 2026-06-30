package eu.tango.scamscreener.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks explicit one-shot inbound chat lines that external mods want ScamScreener to skip.
 */
public final class InboundMessageBypassRegistry {
    private static final long ENTRY_TTL_MS = 2_000L;
    private static final int MAX_PENDING_ENTRIES = 64;
    private static final List<PendingEntry> PENDING_ENTRIES = new ArrayList<>();

    private InboundMessageBypassRegistry() {
    }

    /**
     * Remembers one exact visible chat line for short-lived pre-pipeline bypass.
     *
     * @param rawMessage the exact visible chat line
     */
    public static synchronized void remember(String rawMessage) {
        String normalizedMessage = normalize(rawMessage);
        if (normalizedMessage.isBlank()) {
            purgeExpiredEntries(System.currentTimeMillis());
            return;
        }

        long now = System.currentTimeMillis();
        purgeExpiredEntries(now);
        PENDING_ENTRIES.add(new PendingEntry(normalizedMessage, now));
        trimPendingEntries();
    }

    /**
     * Consumes one pending bypass match, when present.
     *
     * @param rawMessage the currently received visible chat line
     * @return {@code true} when the line was explicitly marked for skipping
     */
    public static synchronized boolean consume(String rawMessage) {
        String normalizedMessage = normalize(rawMessage);
        if (normalizedMessage.isBlank()) {
            purgeExpiredEntries(System.currentTimeMillis());
            return false;
        }

        long now = System.currentTimeMillis();
        purgeExpiredEntries(now);
        for (int index = 0; index < PENDING_ENTRIES.size(); index++) {
            PendingEntry entry = PENDING_ENTRIES.get(index);
            if (entry.normalizedMessage().equals(normalizedMessage)) {
                PENDING_ENTRIES.remove(index);
                return true;
            }
        }

        return false;
    }

    static synchronized void clear() {
        PENDING_ENTRIES.clear();
    }

    private static String normalize(String rawMessage) {
        return rawMessage == null ? "" : rawMessage.trim();
    }

    private static void purgeExpiredEntries(long now) {
        PENDING_ENTRIES.removeIf(entry -> now - entry.createdAtMs() > ENTRY_TTL_MS);
    }

    private static void trimPendingEntries() {
        while (PENDING_ENTRIES.size() > MAX_PENDING_ENTRIES) {
            PENDING_ENTRIES.remove(0);
        }
    }

    private record PendingEntry(String normalizedMessage, long createdAtMs) {
    }
}
