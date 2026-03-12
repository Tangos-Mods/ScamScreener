package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;

import java.util.Locale;
import java.util.UUID;

/**
 * Shared helper methods for stateful pipeline stores.
 */
final class StateStoreSupport {
    private StateStoreSupport() {
    }

    /**
     * Returns a safe event timestamp.
     *
     * @param chatEvent the event to inspect
     * @return a non-negative timestamp in milliseconds
     */
    static long timestamp(ChatEvent chatEvent) {
        return chatEvent == null ? System.currentTimeMillis() : Math.max(0L, chatEvent.getTimestampMs());
    }

    /**
     * Returns the raw message text or an empty string.
     *
     * @param chatEvent the event to inspect
     * @return the normalized raw message fallback
     */
    static String rawMessage(ChatEvent chatEvent) {
        return chatEvent == null || chatEvent.getRawMessage() == null ? "" : chatEvent.getRawMessage();
    }

    /**
     * Returns the normalized message text or an empty string.
     *
     * @param chatEvent the event to inspect
     * @return the normalized message fallback
     */
    static String normalizedMessage(ChatEvent chatEvent) {
        return chatEvent == null || chatEvent.getNormalizedMessage() == null ? "" : chatEvent.getNormalizedMessage();
    }

    /**
     * Returns the precomputed message fingerprint or an empty string.
     *
     * @param chatEvent the event to inspect
     * @return the fingerprint fallback
     */
    static String messageFingerprint(ChatEvent chatEvent) {
        return chatEvent == null || chatEvent.getMessageFingerprint() == null ? "" : chatEvent.getMessageFingerprint();
    }

    /**
     * Returns a stable sender key for non-system player messages.
     *
     * @param chatEvent the event to inspect
     * @return the sender key, or an empty string when none is available
     */
    static String senderKey(ChatEvent chatEvent) {
        return chatEvent == null || chatEvent.getSenderKey() == null ? "" : chatEvent.getSenderKey();
    }
}
