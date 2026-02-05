package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public final class LocalAiScorer {
	private static final String[] PAYMENT_WORDS = {"pay", "payment", "vorkasse", "coins", "money", "btc", "crypto"};
	private static final String[] ACCOUNT_WORDS = {"password", "passwort", "2fa", "code", "email", "login"};
	private static final String[] URGENCY_WORDS = {"now", "quick", "fast", "urgent", "sofort", "jetzt"};
	private static final String[] TRUST_WORDS = {"trust", "legit", "safe", "trusted", "middleman"};
	private static final String[] TOO_GOOD_WORDS = {"free", "100%", "guaranteed", "garantiert", "dupe", "rank"};
	private static final String[] PLATFORM_WORDS = {"discord", "telegram", "t.me", "server", "dm"};
	private volatile ModelWeights model = ModelWeights.from(LocalAiModelConfig.loadOrCreate());

	public void reloadModel() {
		model = ModelWeights.from(LocalAiModelConfig.loadOrCreate());
	}

	public AiResult score(ScamRules.BehaviorContext context, int maxScore, double triggerProbability) {
		Features f = extractFeatures(context);
		ModelWeights w = model;
		double linear = w.intercept
			+ f.hasPaymentWords * w.hasPaymentWords
			+ f.hasAccountWords * w.hasAccountWords
			+ f.hasUrgencyWords * w.hasUrgencyWords
			+ f.hasTrustWords * w.hasTrustWords
			+ f.hasTooGoodWords * w.hasTooGoodWords
			+ f.hasPlatformWords * w.hasPlatformWords
			+ f.hasLink * w.hasLink
			+ f.hasSuspiciousPunctuation * w.hasSuspiciousPunctuation
			+ f.ctxPushesExternalPlatform * w.ctxPushesExternalPlatform
			+ f.ctxDemandsUpfrontPayment * w.ctxDemandsUpfrontPayment
			+ f.ctxRequestsSensitiveData * w.ctxRequestsSensitiveData
			+ f.ctxClaimsMiddlemanWithoutProof * w.ctxClaimsMiddlemanWithoutProof
			+ f.ctxTooGoodToBeTrue * w.ctxTooGoodToBeTrue
			+ f.ctxRepeatedContact3Plus * w.ctxRepeatedContact3Plus
			+ f.ctxIsSpam * w.ctxIsSpam
			+ f.ctxAsksForStuff * w.ctxAsksForStuff
			+ f.ctxAdvertising * w.ctxAdvertising;
		linear += tokenContribution(context.message(), w.tokenWeights);

		double probability = sigmoid(linear);
		int rawScore = (int) Math.round(probability * clampScore(maxScore));
		boolean triggered = probability >= clampProbability(triggerProbability);
		int appliedScore = triggered ? rawScore : 0;
		String explanation = buildExplanation(context.message(), f, w);

		return new AiResult(appliedScore, probability, triggered, explanation);
	}

	private static Features extractFeatures(ScamRules.BehaviorContext context) {
		String message = normalize(context.message());
		return new Features(
			bool(hasAny(message, PAYMENT_WORDS)),
			bool(hasAny(message, ACCOUNT_WORDS)),
			bool(hasAny(message, URGENCY_WORDS)),
			bool(hasAny(message, TRUST_WORDS)),
			bool(hasAny(message, TOO_GOOD_WORDS)),
			bool(hasAny(message, PLATFORM_WORDS)),
			bool(hasLink(message)),
			bool(hasSuspiciousPunctuation(message)),
			bool(context.pushesExternalPlatform()),
			bool(context.demandsUpfrontPayment()),
			bool(context.requestsSensitiveData()),
			bool(context.claimsTrustedMiddlemanWithoutProof()),
			bool(hasAny(message, TOO_GOOD_WORDS)),
			bool(context.repeatedContactAttempts() >= 3),
			bool(hasAny(message, "spam", "last chance", "buy now", "cheap", "limited")),
			bool(hasAny(message, "can i borrow", "borrow", "lend me", "give me", "can i have")),
			bool(hasAny(message, "/visit", "visit me", "join my", "discord.gg", "shop", "selling"))
		);
	}

	private static boolean hasAny(String text, String[] words) {
		for (String word : words) {
			if (text.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasAny(String text, String first, String... others) {
		if (text.contains(first)) {
			return true;
		}
		for (String token : others) {
			if (text.contains(token)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLink(String text) {
		return text.contains("http://") || text.contains("https://") || text.contains("www.");
	}

	private static boolean hasSuspiciousPunctuation(String text) {
		return text.contains("!!!") || text.contains("??") || text.contains("$$");
	}

	private static double bool(boolean value) {
		return value ? 1.0 : 0.0;
	}

	private static int clampScore(int value) {
		return Math.max(0, Math.min(100, value));
	}

	private static double clampProbability(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	private static String normalize(String text) {
		return text == null ? "" : text.toLowerCase(Locale.ROOT);
	}

	private static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

	private static double tokenContribution(String message, Map<String, Double> tokenWeights) {
		if (tokenWeights == null || tokenWeights.isEmpty()) {
			return 0.0;
		}

		Set<String> tokens = TokenFeatureExtractor.extractFeatureTokens(message);
		double sum = 0.0;
		for (String token : tokens) {
			Double weight = tokenWeights.get(token);
			if (weight != null) {
				sum += weight;
			}
		}

		// Backward compatibility: support old model files that stored plain unigram keys without prefixes.
		for (String token : TokenFeatureExtractor.tokenizeWords(message)) {
			Double weight = tokenWeights.get(token);
			if (weight != null) {
				sum += weight;
			}
		}
		return sum;
	}

	private static String buildExplanation(String message, Features f, ModelWeights w) {
		List<Contribution> contributions = new ArrayList<>();
		addFeatureContribution(contributions, "hasPaymentWords", f.hasPaymentWords, w.hasPaymentWords);
		addFeatureContribution(contributions, "hasAccountWords", f.hasAccountWords, w.hasAccountWords);
		addFeatureContribution(contributions, "hasUrgencyWords", f.hasUrgencyWords, w.hasUrgencyWords);
		addFeatureContribution(contributions, "hasTrustWords", f.hasTrustWords, w.hasTrustWords);
		addFeatureContribution(contributions, "hasTooGoodWords", f.hasTooGoodWords, w.hasTooGoodWords);
		addFeatureContribution(contributions, "hasPlatformWords", f.hasPlatformWords, w.hasPlatformWords);
		addFeatureContribution(contributions, "hasLink", f.hasLink, w.hasLink);
		addFeatureContribution(contributions, "hasSuspiciousPunctuation", f.hasSuspiciousPunctuation, w.hasSuspiciousPunctuation);
		addFeatureContribution(contributions, "ctxPushesExternalPlatform", f.ctxPushesExternalPlatform, w.ctxPushesExternalPlatform);
		addFeatureContribution(contributions, "ctxDemandsUpfrontPayment", f.ctxDemandsUpfrontPayment, w.ctxDemandsUpfrontPayment);
		addFeatureContribution(contributions, "ctxRequestsSensitiveData", f.ctxRequestsSensitiveData, w.ctxRequestsSensitiveData);
		addFeatureContribution(contributions, "ctxClaimsMiddlemanWithoutProof", f.ctxClaimsMiddlemanWithoutProof, w.ctxClaimsMiddlemanWithoutProof);
		addFeatureContribution(contributions, "ctxTooGoodToBeTrue", f.ctxTooGoodToBeTrue, w.ctxTooGoodToBeTrue);
		addFeatureContribution(contributions, "ctxRepeatedContact3Plus", f.ctxRepeatedContact3Plus, w.ctxRepeatedContact3Plus);
		addFeatureContribution(contributions, "ctxIsSpam", f.ctxIsSpam, w.ctxIsSpam);
		addFeatureContribution(contributions, "ctxAsksForStuff", f.ctxAsksForStuff, w.ctxAsksForStuff);
		addFeatureContribution(contributions, "ctxAdvertising", f.ctxAdvertising, w.ctxAdvertising);

		if (w.tokenWeights != null && !w.tokenWeights.isEmpty()) {
			for (String token : TokenFeatureExtractor.extractFeatureTokens(message)) {
				Double weight = w.tokenWeights.get(token);
				if (weight != null) {
					contributions.add(new Contribution("token " + token, weight));
				}
			}
			for (String token : TokenFeatureExtractor.tokenizeWords(message)) {
				Double weight = w.tokenWeights.get(token);
				if (weight != null) {
					contributions.add(new Contribution("token " + token, weight));
				}
			}
		}

		if (contributions.isEmpty()) {
			return "Top model factors: none";
		}

		contributions.sort(Comparator.comparingDouble((Contribution c) -> Math.abs(c.value())).reversed());
		int limit = Math.min(4, contributions.size());
		StringBuilder text = new StringBuilder("Top model factors:");
		for (int i = 0; i < limit; i++) {
			Contribution c = contributions.get(i);
			text.append("\n- ").append(c.label()).append(" (").append(formatSigned(c.value())).append(")");
		}
		return text.toString();
	}

	private static void addFeatureContribution(List<Contribution> out, String label, double featureValue, double weight) {
		if (featureValue <= 0.0) {
			return;
		}
		double contribution = featureValue * weight;
		out.add(new Contribution(label, contribution));
	}

	private static String formatSigned(double value) {
		return String.format(Locale.ROOT, "%+.3f", value);
	}

	private record Contribution(String label, double value) {
	}

	public record AiResult(int score, double probability, boolean triggered, String explanation) {
	}

	private record Features(
		double hasPaymentWords,
		double hasAccountWords,
		double hasUrgencyWords,
		double hasTrustWords,
		double hasTooGoodWords,
		double hasPlatformWords,
		double hasLink,
		double hasSuspiciousPunctuation,
		double ctxPushesExternalPlatform,
		double ctxDemandsUpfrontPayment,
		double ctxRequestsSensitiveData,
		double ctxClaimsMiddlemanWithoutProof,
		double ctxTooGoodToBeTrue,
		double ctxRepeatedContact3Plus,
		double ctxIsSpam,
		double ctxAsksForStuff,
		double ctxAdvertising
	) {
	}

	private record ModelWeights(
		double intercept,
		double hasPaymentWords,
		double hasAccountWords,
		double hasUrgencyWords,
		double hasTrustWords,
		double hasTooGoodWords,
		double hasPlatformWords,
		double hasLink,
		double hasSuspiciousPunctuation,
		double ctxPushesExternalPlatform,
		double ctxDemandsUpfrontPayment,
		double ctxRequestsSensitiveData,
		double ctxClaimsMiddlemanWithoutProof,
		double ctxTooGoodToBeTrue,
		double ctxRepeatedContact3Plus,
		double ctxIsSpam,
		double ctxAsksForStuff,
		double ctxAdvertising,
		Map<String, Double> tokenWeights
	) {
		private static ModelWeights from(LocalAiModelConfig cfg) {
			return new ModelWeights(
				cfg.intercept,
				cfg.hasPaymentWords,
				cfg.hasAccountWords,
				cfg.hasUrgencyWords,
				cfg.hasTrustWords,
				cfg.hasTooGoodWords,
				cfg.hasPlatformWords,
				cfg.hasLink,
				cfg.hasSuspiciousPunctuation,
				cfg.ctxPushesExternalPlatform,
				cfg.ctxDemandsUpfrontPayment,
				cfg.ctxRequestsSensitiveData,
				cfg.ctxClaimsMiddlemanWithoutProof,
				cfg.ctxTooGoodToBeTrue,
				cfg.ctxRepeatedContact3Plus,
				cfg.ctxIsSpam,
				cfg.ctxAsksForStuff,
				cfg.ctxAdvertising,
				cfg.tokenWeights
			);
		}
	}
}
