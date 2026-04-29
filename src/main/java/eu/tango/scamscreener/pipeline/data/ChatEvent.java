package eu.tango.scamscreener.pipeline.data;

import com.mojang.authlib.GameProfile;
import eu.tango.scamscreener.chat.TextNormalization;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Immutable pipeline input for a single inbound chat message.
 *
 * <p>This bundles the raw message with the most important metadata so stages
 * can evolve beyond simple string-only checks.
 */
@Getter
public final class ChatEvent {
    private final String rawMessage;
    private final String normalizedMessage;
    private final String similarityMessage;
    private final String messageFingerprint;
    private final UUID senderUuid;
    private final String senderName;
    private final String senderKey;
    private final long timestampMs;
    private final ChatSourceType sourceType;
    @Getter(AccessLevel.NONE)
    private final Map<String, Optional<String>> cachedPatternMatches = new HashMap<>();

    /**
     * Creates a chat event with normalized text and sender metadata.
     *
     * @param rawMessage the original message text
     * @param senderUuid the sender UUID, if available
     * @param senderName the sender name, if available
     * @param timestampMs the receive timestamp in epoch milliseconds
     */
    public ChatEvent(String rawMessage, UUID senderUuid, String senderName, long timestampMs) {
        this(rawMessage, senderUuid, senderName, timestampMs, ChatSourceType.UNKNOWN);
    }

    /**
     * Creates a chat event with normalized text, sender metadata and source type.
     *
     * @param rawMessage the original message text
     * @param senderUuid the sender UUID, if available
     * @param senderName the sender name, if available
     * @param timestampMs the receive timestamp in epoch milliseconds
     * @param sourceType the detected source type of the message
     */
    public ChatEvent(String rawMessage, UUID senderUuid, String senderName, long timestampMs, ChatSourceType sourceType) {
        // Normalize null input early so downstream stages can stay simple.
        this.rawMessage = rawMessage == null ? "" : rawMessage;
        this.normalizedMessage = normalizeMessage(this.rawMessage);
        this.similarityMessage = TextNormalization.normalizeForSimilarity(this.rawMessage);
        this.messageFingerprint = similarityMessage;
        this.senderUuid = senderUuid;
        this.senderName = senderName == null ? "" : senderName.trim();
        this.timestampMs = timestampMs;
        this.sourceType = sourceType == null ? ChatSourceType.UNKNOWN : sourceType;
        this.senderKey = buildSenderKey(this.senderUuid, this.senderName, this.sourceType);
    }

    /**
     * Creates a minimal chat event when only the raw message is available.
     *
     * @param rawMessage the original message text
     * @return a minimal event with normalized text and a current timestamp
     */
    public static ChatEvent messageOnly(String rawMessage) {
        // Keep the convenience path for tests and low-context callers.
        return new ChatEvent(rawMessage, null, "", System.currentTimeMillis(), ChatSourceType.UNKNOWN);
    }

    /**
     * Creates a minimal chat event when only the raw message and source are available.
     *
     * @param rawMessage the original message text
     * @param sourceType the detected source type of the message
     * @return a minimal event with normalized text and a current timestamp
     */
    public static ChatEvent messageOnly(String rawMessage, ChatSourceType sourceType) {
        return new ChatEvent(rawMessage, null, "", System.currentTimeMillis(), sourceType);
    }

    /**
     * Creates a chat event directly from the inbound Fabric chat callback values.
     *
     * @param message the inbound chat message text component
     * @param sender the sender profile, if available
     * @param receptionTimestamp the receive timestamp
     * @param maxChatLength the maximum message length to extract
     * @return a normalized chat event for pipeline processing
     */
    public static ChatEvent fromInboundChat(
        Component message,
        GameProfile sender,
        Instant receptionTimestamp,
        int maxChatLength
    ) {
        return fromInboundChat(message, sender, null, receptionTimestamp, maxChatLength, ChatSourceType.UNKNOWN);
    }

    /**
     * Creates a chat event directly from the inbound Fabric chat callback values.
     *
     * @param message the inbound chat message text component
     * @param sender the sender profile, if available
     * @param receptionTimestamp the receive timestamp
     * @param maxChatLength the maximum message length to extract
     * @param sourceType the detected source type of the message
     * @return a normalized chat event for pipeline processing
     */
    public static ChatEvent fromInboundChat(
        Component message,
        GameProfile sender,
        Instant receptionTimestamp,
        int maxChatLength,
        ChatSourceType sourceType
    ) {
        return fromInboundChat(message, sender, null, receptionTimestamp, maxChatLength, sourceType);
    }

    /**
     * Creates a chat event directly from the inbound Fabric chat callback values.
     *
     * @param message the inbound chat message text component
     * @param sender the sender profile, if available
     * @param params the Fabric message parameter object, if available
     * @param receptionTimestamp the receive timestamp
     * @param maxChatLength the maximum message length to extract
     * @param sourceType the detected source type of the message
     * @return a normalized chat event for pipeline processing
     */
    public static ChatEvent fromInboundChat(
        Component message,
        GameProfile sender,
        Object params,
        Instant receptionTimestamp,
        int maxChatLength,
        ChatSourceType sourceType
    ) {
        return fromInboundChat(message, sender, params, receptionTimestamp, maxChatLength, sourceType, false);
    }

    /**
     * Creates a chat event directly from the inbound Fabric chat callback values.
     *
     * @param message the inbound chat message text component
     * @param sender the sender profile, if available
     * @param params the Fabric message parameter object, if available
     * @param receptionTimestamp the receive timestamp
     * @param maxChatLength the maximum message length to extract
     * @param sourceType the detected source type of the message
     * @param classifyPlayerFromSender when {@code true}, sender metadata promotes the event to player chat
     * @return a normalized chat event for pipeline processing
     */
    public static ChatEvent fromInboundChat(
        Component message,
        GameProfile sender,
        Object params,
        Instant receptionTimestamp,
        int maxChatLength,
        ChatSourceType sourceType,
        boolean classifyPlayerFromSender
    ) {
        String rawMessage = message == null ? "" : message.getString(maxChatLength);
        UUID senderUuid = sender == null ? null : sender.id();
        String senderName = sender == null || sender.name() == null ? "" : sender.name();
        if (senderName.isBlank()) {
            senderName = extractSenderNameFromParams(params, maxChatLength);
        }
        long timestampMs = receptionTimestamp == null ? System.currentTimeMillis() : receptionTimestamp.toEpochMilli();
        ChatSourceType resolvedSourceType = sourceType == null ? ChatSourceType.UNKNOWN : sourceType;
        if (classifyPlayerFromSender && (senderUuid != null || !senderName.isBlank())) {
            resolvedSourceType = ChatSourceType.PLAYER;
        }

        // Centralize callback-to-event conversion so the listener stays lean.
        return new ChatEvent(rawMessage, senderUuid, senderName, timestampMs, resolvedSourceType);
    }

    /**
     * Creates a system-oriented chat event from the Fabric game-message callback.
     *
     * @param message the inbound server or system message text component
     * @param maxChatLength the maximum message length to extract
     * @return a normalized system chat event for pipeline processing
     */
    public static ChatEvent fromGameMessage(Component message, int maxChatLength) {
        String rawMessage = message == null ? "" : message.getString(maxChatLength);
        return new ChatEvent(rawMessage, null, "", System.currentTimeMillis(), ChatSourceType.SYSTEM);
    }

    /**
     * Normalizes a chat message for case-insensitive matching.
     *
     * @param message the raw message text
     * @return the normalized message text
     */
    public static String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }

        // Lowercasing and trimming are enough for the current v2 baseline.
        return message.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Indicates whether sender metadata is available on this event.
     *
     * @return {@code true} when either sender field is populated
     */
    public boolean hasSender() {
        // Either UUID or a non-blank name is enough for sender-aware stages.
        return senderUuid != null || !senderName.isBlank();
    }

    /**
     * Indicates whether this event is classified as player-authored.
     *
     * @return {@code true} when the event source is a player
     */
    public boolean isPlayerSource() {
        return sourceType == ChatSourceType.PLAYER;
    }

    /**
     * Indicates whether this event is classified as system-authored.
     *
     * @return {@code true} when the event source is a system source
     */
    public boolean isSystemSource() {
        return sourceType == ChatSourceType.SYSTEM;
    }

    /**
     * Returns one cached regex match for this event.
     *
     * @param cacheKey the stable cache key for the compiled rule
     * @param resolver the regex work to run on a cache miss
     * @return the first matching substring, or {@code null} when none matched
     */
    public synchronized String cachedPatternMatch(String cacheKey, Supplier<String> resolver) {
        if (resolver == null) {
            return null;
        }
        if (cacheKey == null || cacheKey.isBlank()) {
            return resolver.get();
        }

        Optional<String> cachedMatch = cachedPatternMatches.get(cacheKey);
        if (cachedMatch != null) {
            return cachedMatch.orElse(null);
        }

        String resolvedMatch = resolver.get();
        cachedPatternMatches.put(cacheKey, Optional.ofNullable(resolvedMatch));
        return resolvedMatch;
    }

    private static String buildSenderKey(UUID senderUuid, String senderName, ChatSourceType sourceType) {
        if (sourceType == ChatSourceType.SYSTEM) {
            return "";
        }
        if (senderUuid != null) {
            return senderUuid.toString();
        }
        if (senderName == null || senderName.isBlank()) {
            return "";
        }

        return senderName.trim().toLowerCase(Locale.ROOT);
    }

    private static String extractSenderNameFromParams(Object params, int maxChatLength) {
        if (params == null) {
            return "";
        }

        for (String methodName : new String[] { "name", "getName", "senderName", "targetName" }) {
            String extractedName = invokeNameMethod(params, methodName, maxChatLength);
            if (!extractedName.isBlank()) {
                return extractedName;
            }
        }

        return "";
    }

    private static String invokeNameMethod(Object params, String methodName, int maxChatLength) {
        try {
            Object value = params.getClass().getMethod(methodName).invoke(params);
            if (value instanceof Component textValue) {
                return textValue.getString(maxChatLength).trim();
            }
            if (value instanceof String stringValue) {
                return stringValue.trim();
            }
        } catch (ReflectiveOperationException ignored) {
            return "";
        }

        return "";
    }
}
