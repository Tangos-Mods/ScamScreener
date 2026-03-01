package eu.tango.scamscreener.chat;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classifies raw visible chat lines into player or system-originated messages.
 *
 * <p>This is a lightweight v2 baseline used to distinguish regular player chat
 * from well-known Hypixel system lines such as NPC, boss, or Bazaar output.
 */
public final class ChatLineClassifier {
    private static final String CHAT_PREFIX_PATTERN = "(?:(?:\\[[^\\]]+\\]|[^\\r\\n])\\s*)*";
    private static final Pattern DIRECT_CHAT_PATTERN = Pattern.compile(
        "^" + CHAT_PREFIX_PATTERN + "(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$"
    );
    private static final Set<String> SYSTEM_PREFIXES = Set.of(
        "[npc]",
        "[security]",
        "[hypixel]",
        "[crowd]",
        "[boss]",
        "[bazaar]",
        "[auction]",
        "[scamscreener]"
    );

    private ChatLineClassifier() {
    }

    /**
     * Classifies one raw chat line.
     *
     * @param rawLine the visible chat line to classify
     * @return the detected chat line type
     */
    public static ChatLineType classify(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return ChatLineType.UNKNOWN;
        }

        String cleaned = rawLine.trim();
        if (isSystemPrefix(cleaned)) {
            return ChatLineType.SYSTEM;
        }

        return DIRECT_CHAT_PATTERN.matcher(cleaned).matches()
            ? ChatLineType.PLAYER
            : ChatLineType.UNKNOWN;
    }

    /**
     * Indicates whether the given line is a regular player message.
     *
     * @param rawLine the visible chat line to classify
     * @return {@code true} when the line matches player chat
     */
    public static boolean isPlayerMessage(String rawLine) {
        return classify(rawLine) == ChatLineType.PLAYER;
    }

    private static boolean isSystemPrefix(String rawLine) {
        String normalized = rawLine.toLowerCase(Locale.ROOT);
        for (String prefix : SYSTEM_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * High-level source type for a visible chat line.
     */
    public enum ChatLineType {
        /**
         * A player-authored chat line.
         */
        PLAYER,
        /**
         * A server, NPC, or system-authored line.
         */
        SYSTEM,
        /**
         * A line that could not be classified yet.
         */
        UNKNOWN
    }
}
