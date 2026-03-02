package eu.tango.scamscreener.pipeline.rule;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable compiled rule definition used by deterministic pipeline stages.
 *
 * <p>A rule can either be regex-backed or use the existing keyword plus phrase
 * scoring heuristic. This keeps the stage classes focused on orchestration
 * instead of rebuilding the same rule primitives repeatedly.
 */
@Accessors(fluent = true)
public final class Rule {
    private static final int MAX_REASON_MATCH_LENGTH = 48;

    @Getter
    private final String id;
    @Getter
    private final int score;
    @Getter
    private final int threshold;
    private final String reasonTemplate;
    private final Pattern pattern;
    private final List<String> keywords;
    private final List<String> phrases;

    private Rule(
        String id,
        int score,
        int threshold,
        String reasonTemplate,
        Pattern pattern,
        List<String> keywords,
        List<String> phrases
    ) {
        this.id = id == null ? "" : id.trim();
        this.score = Math.max(0, score);
        this.threshold = Math.max(0, threshold);
        this.reasonTemplate = reasonTemplate == null ? "" : reasonTemplate.trim();
        this.pattern = pattern;
        this.keywords = List.copyOf(keywords);
        this.phrases = List.copyOf(phrases);
    }

    /**
     * Creates one regex-backed rule.
     *
     * @param id the stable logical rule identifier
     * @param rawPattern the regex pattern applied to normalized messages
     * @param score the score contributed by this rule
     * @return one compiled regex rule
     */
    public static Rule pattern(String id, String rawPattern, int score) {
        return pattern(id, rawPattern, score, "");
    }

    /**
     * Creates one regex-backed rule with a centralized reason template.
     *
     * @param id the stable logical rule identifier
     * @param rawPattern the regex pattern applied to normalized messages
     * @param score the score contributed by this rule
     * @param reasonTemplate the reason template with one {@code %s} placeholder
     * @return one compiled regex rule
     */
    public static Rule pattern(String id, String rawPattern, int score, String reasonTemplate) {
        return new Rule(id, score, 0, reasonTemplate, compilePattern(rawPattern), List.of(), List.of());
    }

    /**
     * Creates one fixed-score rule used for compound stage logic.
     *
     * @param id the stable logical rule identifier
     * @param score the score contributed by this rule
     * @param reasonTemplate the centralized reason text
     * @return one fixed-score rule without an intrinsic matcher
     */
    public static Rule fixed(String id, int score, String reasonTemplate) {
        return new Rule(id, score, 0, reasonTemplate, null, List.of(), List.of());
    }

    /**
     * Creates one keyword and phrase scoring rule.
     *
     * @param id the stable logical rule identifier
     * @param keywords the single-word tokens that contribute one hit each
     * @param phrases the multi-token phrases that contribute two hits each
     * @param score the score contributed after the threshold is met
     * @param threshold the minimum combined hit count required to trigger
     * @return one phrase-scoring rule
     */
    public static Rule phrase(String id, List<String> keywords, List<String> phrases, int score, int threshold) {
        return phrase(id, keywords, phrases, score, threshold, "");
    }

    /**
     * Creates one keyword and phrase scoring rule with a centralized reason template.
     *
     * @param id the stable logical rule identifier
     * @param keywords the single-word tokens that contribute one hit each
     * @param phrases the multi-token phrases that contribute two hits each
     * @param score the score contributed after the threshold is met
     * @param threshold the minimum combined hit count required to trigger
     * @param reasonTemplate the reason template with one {@code %s} placeholder
     * @return one phrase-scoring rule
     */
    public static Rule phrase(
        String id,
        List<String> keywords,
        List<String> phrases,
        int score,
        int threshold,
        String reasonTemplate
    ) {
        return new Rule(id, score, threshold, reasonTemplate, null, normalizeTerms(keywords), normalizeTerms(phrases));
    }

    /**
     * Returns the first regex match, if this is a regex-backed rule.
     *
     * @param message the normalized message to inspect
     * @return the first regex match, or {@code null} when none matched
     */
    public String firstPatternMatch(String message) {
        if (pattern == null || message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group();
    }

    /**
     * Indicates whether the regex-backed rule matched the message.
     *
     * @param message the normalized message to inspect
     * @return {@code true} when the regex rule matched
     */
    public boolean patternMatches(String message) {
        return firstPatternMatch(message) != null;
    }

    /**
     * Applies the keyword plus phrase scoring heuristic.
     *
     * @param message the normalized message to inspect
     * @return the combined hit score and the first matching token or phrase
     */
    public PhraseScore phraseMatch(String message) {
        if (message == null || message.isBlank()) {
            return new PhraseScore(0, null);
        }

        List<String> tokens = tokenize(message);
        int keywordHits = countKeywordHits(tokens);
        String normalized = " " + String.join(" ", tokens) + " ";
        int phraseHits = countPhraseHits(normalized);
        String firstMatch = firstPhraseMatch(normalized);
        if (firstMatch == null) {
            firstMatch = firstKeywordMatch(tokens);
        }

        return new PhraseScore(keywordHits + (phraseHits * 2), firstMatch);
    }

    /**
     * Formats the centralized reason text for one matching rule.
     *
     * @param match the matching substring, keyword or phrase
     * @return the formatted reason text
     */
    public String reason(String match) {
        if (reasonTemplate.isBlank()) {
            return "";
        }

        return String.format(Locale.ROOT, reasonTemplate, summarizeMatch(match));
    }

    private static Pattern compilePattern(String rawPattern) {
        if (rawPattern == null || rawPattern.isBlank()) {
            return null;
        }

        return Pattern.compile(rawPattern);
    }

    private static List<String> normalizeTerms(List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String term : terms) {
            if (term == null) {
                continue;
            }

            String cleaned = term.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isBlank()) {
                normalized.add(cleaned);
            }
        }

        return normalized;
    }

    private static List<String> tokenize(String message) {
        String[] rawTokens = message.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String token : rawTokens) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private int countKeywordHits(List<String> tokens) {
        int hits = 0;
        for (String token : tokens) {
            if (keywords.contains(token)) {
                hits++;
            }
        }

        return hits;
    }

    private int countPhraseHits(String paddedMessage) {
        int hits = 0;
        for (String phrase : phrases) {
            if (paddedMessage.contains(" " + phrase + " ")) {
                hits++;
            }
        }

        return hits;
    }

    private String firstPhraseMatch(String paddedMessage) {
        for (String phrase : phrases) {
            if (paddedMessage.contains(" " + phrase + " ")) {
                return phrase;
            }
        }

        return null;
    }

    private String firstKeywordMatch(List<String> tokens) {
        for (String token : tokens) {
            if (keywords.contains(token)) {
                return token;
            }
        }

        return null;
    }

    private static String summarizeMatch(String match) {
        if (match == null || match.isBlank()) {
            return "unknown";
        }

        String cleanedMatch = match.trim();
        if (cleanedMatch.length() <= MAX_REASON_MATCH_LENGTH) {
            return cleanedMatch;
        }

        return cleanedMatch.substring(0, MAX_REASON_MATCH_LENGTH - 3) + "...";
    }

    /**
     * The result of a phrase-scoring rule evaluation.
     *
     * @param score the combined hit score
     * @param match the first matching phrase or keyword
     */
    public record PhraseScore(int score, String match) {
    }
}
