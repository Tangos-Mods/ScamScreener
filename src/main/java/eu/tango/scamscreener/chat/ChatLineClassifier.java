package eu.tango.scamscreener.chat;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Classifies raw visible chat lines into player or system-originated messages.
 *
 * <p>This is a lightweight v2 baseline used to distinguish regular player chat
 * from well-known Hypixel system lines such as NPC, boss, or Bazaar output.
 */
public final class ChatLineClassifier {
    private static final int MIN_PLAYER_NAME_LENGTH = 3;
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
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

        return parsePlayerMessage(cleaned).isPresent()
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

    /**
     * Extracts sender and message from a visible player chat line.
     *
     * @param rawLine the visible chat line to parse
     * @return the parsed sender/message pair, when the line matches player chat
     */
    public static Optional<ParsedPlayerLine> parsePlayerMessage(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }

        String cleaned = rawLine.trim();
        if (isSystemPrefix(cleaned)) {
            return Optional.empty();
        }

        int separatorIndex = cleaned.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= cleaned.length() - 1) {
            return Optional.empty();
        }

        String senderSection = cleaned.substring(0, separatorIndex).trim();
        String message = cleaned.substring(separatorIndex + 1).trim();
        String senderName = extractSenderName(senderSection);
        if (senderName.isBlank() || message.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ParsedPlayerLine(senderName, message));
    }

    private static String extractSenderName(String senderSection) {
        if (senderSection == null || senderSection.isBlank()) {
            return "";
        }

        String remaining = senderSection.trim();
        while (remaining.startsWith("[")) {
            int closingBracketIndex = remaining.indexOf(']');
            if (closingBracketIndex < 0) {
                break;
            }

            remaining = remaining.substring(closingBracketIndex + 1).trim();
        }

        if (!isValidPlayerName(remaining)) {
            return "";
        }

        return remaining;
    }

    private static boolean isValidPlayerName(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim();
        if (trimmed.length() < MIN_PLAYER_NAME_LENGTH || trimmed.length() > MAX_PLAYER_NAME_LENGTH) {
            return false;
        }

        for (int index = 0; index < trimmed.length(); index++) {
            if (!isValidPlayerNameCharacter(trimmed.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidPlayerNameCharacter(char value) {
        return (value >= 'a' && value <= 'z')
            || (value >= 'A' && value <= 'Z')
            || (value >= '0' && value <= '9')
            || value == '_';
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

    /**
     * Parsed sender/message pair from a visible player chat line.
     *
     * @param senderName the extracted player name
     * @param message the extracted visible message body
     */
    public record ParsedPlayerLine(String senderName, String message) {
        public ParsedPlayerLine {
            senderName = senderName == null ? "" : senderName.trim();
            message = message == null ? "" : message.trim();
        }
    }
}
