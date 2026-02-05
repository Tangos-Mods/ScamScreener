package eu.tango.scamscreener.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TokenFeatureExtractor {
	private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9_]{3,24}");
	private static final String KW_PREFIX = "kw:";
	private static final String BI_PREFIX = "ng2:";
	private static final String TRI_PREFIX = "ng3:";

	private TokenFeatureExtractor() {
	}

	static Set<String> tokenize(String text) {
		return extractFeatureTokens(text);
	}

	static Set<String> tokenizeWords(String text) {
		Set<String> tokens = new LinkedHashSet<>();
		for (String token : wordSequence(text)) {
			tokens.add(token);
		}
		return tokens;
	}

	private static List<String> wordSequence(String text) {
		List<String> tokens = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return tokens;
		}

		Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
		while (matcher.find()) {
			tokens.add(matcher.group());
		}
		return tokens;
	}

	static Set<String> extractFeatureTokens(String text) {
		List<String> words = wordSequence(text);
		if (words.isEmpty()) {
			return Set.of();
		}

		Set<String> features = new LinkedHashSet<>();
		for (int i = 0; i < words.size(); i++) {
			String unigram = words.get(i);
			features.add(KW_PREFIX + unigram);

			if (i + 1 < words.size()) {
				features.add(BI_PREFIX + unigram + " " + words.get(i + 1));
			}
			if (i + 2 < words.size()) {
				features.add(TRI_PREFIX + unigram + " " + words.get(i + 1) + " " + words.get(i + 2));
			}
		}
		return features;
	}

	static List<String> buildVocab(Collection<LocalAiTrainer.Sample> samples, int maxSize, int minCount) {
		if (samples == null || samples.isEmpty()) {
			return List.of();
		}

		Map<String, TokenStat> stats = new HashMap<>();
		int positives = 0;
		for (LocalAiTrainer.Sample sample : samples) {
			if (sample.label() == 1) {
				positives++;
			}

			for (String token : extractFeatureTokens(sample.message())) {
				TokenStat stat = stats.computeIfAbsent(token, ignored -> new TokenStat());
				stat.count++;
				if (sample.label() == 1) {
					stat.positiveCount++;
				}
			}
		}

		int total = samples.size();
		double baseRate = positives / (double) total;
		List<TokenScore> ranked = new ArrayList<>();
		for (Map.Entry<String, TokenStat> entry : stats.entrySet()) {
			TokenStat stat = entry.getValue();
			if (stat.count < minCount) {
				continue;
			}

			double tokenRate = stat.positiveCount / (double) stat.count;
			double discriminative = Math.abs(tokenRate - baseRate);
			double score = discriminative * Math.log1p(stat.count);
			if (score > 0.0) {
				ranked.add(new TokenScore(entry.getKey(), score, stat.count));
			}
		}

		ranked.sort((a, b) -> {
			int byScore = Double.compare(b.score(), a.score());
			if (byScore != 0) {
				return byScore;
			}
			int byCount = Integer.compare(b.count(), a.count());
			if (byCount != 0) {
				return byCount;
			}
			return a.token().compareTo(b.token());
		});

		int limit = Math.min(maxSize, ranked.size());
		List<String> vocab = new ArrayList<>(limit);
		for (int i = 0; i < limit; i++) {
			vocab.add(ranked.get(i).token());
		}
		return vocab;
	}

	private static final class TokenStat {
		int count;
		int positiveCount;
	}

	private record TokenScore(String token, double score, int count) {
	}
}
