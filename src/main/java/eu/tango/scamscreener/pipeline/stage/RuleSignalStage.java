package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import eu.tango.scamscreener.pipeline.core.RuleConfig;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;

public final class RuleSignalStage {
	private static final Pattern URGENCY_ALLOWLIST = Pattern.compile("\\b(auction|ah|flip|bin|bid|bidding)\\b");
	private static final Pattern TRADE_CONTEXT_ALLOWLIST = Pattern.compile("\\b(sell|selling|buy|buying|trade|trading|price|coins?|payment|pay|lf|lb)\\b");
	private static final Pattern DISCORD_HANDLE_PATTERN = Pattern.compile("@[a-z0-9._-]{2,32}");
	private static final Pattern DISCORD_WORD_PATTERN = Pattern.compile("\\bdiscord\\b");
	private static final int ENTROPY_MIN_TOKENS = 4;
	private static final int ENTROPY_MIN_LENGTH = 20;
	private static final double ENTROPY_THRESHOLD = 2.5;
	private static final List<String> URGENCY_KEYWORDS = List.of(
		"now",
		"quick",
		"fast",
		"urgent",
		"asap",
		"immediately",
		"right",
		"sofort",
		"jetzt"
	);
	private static final List<String> URGENCY_PHRASES = List.of(
		"right now",
		"right away",
		"as soon as possible",
		"need it now",
		"need this now",
		"need this right now",
		"fast fast",
		"quick payment"
	);
	private static final List<String> TRUST_KEYWORDS = List.of(
		"trust",
		"trusted",
		"legit",
		"safe",
		"verified",
		"vouched",
		"reputable",
		"middleman"
	);
	private static final List<String> TRUST_PHRASES = List.of(
		"trust me",
		"i am legit",
		"its legit",
		"it's legit",
		"safe trade",
		"trusted middleman",
		"legit middleman"
	);
	private static final int URGENCY_SCORE_THRESHOLD = 2;
	private static final int TRUST_SCORE_THRESHOLD = 2;
	private final RuleConfig ruleConfig;

	/**
	 * Creates rule-based signals by regex matching the chat message.
	 * These signals contribute to the total score in {@link ScoringStage}.
	 */
	public RuleSignalStage(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	/**
	 * Returns one {@link Signal} per triggered rule. Empty when nothing matches.
	 */
	public List<Signal> collectSignals(MessageEvent event) {
		if (event == null || event.normalizedMessage().isBlank()) {
			return List.of();
		}

		ScamRules.PatternSet patterns = ruleConfig.patterns();
		ScamRules.BehaviorPatternSet behaviorPatterns = ruleConfig.behaviorPatterns();
		String message = event.normalizedMessage();
		List<Signal> signals = new ArrayList<>();

		String linkMatch = firstMatch(patterns.link(), message);
		if (linkMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.SUSPICIOUS_LINK)) {
			signals.add(new Signal(
				ScamRules.ScamRule.SUSPICIOUS_LINK.name(),
				SignalSource.RULE,
				20,
				"Matched link pattern: \"" + linkMatch + "\" (+20)",
				ScamRules.ScamRule.SUSPICIOUS_LINK,
				List.of()
			));
		}

		if (ruleConfig.isEnabled(ScamRules.ScamRule.PRESSURE_AND_URGENCY)) {
			PhraseScore urgencyScore = scorePhrase(message, URGENCY_KEYWORDS, URGENCY_PHRASES);
			boolean hasSuspiciousContext = hasSuspiciousContext(message, patterns, behaviorPatterns);
			if (urgencyScore.score() >= URGENCY_SCORE_THRESHOLD && !(URGENCY_ALLOWLIST.matcher(message).find() && !hasSuspiciousContext)
				&& !(TRADE_CONTEXT_ALLOWLIST.matcher(message).find() && !hasSuspiciousContext)) {
				signals.add(new Signal(
					ScamRules.ScamRule.PRESSURE_AND_URGENCY.name(),
					SignalSource.RULE,
					15,
					"Urgency phrase score=" + urgencyScore.score() + " (keywords=" + urgencyScore.keywordHits()
						+ ", phrases=" + urgencyScore.phraseHits() + ", threshold=" + URGENCY_SCORE_THRESHOLD + ")"
						+ matchEvidence(urgencyScore.match()) + " (+15)",
					ScamRules.ScamRule.PRESSURE_AND_URGENCY,
					List.of()
				));
			}
		}

		String paymentMatch = firstMatch(patterns.paymentFirst(), message);
		if (paymentMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.UPFRONT_PAYMENT)) {
			signals.add(new Signal(
				ScamRules.ScamRule.UPFRONT_PAYMENT.name(),
				SignalSource.RULE,
				25,
				"Matched payment-first wording: \"" + paymentMatch + "\" (+25)",
				ScamRules.ScamRule.UPFRONT_PAYMENT,
				List.of()
			));
		}

		String accountMatch = firstMatch(patterns.accountData(), message);
		if (accountMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.ACCOUNT_DATA_REQUEST)) {
			signals.add(new Signal(
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST.name(),
				SignalSource.RULE,
				35,
				"Matched sensitive-account wording: \"" + accountMatch + "\" (+35)",
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST,
				List.of()
			));
		}

		String tooGoodMatch = firstMatch(patterns.tooGood(), message);
		if (tooGoodMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.TOO_GOOD_TO_BE_TRUE)) {
			signals.add(new Signal(
				ScamRules.ScamRule.TOO_GOOD_TO_BE_TRUE.name(),
				SignalSource.RULE,
				15,
				"Matched unrealistic-promise wording: \"" + tooGoodMatch + "\" (+15)",
				ScamRules.ScamRule.TOO_GOOD_TO_BE_TRUE,
				List.of()
			));
		}

		if (ruleConfig.isEnabled(ScamRules.ScamRule.TRUST_MANIPULATION)) {
			PhraseScore trustScore = scorePhrase(message, TRUST_KEYWORDS, TRUST_PHRASES);
			if (trustScore.score() >= TRUST_SCORE_THRESHOLD) {
				signals.add(new Signal(
					ScamRules.ScamRule.TRUST_MANIPULATION.name(),
					SignalSource.RULE,
					10,
					"Trust phrase score=" + trustScore.score() + " (keywords=" + trustScore.keywordHits()
						+ ", phrases=" + trustScore.phraseHits() + ", threshold=" + TRUST_SCORE_THRESHOLD + ")"
						+ matchEvidence(trustScore.match()) + " (+10)",
					ScamRules.ScamRule.TRUST_MANIPULATION,
					List.of()
				));
			}
		}

		int entropyBonusWeight = ScamRules.entropyBonusWeight();
		if (entropyBonusWeight < 0) {
			EntropyResult entropy = tokenEntropy(message);
			if (entropy.tokenCount() >= ENTROPY_MIN_TOKENS && entropy.length() >= ENTROPY_MIN_LENGTH && entropy.entropy() >= ENTROPY_THRESHOLD) {
				signals.add(new Signal(
					"ENTROPY_BONUS",
					SignalSource.RULE,
					entropyBonusWeight,
					"",
					null,
					List.of()
				));
			}
		}

		if (ruleConfig.isEnabled(ScamRules.ScamRule.DISCORD_HANDLE)) {
			Matcher handleMatch = DISCORD_HANDLE_PATTERN.matcher(message);
			if (DISCORD_WORD_PATTERN.matcher(message).find() && handleMatch.find()) {
				String handle = handleMatch.group();
				signals.add(new Signal(
					ScamRules.ScamRule.DISCORD_HANDLE.name(),
					SignalSource.RULE,
					50,
					"Discord handle with platform mention: \"" + handle + "\" (+50). External platform behavior skipped.",
					ScamRules.ScamRule.DISCORD_HANDLE,
					List.of()
				));
			}
		}

		return signals;
	}

	private static boolean hasSuspiciousContext(String message, ScamRules.PatternSet patterns, ScamRules.BehaviorPatternSet behaviorPatterns) {
		return matches(patterns.link(), message)
			|| matches(patterns.paymentFirst(), message)
			|| matches(patterns.accountData(), message)
			|| matches(patterns.tooGood(), message)
			|| matches(behaviorPatterns.externalPlatform(), message)
			|| matches(behaviorPatterns.upfrontPayment(), message)
			|| matches(behaviorPatterns.accountData(), message)
			|| matches(behaviorPatterns.middlemanClaim(), message);
	}

	private static boolean matches(Pattern pattern, String message) {
		if (pattern == null || message == null || message.isBlank()) {
			return false;
		}
		return pattern.matcher(message).find();
	}

	private static String firstMatch(Pattern pattern, String message) {
		Matcher matcher = pattern.matcher(message);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group();
	}

	private static PhraseScore scorePhrase(String message, List<String> keywords, List<String> phrases) {
		if (message == null || message.isBlank()) {
			return new PhraseScore(0, 0, 0, null);
		}
		List<String> tokens = tokenize(message);
		String normalized = String.join(" ", tokens);
		int keywordHits = countKeywordHits(tokens, keywords);
		int phraseHits = countPhraseHits(normalized, phrases);
		int score = keywordHits + (phraseHits * 2);
		String match = firstPhraseMatch(normalized, phrases);
		if (match == null) {
			match = firstKeywordMatch(tokens, keywords);
		}
		return new PhraseScore(score, keywordHits, phraseHits, match);
	}

	private static List<String> tokenize(String message) {
		String lower = message.toLowerCase(java.util.Locale.ROOT);
		String[] raw = lower.split("[^a-z0-9]+");
		List<String> tokens = new ArrayList<>();
		for (String token : raw) {
			if (!token.isBlank()) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	private static int countKeywordHits(List<String> tokens, List<String> keywords) {
		int hits = 0;
		for (String token : tokens) {
			for (String keyword : keywords) {
				if (token.equals(keyword)) {
					hits++;
					if (hits >= 4) {
						return hits;
					}
				}
			}
		}
		return hits;
	}

	private static int countPhraseHits(String normalized, List<String> phrases) {
		int hits = 0;
		String padded = " " + normalized + " ";
		for (String phrase : phrases) {
			String needle = " " + phrase + " ";
			if (padded.contains(needle)) {
				hits++;
			}
		}
		return hits;
	}

	private static String firstPhraseMatch(String normalized, List<String> phrases) {
		String padded = " " + normalized + " ";
		for (String phrase : phrases) {
			String needle = " " + phrase + " ";
			if (padded.contains(needle)) {
				return phrase;
			}
		}
		return null;
	}

	private static String firstKeywordMatch(List<String> tokens, List<String> keywords) {
		for (String token : tokens) {
			for (String keyword : keywords) {
				if (token.equals(keyword)) {
					return token;
				}
			}
		}
		return null;
	}

	private static String matchEvidence(String match) {
		if (match == null || match.isBlank()) {
			return "";
		}
		return " Match: \"" + match + "\"";
	}

	private static EntropyResult tokenEntropy(String message) {
		if (message == null || message.isBlank()) {
			return new EntropyResult(0.0, 0, 0);
		}
		List<String> tokens = tokenize(message);
		int tokenCount = tokens.size();
		int length = message.length();
		if (tokenCount == 0) {
			return new EntropyResult(0.0, 0, length);
		}
		java.util.Map<String, Integer> counts = new java.util.HashMap<>();
		for (String token : tokens) {
			counts.merge(token, 1, Integer::sum);
		}
		double total = tokenCount;
		double entropy = 0.0;
		for (int count : counts.values()) {
			double p = count / total;
			entropy += -p * (Math.log(p) / Math.log(2));
		}
		return new EntropyResult(entropy, tokenCount, length);
	}

	private record EntropyResult(double entropy, int tokenCount, int length) {
	}

	private record PhraseScore(int score, int keywordHits, int phraseHits, String match) {
	}
}
