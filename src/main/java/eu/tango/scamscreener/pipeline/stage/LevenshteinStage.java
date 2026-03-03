package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.chat.TextNormalization;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.rule.SimilarityRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Similarity stage for fuzzy phrase matching against known risky wording.
 *
 * <p>This is a reduced v2 port of the old v1 Levenshtein stage. The phrase
 * list now comes from an injected rules config so similarity tuning can happen
 * outside the stage implementation.
 */
public final class LevenshteinStage extends Stage {
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

        String normalizedMessage = TextNormalization.normalizeForSimilarity(chatEvent.getRawMessage());
        if (normalizedMessage.length() < rules.minCompareLength()) {
            return pass();
        }

        List<String> messageTokens = tokenize(normalizedMessage);
        if (messageTokens.isEmpty() || rules.similarityRules().isEmpty()) {
            return pass();
        }

        Map<String, PhraseMatch> bestMatchesByCategory = new LinkedHashMap<>();
        for (SimilarityRule entry : rules.similarityRules()) {
            double similarity = bestSimilarity(normalizedMessage, messageTokens, entry.normalizedPhrase(), entry.tokenCount());
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

        int totalScore = 0;
        List<String> reasonParts = new ArrayList<>();
        for (PhraseMatch match : bestMatchesByCategory.values()) {
            totalScore += match.entry().score();
            reasonParts.add(match.entry().reason(match.similarity()));
        }

        return score(totalScore, String.join("; ", reasonParts));
    }

    private static double bestSimilarity(String normalizedMessage, List<String> messageTokens, String phrase, int phraseTokenCount) {
        double bestSimilarity = similarity(normalizedMessage, phrase);
        if (phraseTokenCount <= 0 || messageTokens.size() < phraseTokenCount) {
            return bestSimilarity;
        }

        for (int startIndex = 0; startIndex <= messageTokens.size() - phraseTokenCount; startIndex++) {
            String window = joinWindow(messageTokens, startIndex, phraseTokenCount);
            bestSimilarity = Math.max(bestSimilarity, similarity(window, phrase));
        }

        return bestSimilarity;
    }

    private static String joinWindow(List<String> tokens, int startIndex, int length) {
        StringBuilder builder = new StringBuilder();
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

        String[] rawTokens = normalizedMessage.split("\\s+");
        List<String> tokens = new ArrayList<>(rawTokens.length);
        for (String token : rawTokens) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private static double similarity(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0.0;
        }

        int distance = levenshteinDistance(left, right);
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - (distance / (double) maxLength);
    }

    private static int levenshteinDistance(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        int[] previous = new int[rightLength + 1];
        int[] current = new int[rightLength + 1];

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
}
