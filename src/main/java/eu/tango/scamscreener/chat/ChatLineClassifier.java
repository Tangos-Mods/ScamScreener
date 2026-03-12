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
        "[skyblocker]",
        "[skyhanni]",
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
        return analyze(rawLine).type();
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
        Analysis analysis = analyze(rawLine);
        if (analysis.cleanedLine().isBlank()) {
            return "";
        }

        String cleaned = analysis.cleanedLine();
        ParsedPlayerLine parsedPlayerLine = analysis.parsedPlayerLine();
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

        if (isSystemPrefix(prefixSection) || prefixSection.startsWith("[") || speakerWordCount(prefixSection) > 1) {
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
        return Optional.ofNullable(analyze(rawLine).parsedPlayerLine());
    }

    /**
     * Performs one single-pass analysis of a visible line for the hot GAME callback path.
     *
     * @param rawLine the visible raw line
     * @return the combined analysis result
     */
    public static Analysis analyze(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Analysis.unknown("");
        }

        String cleaned = stripFormatting(rawLine).trim();
        if (cleaned.isBlank()) {
            return Analysis.unknown("");
        }
        if (isSystemPrefix(cleaned)) {
            return Analysis.system(cleaned);
        }

        ParsedPlayerLine parsedPlayerLine = null;
        if (mayBePlayerMessage(cleaned)) {
            parsedPlayerLine = parseCleanedPlayerMessage(cleaned).orElse(null);
            if (parsedPlayerLine != null) {
                return Analysis.player(cleaned, parsedPlayerLine);
            }
        }

        if (looksLikeIgnoredFormat(cleaned)) {
            return Analysis.ignored(cleaned);
        }

        return looksLikeSystemMessage(cleaned)
            ? Analysis.system(cleaned)
            : Analysis.unknown(cleaned);
    }

    private static Optional<ParsedPlayerLine> parseCleanedPlayerMessage(String cleaned) {
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

    private static boolean mayBePlayerMessage(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return false;
        }
        if (rawLine.indexOf(':') <= 0) {
            return false;
        }

        return startsWithIgnoreCase(rawLine, "From:")
            || startsWithIgnoreCase(rawLine, "Guild >")
            || startsWithIgnoreCase(rawLine, "Party >")
            || hasPublicLevelPrefix(rawLine);
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

        String trimmedSection = senderSection.trim();
        int candidateStart = lastTokenStart(trimmedSection);
        if (candidateStart < 0 || candidateStart >= trimmedSection.length()) {
            return "";
        }

        String candidateName = trimmedSection.substring(candidateStart);
        if (!isValidPlayerName(candidateName)) {
            return "";
        }

        int index = 0;
        while (index < candidateStart) {
            while (index < candidateStart && Character.isWhitespace(trimmedSection.charAt(index))) {
                index++;
            }
            if (index >= candidateStart) {
                break;
            }

            int tokenEnd = index;
            while (tokenEnd < candidateStart && !Character.isWhitespace(trimmedSection.charAt(tokenEnd))) {
                tokenEnd++;
            }
            if (!isAllowedSenderMetadataToken(trimmedSection.substring(index, tokenEnd))) {
                return "";
            }
            index = tokenEnd;
        }

        return candidateName;
    }

    private static boolean isAllowedSenderMetadataToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String trimmed = token.trim();
        if (isBracketedMetadataToken(trimmed)) {
            return true;
        }

        for (int index = 0; index < trimmed.length(); index++) {
            if (isValidPlayerNameCharacter(trimmed.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBracketedMetadataToken(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '[' || token.charAt(token.length() - 1) != ']') {
            return false;
        }

        String inner = token.substring(1, token.length() - 1).trim();
        return !inner.isEmpty();
    }

    private static boolean looksLikeIgnoredFormat(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return false;
        }

        if (startsWithBracketedTag(rawLine) && !hasPublicLevelPrefix(rawLine) && rawLine.contains(":")) {
            return true;
        }

        if (startsWithIgnoreCase(rawLine, "From:")) {
            return parseSenderAndMessage(rawLine.substring("From:".length()).trim()).isEmpty();
        }
        if (startsWithIgnoreCase(rawLine, "Guild >")) {
            return parseSenderAndMessage(rawLine.substring("Guild >".length()).trim()).isEmpty();
        }
        if (startsWithIgnoreCase(rawLine, "Party >")) {
            return parseSenderAndMessage(rawLine.substring("Party >".length()).trim()).isEmpty();
        }
        if (hasPublicLevelPrefix(rawLine)) {
            return parseSenderAndMessage(rawLine).isEmpty();
        }

        int separatorIndex = rawLine.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= rawLine.length() - 1) {
            return false;
        }

        String speakerSection = rawLine.substring(0, separatorIndex).trim();
        return speakerSection.contains(">");
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

    private static boolean looksLikeSystemMessage(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return false;
        }

        if (startsWithBracketedTag(rawLine) && !hasPublicLevelPrefix(rawLine)) {
            return true;
        }

        int separatorIndex = rawLine.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= rawLine.length() - 1) {
            return false;
        }

        String speakerSection = rawLine.substring(0, separatorIndex).trim();
        if (speakerSection.isBlank()) {
            return false;
        }

        return speakerWordCount(speakerSection) > 1;
    }

    private static boolean startsWithBracketedTag(String rawLine) {
        if (rawLine == null || rawLine.isBlank() || rawLine.charAt(0) != '[') {
            return false;
        }

        int closingBracketIndex = rawLine.indexOf(']');
        return closingBracketIndex > 1;
    }

    private static int speakerWordCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        int count = 0;
        boolean inWord = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character)) {
                inWord = false;
                continue;
            }
            if (!inWord) {
                count++;
                inWord = true;
            }
        }

        return count;
    }

    private static int lastTokenStart(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }

        int index = value.length() - 1;
        while (index >= 0 && Character.isWhitespace(value.charAt(index))) {
            index--;
        }
        if (index < 0) {
            return -1;
        }
        while (index >= 0 && !Character.isWhitespace(value.charAt(index))) {
            index--;
        }

        return index + 1;
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
         * A malformed or unsupported line that should be ignored before the pipeline.
         */
        IGNORED,
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

    /**
     * Single-pass classifier output for one visible line.
     *
     * @param type the detected line type
     * @param cleanedLine the formatting-stripped visible line
     * @param parsedPlayerLine the parsed player payload when the line is player chat
     */
    public record Analysis(ChatLineType type, String cleanedLine, ParsedPlayerLine parsedPlayerLine) {
        private static Analysis player(String cleanedLine, ParsedPlayerLine parsedPlayerLine) {
            return new Analysis(ChatLineType.PLAYER, cleanedLine, parsedPlayerLine);
        }

        private static Analysis system(String cleanedLine) {
            return new Analysis(ChatLineType.SYSTEM, cleanedLine, null);
        }

        private static Analysis ignored(String cleanedLine) {
            return new Analysis(ChatLineType.IGNORED, cleanedLine, null);
        }

        private static Analysis unknown(String cleanedLine) {
            return new Analysis(ChatLineType.UNKNOWN, cleanedLine, null);
        }
    }
}
