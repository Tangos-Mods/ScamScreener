package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.ScamScreenerPaths;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.CsvLineParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds rule signals based on Levenshtein similarity to known phrases and training samples.
 */
public final class LevenshteinSignalStage {
	private static final Path TRAINING_DATA_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-training-data.csv");
	private static final List<PhraseEntry> RULE_PHRASES = buildRulePhrases();

	private final TrainingCache trainingCache = new TrainingCache();

	public List<Signal> collectSignals(MessageEvent event) {
		if (event == null || event.normalizedMessage().isBlank()) {
			return List.of();
		}

		String normalized = normalizeForSimilarity(event.normalizedMessage());
		if (normalized.length() < ScamRules.similarityMinMessageLength()) {
			return List.of();
		}

		List<Signal> signals = new ArrayList<>();
		addRulePhraseSignals(normalized, signals);
		addTrainingSimilaritySignal(normalized, signals);
		return signals;
	}

	private void addRulePhraseSignals(String message, List<Signal> signals) {
		Map<ScamRules.ScamRule, PhraseMatch> bestMatches = new LinkedHashMap<>();
		double threshold = ScamRules.similarityRuleThreshold();
		int weight = ScamRules.similarityRuleWeight();
		for (PhraseEntry entry : RULE_PHRASES) {
			if (!ScamRules.isRuleEnabled(entry.rule())) {
				continue;
			}
			double score = similarity(message, entry.normalized());
			if (score < threshold) {
				continue;
			}
			PhraseMatch current = bestMatches.get(entry.rule());
			if (current == null || score > current.similarity()) {
				bestMatches.put(entry.rule(), new PhraseMatch(entry.phrase(), score));
			}
		}

		for (Map.Entry<ScamRules.ScamRule, PhraseMatch> entry : bestMatches.entrySet()) {
			PhraseMatch match = entry.getValue();
			signals.add(new Signal(
				entry.getKey().name(),
				SignalSource.RULE,
				weight,
				"Levenshtein similarity=" + formatPercent(match.similarity())
					+ " to phrase: \"" + match.phrase() + "\" (+" + weight + ")",
				entry.getKey(),
				List.of()
			));
		}
	}

	private void addTrainingSimilaritySignal(String message, List<Signal> signals) {
		TrainingSnapshot snapshot = trainingCache.loadIfNeeded();
		if (snapshot == null || snapshot.samples().isEmpty() || !ScamRules.isRuleEnabled(ScamRules.ScamRule.SIMILARITY_MATCH)) {
			return;
		}

		double threshold = ScamRules.similarityTrainingThreshold();
		double margin = ScamRules.similarityTrainingMargin();
		int weight = ScamRules.similarityTrainingWeight();
		TrainingMatch bestScam = null;
		TrainingMatch bestLegit = null;

		for (TrainingSample sample : snapshot.samples()) {
			double score = similarity(message, sample.normalized());
			if (sample.label() == 1) {
				if (bestScam == null || score > bestScam.similarity()) {
					bestScam = new TrainingMatch(sample.raw(), score);
				}
			} else {
				if (bestLegit == null || score > bestLegit.similarity()) {
					bestLegit = new TrainingMatch(sample.raw(), score);
				}
			}
		}

		if (bestScam == null || bestScam.similarity() < threshold) {
			return;
		}
		double legitScore = bestLegit == null ? 0.0 : bestLegit.similarity();
		if (bestScam.similarity() < legitScore + margin) {
			return;
		}

		signals.add(new Signal(
			ScamRules.ScamRule.SIMILARITY_MATCH.name(),
			SignalSource.RULE,
			weight,
			"Levenshtein similarity=" + formatPercent(bestScam.similarity())
				+ " to scam training sample: \"" + bestScam.sample() + "\" (+" + weight + ")",
			ScamRules.ScamRule.SIMILARITY_MATCH,
			List.of()
		));
	}

	private static List<PhraseEntry> buildRulePhrases() {
		List<PhraseEntry> phrases = new ArrayList<>();
		addAll(phrases, ScamRules.ScamRule.PRESSURE_AND_URGENCY, List.of(
			"right now",
			"right away",
			"as soon as possible",
			"need it now",
			"need this now",
			"need this right now",
			"fast fast",
			"quick payment"
		));
		addAll(phrases, ScamRules.ScamRule.TRUST_MANIPULATION, List.of(
			"trust me",
			"i am legit",
			"its legit",
			"it's legit",
			"safe trade",
			"trusted middleman",
			"legit middleman"
		));
		addAll(phrases, ScamRules.ScamRule.UPFRONT_PAYMENT, List.of(
			"pay first",
			"first payment",
			"send first",
			"pay upfront",
			"upfront payment",
			"vorkasse"
		));
		addAll(phrases, ScamRules.ScamRule.ACCOUNT_DATA_REQUEST, List.of(
			"password",
			"passwort",
			"2fa code",
			"verification code",
			"email login",
			"login details"
		));
		addAll(phrases, ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH, List.of(
			"add me on discord",
			"discord server",
			"dm me",
			"telegram",
			"t.me",
			"add me on telegram"
		));
		addAll(phrases, ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM, List.of(
			"trusted middleman",
			"legit middleman",
			"middleman"
		));
		addAll(phrases, ScamRules.ScamRule.TOO_GOOD_TO_BE_TRUE, List.of(
			"free coins",
			"free rank",
			"100% safe",
			"guaranteed",
			"garantiert",
			"dupe"
		));
		return phrases;
	}

	private static void addAll(List<PhraseEntry> phrases, ScamRules.ScamRule rule, List<String> raw) {
		for (String phrase : raw) {
			if (phrase == null || phrase.isBlank()) {
				continue;
			}
			String normalized = normalizeForSimilarity(phrase);
			if (normalized.length() < ScamRules.similarityMinMessageLength()) {
				continue;
			}
			phrases.add(new PhraseEntry(rule, phrase, normalized));
		}
	}

	private static String normalizeForSimilarity(String input) {
		if (input == null) {
			return "";
		}
		String lower = input.toLowerCase(Locale.ROOT);
		String cleaned = lower.replaceAll("[^a-z0-9]+", " ").trim();
		int max = ScamRules.similarityMaxCompareLength();
		if (cleaned.length() > max) {
			return cleaned.substring(0, max);
		}
		return cleaned;
	}

	private static double similarity(String a, String b) {
		if (a.isBlank() || b.isBlank()) {
			return 0.0;
		}
		int distance = levenshteinDistance(a, b);
		int maxLen = Math.max(a.length(), b.length());
		if (maxLen == 0) {
			return 1.0;
		}
		return 1.0 - (distance / (double) maxLen);
	}

	private static int levenshteinDistance(String a, String b) {
		int lenA = a.length();
		int lenB = b.length();
		int[] prev = new int[lenB + 1];
		int[] curr = new int[lenB + 1];

		for (int j = 0; j <= lenB; j++) {
			prev[j] = j;
		}

		for (int i = 1; i <= lenA; i++) {
			curr[0] = i;
			char ca = a.charAt(i - 1);
			for (int j = 1; j <= lenB; j++) {
				char cb = b.charAt(j - 1);
				int cost = ca == cb ? 0 : 1;
				curr[j] = Math.min(
					Math.min(curr[j - 1] + 1, prev[j] + 1),
					prev[j - 1] + cost
				);
			}
			int[] swap = prev;
			prev = curr;
			curr = swap;
		}
		return prev[lenB];
	}

	private static String formatPercent(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private record PhraseEntry(ScamRules.ScamRule rule, String phrase, String normalized) {
	}

	private record PhraseMatch(String phrase, double similarity) {
	}

	private record TrainingMatch(String sample, double similarity) {
	}

	private static final class TrainingCache {
		private long lastModified;
		private TrainingSnapshot cached;

		private TrainingSnapshot loadIfNeeded() {
			try {
				if (!Files.exists(TRAINING_DATA_PATH)) {
					cached = null;
					lastModified = 0L;
					return null;
				}
				long modified = Files.getLastModifiedTime(TRAINING_DATA_PATH).toMillis();
				if (cached != null && modified == lastModified) {
					return cached;
				}
				cached = loadTrainingSamples();
				lastModified = modified;
				return cached;
			} catch (Exception ignored) {
				return cached;
			}
		}

		private TrainingSnapshot loadTrainingSamples() throws Exception {
			List<String> lines = Files.readAllLines(TRAINING_DATA_PATH, StandardCharsets.UTF_8);
			if (lines.size() < 2) {
				return new TrainingSnapshot(List.of());
			}
			List<TrainingSample> samples = new ArrayList<>();
			int scamCount = 0;
			int legitCount = 0;
			for (int i = lines.size() - 1; i >= 1; i--) {
				String line = lines.get(i).trim();
				if (line.isEmpty()) {
					continue;
				}
				List<String> cols = CsvLineParser.parse(line);
				if (cols.size() < 2) {
					continue;
				}
				String rawMessage = unescapeCsv(cols.get(0));
				if (rawMessage.isBlank()) {
					continue;
				}
				int label = parseInt(cols.get(1), -1);
				if (label != 0 && label != 1) {
					continue;
				}
				int maxSamples = ScamRules.similarityMaxTrainingSamples();
				if (label == 1 && scamCount >= maxSamples) {
					continue;
				}
				if (label == 0 && legitCount >= maxSamples) {
					continue;
				}
				String normalized = normalizeForSimilarity(rawMessage);
				if (normalized.length() < ScamRules.similarityMinMessageLength()) {
					continue;
				}
				samples.add(new TrainingSample(rawMessage, normalized, label));
				if (label == 1) {
					scamCount++;
				} else {
					legitCount++;
				}
				if (scamCount >= maxSamples && legitCount >= maxSamples) {
					break;
				}
			}
			return new TrainingSnapshot(samples);
		}

		private static String unescapeCsv(String value) {
			if (value == null) {
				return "";
			}
			String trimmed = value;
			if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
				trimmed = trimmed.substring(1, trimmed.length() - 1);
			}
			return trimmed.replace("\"\"", "\"").trim();
		}

		private static int parseInt(String value, int fallback) {
			try {
				return Integer.parseInt(value.trim());
			} catch (Exception ignored) {
				return fallback;
			}
		}
	}

	private record TrainingSample(String raw, String normalized, int label) {
	}

	private record TrainingSnapshot(List<TrainingSample> samples) {
	}
}
