package eu.tango.scamscreener.pipeline.rule;

import java.util.Locale;

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
     * Formats the centralized reason text for this similarity rule.
     *
     * @param similarity the measured similarity score
     * @return the formatted reason text
     */
    public String reason(double similarity) {
        return String.format(Locale.ROOT, "%s matched \"%s\" at %.2f", category, rawPhrase, similarity);
    }
}
