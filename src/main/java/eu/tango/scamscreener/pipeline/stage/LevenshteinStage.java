package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.rule.SimilarityRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Similarity stage for fuzzy phrase matching against known risky wording.
 *
 * <p>This is a reduced v2 port of the old v1 Levenshtein stage. The phrase
 * list now comes from an injected rules config so similarity tuning can happen
 * outside the stage implementation.
 */
public final class LevenshteinStage extends Stage {
    private static final ThreadLocal<LevenshteinScratch> LEVENSHTEIN_SCRATCH =
        ThreadLocal.withInitial(LevenshteinScratch::new);

    private final RuleCatalog rules;

    /**
     * Creates the similarity stage with the built-in default rules config.
     */
    public LevenshteinStage() {
        this(new RulesConfig());
    }

    /**
     * Creates the similarity stage with an explicit rules config.
     *
     * @param rulesConfig the config backing similarity-based rule checks
     */
    public LevenshteinStage(RulesConfig rulesConfig) {
        this(new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the similarity stage with an explicit compiled rule catalog.
     *
     * @param ruleCatalog the shared rule catalog
     */
    public LevenshteinStage(RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
    }

    /**
     * Evaluates similarity-based detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return a score-only result when one or more fuzzy phrase matches were found
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.similarityStageEnabled()) {
            return pass();
        }

        String normalizedMessage = chatEvent.getSimilarityMessage();
        if (normalizedMessage.length() < rules.minCompareLength()) {
            return pass();
        }

        List<String> messageTokens = tokenize(normalizedMessage);
        if (messageTokens.isEmpty() || rules.similarityRules().isEmpty()) {
            return pass();
        }

        Map<String, PhraseMatch> bestMatchesByCategory = new LinkedHashMap<>();
        for (SimilarityRule entry : rules.similarityRules()) {
            double similarity = bestSimilarity(normalizedMessage, messageTokens, entry);
            if (similarity < entry.threshold()) {
                continue;
            }

            PhraseMatch current = bestMatchesByCategory.get(entry.category());
            if (current == null || similarity > current.similarity()) {
                bestMatchesByCategory.put(entry.category(), new PhraseMatch(entry, similarity));
            }
        }

        if (bestMatchesByCategory.isEmpty()) {
            return pass();
        }
        if (containsOnlyUrgencyMatches(bestMatchesByCategory)) {
            // Fuzzy urgency alone is too noisy and should not open a review path by itself.
            return pass();
        }

        int totalScore = 0;
        List<String> reasonParts = new ArrayList<>();
        List<String> reasonIds = new ArrayList<>();
        for (PhraseMatch match : bestMatchesByCategory.values()) {
            totalScore += match.entry().score();
            reasonParts.add(match.entry().reason(match.similarity()));
            reasonIds.add(match.entry().reasonId());
        }

        return score(totalScore, reasonIds, String.join("; ", reasonParts));
    }

    private static boolean containsOnlyUrgencyMatches(Map<String, PhraseMatch> bestMatchesByCategory) {
        if (bestMatchesByCategory.isEmpty()) {
            return false;
        }

        for (PhraseMatch match : bestMatchesByCategory.values()) {
            if (!isUrgencyCategory(match.entry().category())) {
                return false;
            }
        }

        return true;
    }

    private static boolean isUrgencyCategory(String category) {
        return category != null && category.toLowerCase(Locale.ROOT).contains("urgency");
    }

    private static double bestSimilarity(String normalizedMessage, List<String> messageTokens, SimilarityRule entry) {
        String phrase = entry.normalizedPhrase();
        int phraseTokenCount = entry.tokenCount();
        LevenshteinScratch scratch = LEVENSHTEIN_SCRATCH.get();
        double bestSimilarity = similarityWithUpperBound(normalizedMessage, phrase, entry.threshold(), scratch);
        if (phraseTokenCount <= 0 || messageTokens.size() < phraseTokenCount) {
            return bestSimilarity;
        }

        StringBuilder windowBuilder = new StringBuilder(normalizedMessage.length());
        for (int startIndex = 0; startIndex <= messageTokens.size() - phraseTokenCount; startIndex++) {
            String window = joinWindow(messageTokens, startIndex, phraseTokenCount, windowBuilder);
            double minimumUsefulSimilarity = Math.max(entry.threshold(), Math.nextUp(bestSimilarity));
            double candidateSimilarity = similarityWithUpperBound(window, phrase, minimumUsefulSimilarity, scratch);
            if (candidateSimilarity > bestSimilarity) {
                bestSimilarity = candidateSimilarity;
            }
        }

        return bestSimilarity;
    }

    private static String joinWindow(List<String> tokens, int startIndex, int length, StringBuilder builder) {
        builder.setLength(0);
        for (int index = startIndex; index < startIndex + length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens.get(index));
        }

        return builder.toString();
    }

    private static List<String> tokenize(String normalizedMessage) {
        if (normalizedMessage.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        int tokenStart = -1;
        for (int index = 0; index < normalizedMessage.length(); index++) {
            char character = normalizedMessage.charAt(index);
            if (Character.isWhitespace(character)) {
                if (tokenStart >= 0) {
                    tokens.add(normalizedMessage.substring(tokenStart, index));
                    tokenStart = -1;
                }
                continue;
            }

            if (tokenStart < 0) {
                tokenStart = index;
            }
        }
        if (tokenStart >= 0) {
            tokens.add(normalizedMessage.substring(tokenStart));
        }

        return tokens;
    }

    private static double similarityWithUpperBound(
        String left,
        String right,
        double minimumUsefulSimilarity,
        LevenshteinScratch scratch
    ) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }

        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int minimumDistance = Math.abs(left.length() - right.length());
        double similarityUpperBound = 1.0 - (minimumDistance / (double) maxLength);
        if (similarityUpperBound < minimumUsefulSimilarity) {
            return 0.0;
        }

        int distance = levenshteinDistance(left, right, scratch);
        return 1.0 - (distance / (double) maxLength);
    }

    private static int levenshteinDistance(String left, String right, LevenshteinScratch scratch) {
        int leftLength = left.length();
        int rightLength = right.length();
        scratch.ensureCapacity(rightLength + 1);
        int[] previous = scratch.previous();
        int[] current = scratch.current();

        for (int index = 0; index <= rightLength; index++) {
            previous[index] = index;
        }

        for (int leftIndex = 1; leftIndex <= leftLength; leftIndex++) {
            current[0] = leftIndex;
            char leftCharacter = left.charAt(leftIndex - 1);
            for (int rightIndex = 1; rightIndex <= rightLength; rightIndex++) {
                char rightCharacter = right.charAt(rightIndex - 1);
                int substitutionCost = leftCharacter == rightCharacter ? 0 : 1;
                current[rightIndex] = Math.min(
                    Math.min(current[rightIndex - 1] + 1, previous[rightIndex] + 1),
                    previous[rightIndex - 1] + substitutionCost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[rightLength];
    }

    private record PhraseMatch(SimilarityRule entry, double similarity) {
    }

    private static final class LevenshteinScratch {
        private int[] previous = new int[0];
        private int[] current = new int[0];

        private void ensureCapacity(int requiredLength) {
            if (previous.length >= requiredLength && current.length >= requiredLength) {
                return;
            }

            previous = new int[requiredLength];
            current = new int[requiredLength];
        }

        private int[] previous() {
            return previous;
        }

        private int[] current() {
            return current;
        }
    }
}
