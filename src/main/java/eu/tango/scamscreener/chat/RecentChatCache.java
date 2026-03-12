package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small in-memory cache of recent inbound chat lines for case review.
 */
public final class RecentChatCache {
    private static final int DEFAULT_MAX_ENTRIES = 200;

    private final int maxEntries;
    private final Deque<CachedChatMessage> entries = new ArrayDeque<>();
    private final Map<String, Deque<CachedChatMessage>> playerEntriesBySender = new LinkedHashMap<>();
    private long version;

    public RecentChatCache() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public RecentChatCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    /**
     * Records one recent inbound chat line for later case selection.
     *
     * @param chatEvent the inbound chat event
     */
    public synchronized void record(ChatEvent chatEvent) {
        ChatEvent safeEvent = chatEvent == null ? ChatEvent.messageOnly("") : chatEvent;
        String cleanText = normalizeText(safeEvent);
        if (cleanText.isBlank()) {
            return;
        }

        ChatSourceType sourceType = safeEvent.getSourceType() == null ? ChatSourceType.UNKNOWN : safeEvent.getSourceType();
        CachedChatMessage cachedChatMessage = new CachedChatMessage(
            safeEvent.getTimestampMs() <= 0L ? System.currentTimeMillis() : safeEvent.getTimestampMs(),
            safeEvent.getSenderName(),
            displaySender(safeEvent.getSenderName(), sourceType),
            cleanText,
            sourceType
        );
        entries.addFirst(cachedChatMessage);
        indexBySender(cachedChatMessage);
        while (entries.size() > maxEntries) {
            removeFromSenderIndex(entries.removeLast());
        }
        version++;
    }

    /**
     * Returns the current cached lines, newest first.
     *
     * @return the cached chat lines
     */
    public synchronized List<CachedChatMessage> entries() {
        return List.copyOf(new ArrayList<>(entries));
    }

    /**
     * Returns the cached player messages for one sender, newest first.
     *
     * @param senderName the sender to look up
     * @param maxSenderEntries the maximum number of entries to return
     * @return the cached sender-local messages
     */
    public synchronized List<CachedChatMessage> entriesForSender(String senderName, int maxSenderEntries) {
        String senderKey = normalize(senderName);
        if (senderKey.isBlank()) {
            return List.of();
        }

        Deque<CachedChatMessage> senderEntries = playerEntriesBySender.get(senderKey);
        if (senderEntries == null || senderEntries.isEmpty()) {
            return List.of();
        }

        int boundedLimit = Math.max(1, maxSenderEntries);
        List<CachedChatMessage> result = new ArrayList<>(Math.min(boundedLimit, senderEntries.size()));
        int count = 0;
        for (CachedChatMessage entry : senderEntries) {
            if (count++ >= boundedLimit) {
                break;
            }
            result.add(entry);
        }

        return List.copyOf(result);
    }

    /**
     * Returns the current cache version.
     *
     * @return a monotonically increasing mutation counter
     */
    public synchronized long version() {
        return version;
    }

    /**
     * Clears the cached lines.
     */
    public synchronized void clear() {
        if (entries.isEmpty()) {
            return;
        }

        entries.clear();
        playerEntriesBySender.clear();
        version++;
    }

    private static String normalizeText(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getRawMessage() == null || chatEvent.getRawMessage().isBlank()) {
            return "";
        }

        String rawText = chatEvent.getRawMessage().replace('\n', ' ').replace('\r', ' ').trim();
        if (rawText.isBlank()) {
            return "";
        }
        if (chatEvent.isPlayerSource()) {
            return rawText;
        }

        return ChatLineClassifier.displayMessageOnly(rawText);
    }

    private static String displaySender(String senderName, ChatSourceType sourceType) {
        String normalizedSenderName = senderName == null ? "" : senderName.trim();
        if (!normalizedSenderName.isBlank()) {
            return normalizedSenderName;
        }

        return switch (sourceType == null ? ChatSourceType.UNKNOWN : sourceType) {
            case SYSTEM -> "System";
            case PLAYER -> "Unknown Player";
            case UNKNOWN -> "Unknown";
        };
    }

    private void indexBySender(CachedChatMessage entry) {
        if (entry == null || entry.sourceType() != ChatSourceType.PLAYER) {
            return;
        }

        String senderKey = normalize(entry.senderName());
        if (senderKey.isBlank()) {
            return;
        }

        playerEntriesBySender.computeIfAbsent(senderKey, ignored -> new ArrayDeque<>()).addFirst(entry);
    }

    private void removeFromSenderIndex(CachedChatMessage entry) {
        if (entry == null || entry.sourceType() != ChatSourceType.PLAYER) {
            return;
        }

        String senderKey = normalize(entry.senderName());
        if (senderKey.isBlank()) {
            return;
        }

        Deque<CachedChatMessage> senderEntries = playerEntriesBySender.get(senderKey);
        if (senderEntries == null || senderEntries.isEmpty()) {
            return;
        }
        if (entry.equals(senderEntries.peekLast())) {
            senderEntries.removeLast();
        } else {
            senderEntries.remove(entry);
        }
        if (senderEntries.isEmpty()) {
            playerEntriesBySender.remove(senderKey);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * One cached inbound chat line that can be added into a review case.
     *
     * @param capturedAtMs the local capture timestamp
     * @param senderName the sender name, when available
     * @param displaySender the sender label shown in the picker UI
     * @param cleanText the sanitized message text
     * @param sourceType the inferred source classification
     */
    public record CachedChatMessage(
        long capturedAtMs,
        String senderName,
        String displaySender,
        String cleanText,
        ChatSourceType sourceType
    ) {
        public CachedChatMessage {
            senderName = senderName == null ? "" : senderName.trim();
            displaySender = displaySender == null || displaySender.isBlank() ? "Unknown" : displaySender.trim();
            cleanText = cleanText == null ? "" : cleanText.trim();
            sourceType = sourceType == null ? ChatSourceType.UNKNOWN : sourceType;
        }

        public boolean matchesPlayerFilter(String playerFilter) {
            String normalizedFilter = normalize(playerFilter);
            if (normalizedFilter.isEmpty()) {
                return true;
            }

            return normalize(senderName).contains(normalizedFilter)
                || normalize(displaySender).contains(normalizedFilter);
        }

        public String sourceLabel() {
            return switch (sourceType) {
                case PLAYER -> "Player";
                case SYSTEM -> "System";
                case UNKNOWN -> "Unknown";
            };
        }

        public String speakerRoleId() {
            return switch (sourceType) {
                case PLAYER -> "other";
                case SYSTEM -> "system";
                case UNKNOWN -> "unknown";
            };
        }

        public String messageSourceTypeId() {
            return switch (sourceType) {
                case PLAYER -> "player";
                case SYSTEM -> "system";
                case UNKNOWN -> "unknown";
            };
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }

            return RecentChatCache.normalize(value);
        }
    }
}
