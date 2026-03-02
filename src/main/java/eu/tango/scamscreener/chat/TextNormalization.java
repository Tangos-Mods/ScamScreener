package eu.tango.scamscreener.chat;

/**
 * Shared chat-text normalization helpers for fuzzy matching and spam fingerprinting.
 */
public final class TextNormalization {
    private TextNormalization() {
    }

    /**
     * Normalizes raw chat text for fuzzy similarity checks with light de-obfuscation.
     *
     * @param input the raw input text
     * @return the normalized text
     */
    public static String normalizeForSimilarity(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char normalized = normalizeCharacter(input.charAt(index));
            if (Character.isLetterOrDigit(normalized)) {
                if (shouldCollapseRepeated(builder, normalized)) {
                    continue;
                }
                builder.append(normalized);
            } else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                builder.append(' ');
            }
        }

        return builder.toString().trim();
    }

    /**
     * Normalizes raw chat text into a stable spam-wave fingerprint.
     *
     * @param input the raw input text
     * @return the normalized fingerprint
     */
    public static String fingerprint(String input) {
        return normalizeForSimilarity(input);
    }

    private static boolean shouldCollapseRepeated(StringBuilder builder, char normalized) {
        int length = builder.length();
        if (length < 2) {
            return false;
        }

        return builder.charAt(length - 1) == normalized && builder.charAt(length - 2) == normalized;
    }

    private static char normalizeCharacter(char rawCharacter) {
        char character = Character.toLowerCase(rawCharacter);
        return switch (character) {
            case '0' -> 'o';
            case '1' -> 'i';
            case '3' -> 'e';
            case '4' -> 'a';
            case '5' -> 's';
            case '7' -> 't';
            case '@' -> 'a';
            case '$' -> 's';
            default -> Character.isLetterOrDigit(character) ? character : ' ';
        };
    }
}
