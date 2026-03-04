package eu.tango.scamscreener.pipeline.rule;

/**
 * One normalized similarity rule entry shared by fuzzy matching stages.
 *
 * @param category logical reason category
 * @param rawPhrase configured phrase text
 * @param normalizedPhrase normalized phrase for similarity checks
 * @param score non-negative score contribution
 * @param threshold bounded similarity threshold
 * @param tokenCount number of normalized phrase tokens
 */
public record SimilarityRule(
    String category,
    String rawPhrase,
    String normalizedPhrase,
    int score,
    double threshold,
    int tokenCount
) {
    /**
     * Returns the stable reason id for this similarity category.
     *
     * @return the stable reason id
     */
    public String reasonId() {
        String normalizedCategory = normalizeForId(category);
        if (normalizedCategory.isBlank()) {
            normalizedCategory = "match";
        }

        return "similarity." + normalizedCategory;
    }

    /**
     * Formats the centralized reason text for this similarity rule.
     *
     * @param similarity the measured similarity score
     * @return the formatted reason text
     */
    public String reason(double similarity) {
        long scaled = Math.round(similarity * 100.0);
        long whole = scaled / 100;
        long fraction = Math.abs(scaled % 100);
        String fractionText = fraction < 10 ? "0" + fraction : Long.toString(fraction);
        return safe(category) + " matched \"" + safe(rawPhrase) + "\" at " + whole + "." + fractionText;
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private static String normalizeForId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean lastWasSeparator = false;
        for (int index = 0; index < value.length(); index++) {
            char current = Character.toLowerCase(value.charAt(index));
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                lastWasSeparator = false;
                continue;
            }
            if (!lastWasSeparator) {
                builder.append('_');
                lastWasSeparator = true;
            }
        }

        String normalized = builder.toString();
        while (normalized.startsWith("_")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
