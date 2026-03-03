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
        "[scamscreener]",
        "Profile"
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

        String cleaned = stripFormatting(rawLine).trim();
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
     * Returns the visible message body without player/system prefixes when possible.
     *
     * @param rawLine the visible chat line to simplify
     * @return the simplified message body for UI display
     */
    public static String displayMessageOnly(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return "";
        }

        String cleaned = stripFormatting(rawLine).trim();
        ParsedPlayerLine parsedPlayerLine = parsePlayerMessage(cleaned).orElse(null);
        if (parsedPlayerLine != null) {
            return parsedPlayerLine.message();
        }

        int separatorIndex = cleaned.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= cleaned.length() - 1) {
            return cleaned;
        }

        String prefixSection = cleaned.substring(0, separatorIndex).trim();
        String message = cleaned.substring(separatorIndex + 1).trim();
        if (message.isBlank()) {
            return cleaned;
        }

        if (isSystemPrefix(prefixSection) || prefixSection.startsWith("[")) {
            return message;
        }

        return cleaned;
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

        String cleaned = stripFormatting(rawLine).trim();
        if (isSystemPrefix(cleaned)) {
            return Optional.empty();
        }

        Optional<ParsedPlayerLine> directMessage = parsePrefixedPlayerMessage(cleaned, "From:");
        if (directMessage.isPresent()) {
            return directMessage;
        }

        Optional<ParsedPlayerLine> guildMessage = parsePrefixedPlayerMessage(cleaned, "Guild >");
        if (guildMessage.isPresent()) {
            return guildMessage;
        }

        Optional<ParsedPlayerLine> partyMessage = parsePrefixedPlayerMessage(cleaned, "Party >");
        if (partyMessage.isPresent()) {
            return partyMessage;
        }

        if (!hasPublicLevelPrefix(cleaned)) {
            return Optional.empty();
        }

        return parseSenderAndMessage(cleaned);
    }

    private static Optional<ParsedPlayerLine> parsePrefixedPlayerMessage(String rawLine, String prefix) {
        if (!startsWithIgnoreCase(rawLine, prefix)) {
            return Optional.empty();
        }

        String remainder = rawLine.substring(prefix.length()).trim();
        if (remainder.isBlank()) {
            return Optional.empty();
        }

        return parseSenderAndMessage(remainder);
    }

    private static Optional<ParsedPlayerLine> parseSenderAndMessage(String rawLine) {
        int separatorIndex = rawLine.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= rawLine.length() - 1) {
            return Optional.empty();
        }

        String senderSection = rawLine.substring(0, separatorIndex).trim();
        String message = rawLine.substring(separatorIndex + 1).trim();
        String senderName = extractSenderName(senderSection);
        if (senderName.isBlank() || message.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ParsedPlayerLine(senderName, message));
    }

    private static boolean hasPublicLevelPrefix(String rawLine) {
        if (rawLine == null || rawLine.isBlank() || rawLine.charAt(0) != '[') {
            return false;
        }

        int closingBracketIndex = rawLine.indexOf(']');
        if (closingBracketIndex <= 1) {
            return false;
        }

        for (int index = 1; index < closingBracketIndex; index++) {
            if (!Character.isDigit(rawLine.charAt(index))) {
                return false;
            }
        }

        return true;
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
            remaining = trailingPlayerToken(remaining);
            if (remaining.isBlank()) {
                return "";
            }
        }

        return remaining;
    }

    private static String trailingPlayerToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String[] tokens = value.trim().split("\\s+");
        for (int index = tokens.length - 1; index >= 0; index--) {
            String token = trimNonNameEdges(tokens[index]);
            if (isValidPlayerName(token)) {
                return token;
            }
        }

        return "";
    }

    private static String trimNonNameEdges(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        int start = 0;
        int end = value.length();
        while (start < end && !isValidPlayerNameCharacter(value.charAt(start))) {
            start++;
        }
        while (end > start && !isValidPlayerNameCharacter(value.charAt(end - 1))) {
            end--;
        }

        return start >= end ? "" : value.substring(start, end);
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

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        if (value == null || prefix == null || value.length() < prefix.length()) {
            return false;
        }

        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String stripFormatting(String rawLine) {
        if (rawLine == null || rawLine.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(rawLine.length());
        for (int index = 0; index < rawLine.length(); index++) {
            char current = rawLine.charAt(index);
            if (current == '\u00A7' && index + 1 < rawLine.length()) {
                index++;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
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
