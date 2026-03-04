package eu.tango.scamscreener.review;

import eu.tango.scamscreener.chat.ChatLineClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scrubs player-identifying data from persisted review and training payloads.
 */
public final class ReviewPersistenceSanitizer {
    private static final String PLAYER_PLACEHOLDER = "<player>";
    private static final String UUID_PLACEHOLDER = "<uuid>";
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b"
    );
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s{2,}");

    private ReviewPersistenceSanitizer() {
    }

    /**
     * Sanitizes one persisted message body while preserving the reviewable text.
     *
     * @param rawMessage the raw captured message
     * @param senderName the sender name to scrub, when available
     * @return the sanitized message body
     */
    public static String sanitizePersistedMessage(String rawMessage, String senderName) {
        return sanitize(rawMessage, senderName, true);
    }

    /**
     * Sanitizes one persisted free-text value.
     *
     * @param value the free-text value to sanitize
     * @param senderName the sender name to scrub, when available
     * @return the sanitized value
     */
    public static String sanitizePersistedText(String value, String senderName) {
        return sanitize(value, senderName, false);
    }

    /**
     * Sanitizes one list of persisted free-text values.
     *
     * @param values the values to sanitize
     * @param senderName the sender name to scrub, when available
     * @return the sanitized values, excluding blanks
     */
    public static List<String> sanitizePersistedTextList(Iterable<String> values, String senderName) {
        if (values == null) {
            return List.of();
        }

        List<String> sanitizedValues = new ArrayList<>();
        for (String value : values) {
            String sanitizedValue = sanitizePersistedText(value, senderName);
            if (!sanitizedValue.isBlank()) {
                sanitizedValues.add(sanitizedValue);
            }
        }

        return List.copyOf(sanitizedValues);
    }

    private static String sanitize(String value, String senderName, boolean simplifyChatMessage) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String sanitizedValue = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (simplifyChatMessage) {
            sanitizedValue = ChatLineClassifier.displayMessageOnly(sanitizedValue);
        }

        sanitizedValue = scrubSenderName(sanitizedValue, senderName);
        sanitizedValue = UUID_PATTERN.matcher(sanitizedValue).replaceAll(UUID_PLACEHOLDER);
        sanitizedValue = WHITESPACE_PATTERN.matcher(sanitizedValue).replaceAll(" ").trim();
        return sanitizedValue;
    }

    private static String scrubSenderName(String value, String senderName) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalizedSenderName = senderName == null ? "" : senderName.trim();
        if (normalizedSenderName.isBlank()) {
            return value;
        }

        Pattern senderPattern = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])" + Pattern.quote(normalizedSenderName) + "(?![A-Za-z0-9_])"
        );
        return senderPattern.matcher(value).replaceAll(PLAYER_PLACEHOLDER);
    }
}
