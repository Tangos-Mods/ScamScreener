package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class AiFeatureSpace {
	private static final String REGEX_PREFIX = "re:";
	private static final String[] PAYMENT_WORDS = {"pay", "payment", "vorkasse", "coins", "money", "btc", "crypto"};
	private static final String[] ACCOUNT_WORDS = {
		"password",
		"passwort",
		"2fa",
		"re:\\b(?:give|gimme)\\b.*\\bcode\\b|\\bcode\\b.*\\b(?:give|gimme)\\b",
		"email",
		"login"
	};
	private static final String[] URGENCY_WORDS = {"now", "quick", "fast", "urgent", "sofort", "jetzt"};
	private static final String[] TRUST_WORDS = {"trust", "legit", "safe", "trusted", "middleman"};
	private static final String[] TOO_GOOD_WORDS = {"free", "100%", "guaranteed", "garantiert", "dupe", "rank"};
	private static final String[] PLATFORM_WORDS = {"discord", "telegram", "t.me", "re:\\b(?:join|come on)\\b.*\\bserver\\b", "dm", "vc", "voice"};

	public static final List<String> DENSE_FEATURE_NAMES = List.of(
		"kw_payment",
		"kw_account",
		"kw_urgency",
		"kw_trust",
		"kw_too_good",
		"kw_platform",
		"has_link",
		"has_suspicious_punctuation",
		"ctx_pushes_external_platform",
		"ctx_demands_upfront_payment",
		"ctx_requests_sensitive_data",
		"ctx_claims_middleman_without_proof",
		"ctx_too_good_to_be_true",
		"ctx_repeated_contact_3plus",
		"ctx_is_spam",
		"ctx_asks_for_stuff",
		"ctx_advertising",
		"intent_offer",
		"intent_rep",
		"intent_redirect",
		"intent_instruction",
		"intent_payment",
		"intent_anchor",
		"funnel_step_norm",
		"funnel_sequence_norm",
		"funnel_full_chain",
		"funnel_partial_chain",
		"rapid_followup",
		"channel_pm",
		"channel_party",
		"channel_public",
		"rule_hits_norm",
		"similarity_hits_norm",
		"behavior_hits_norm",
		"trend_hits_norm",
		"funnel_hits_norm"
	);
	public static final List<String> FUNNEL_DENSE_FEATURE_NAMES = List.of(
		"ctx_pushes_external_platform",
		"ctx_repeated_contact_3plus",
		"intent_offer",
		"intent_rep",
		"intent_redirect",
		"intent_instruction",
		"intent_payment",
		"intent_anchor",
		"funnel_step_norm",
		"funnel_sequence_norm",
		"funnel_full_chain",
		"funnel_partial_chain",
		"rapid_followup",
		"funnel_hits_norm"
	);
	private static final Set<String> FUNNEL_DENSE_FEATURE_SET = Set.copyOf(FUNNEL_DENSE_FEATURE_NAMES);

	private AiFeatureSpace() {
	}

	public static Map<String, Double> extractDenseFeatures(ScamRules.BehaviorContext context) {
		ScamRules.BehaviorContext safe = context == null ? emptyContext() : context;
		String message = normalize(safe.message());
		double rapidFollowup = safe.deltaMs() <= 0
			? 0.0
			: 1.0 - clamp01(safe.deltaMs() / 120_000.0);

		Map<String, Double> out = new LinkedHashMap<>();
		out.put("kw_payment", bool(hasAny(message, PAYMENT_WORDS)));
		out.put("kw_account", bool(hasAny(message, ACCOUNT_WORDS)));
		out.put("kw_urgency", bool(hasAny(message, URGENCY_WORDS)));
		out.put("kw_trust", bool(hasAny(message, TRUST_WORDS)));
		out.put("kw_too_good", bool(hasAny(message, TOO_GOOD_WORDS)));
		out.put("kw_platform", bool(hasAny(message, PLATFORM_WORDS)));
		out.put("has_link", bool(hasLink(message)));
		out.put("has_suspicious_punctuation", bool(hasSuspiciousPunctuation(message)));
		out.put("ctx_pushes_external_platform", bool(safe.pushesExternalPlatform()));
		out.put("ctx_demands_upfront_payment", bool(safe.demandsUpfrontPayment()));
		out.put("ctx_requests_sensitive_data", bool(safe.requestsSensitiveData()));
		out.put("ctx_claims_middleman_without_proof", bool(safe.claimsTrustedMiddlemanWithoutProof()));
		out.put("ctx_too_good_to_be_true", bool(safe.tooGoodToBeTrue()));
		out.put("ctx_repeated_contact_3plus", bool(safe.repeatedContactAttempts() >= 3));
		out.put("ctx_is_spam", bool(safe.isSpam()));
		out.put("ctx_asks_for_stuff", bool(safe.asksForStuff()));
		out.put("ctx_advertising", bool(safe.advertising()));
		out.put("intent_offer", bool(safe.intentOffer()));
		out.put("intent_rep", bool(safe.intentRep()));
		out.put("intent_redirect", bool(safe.intentRedirect()));
		out.put("intent_instruction", bool(safe.intentInstruction()));
		out.put("intent_payment", bool(safe.intentPaymentUpfront()));
		out.put("intent_anchor", bool(safe.intentCommunityAnchor()));
		out.put("funnel_step_norm", clamp01(safe.funnelStepIndex() / 4.0));
		out.put("funnel_sequence_norm", clamp01(safe.funnelSequenceScore() / 40.0));
		out.put("funnel_full_chain", bool(safe.funnelFullChain()));
		out.put("funnel_partial_chain", bool(safe.funnelPartialChain()));
		out.put("rapid_followup", rapidFollowup);
		out.put("channel_pm", bool("pm".equalsIgnoreCase(safe.channel())));
		out.put("channel_party", bool("party".equalsIgnoreCase(safe.channel())));
		out.put("channel_public", bool("public".equalsIgnoreCase(safe.channel())));
		out.put("rule_hits_norm", clamp01(safe.ruleHits() / 3.0));
		out.put("similarity_hits_norm", clamp01(safe.similarityHits() / 2.0));
		out.put("behavior_hits_norm", clamp01(safe.behaviorHits() / 3.0));
		out.put("trend_hits_norm", clamp01(safe.trendHits() / 2.0));
		out.put("funnel_hits_norm", clamp01(safe.funnelHits() / 2.0));
		return out;
	}

	public static Map<String, Double> defaultDenseWeights() {
		Map<String, Double> defaults = new LinkedHashMap<>();
		for (String name : DENSE_FEATURE_NAMES) {
			defaults.put(name, 0.0);
		}
		defaults.put("kw_account", 1.1);
		defaults.put("kw_payment", 0.7);
		defaults.put("kw_platform", 0.4);
		defaults.put("ctx_requests_sensitive_data", 1.2);
		defaults.put("ctx_demands_upfront_payment", 0.75);
		defaults.put("intent_redirect", 0.45);
		defaults.put("intent_instruction", 0.5);
		defaults.put("funnel_full_chain", 0.9);
		defaults.put("funnel_partial_chain", 0.45);
		defaults.put("funnel_sequence_norm", 0.55);
		defaults.put("rule_hits_norm", 0.35);
		defaults.put("funnel_hits_norm", 0.4);
		defaults.put("rapid_followup", 0.25);
		return defaults;
	}

	public static Map<String, Double> defaultFunnelDenseWeights() {
		Map<String, Double> defaults = defaultDenseWeights();
		Map<String, Double> out = new LinkedHashMap<>();
		for (String name : DENSE_FEATURE_NAMES) {
			if (!FUNNEL_DENSE_FEATURE_SET.contains(name)) {
				continue;
			}
			out.put(name, defaults.getOrDefault(name, 0.0));
		}
		return out;
	}

	public static boolean isFunnelDenseFeature(String name) {
		return name != null && FUNNEL_DENSE_FEATURE_SET.contains(name);
	}

	private static ScamRules.BehaviorContext emptyContext() {
		return new ScamRules.BehaviorContext(
			"",
			"unknown",
			0L,
			false,
			false,
			false,
			false,
			0,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			0,
			0.0,
			false,
			false,
			0,
			0,
			0,
			0,
			0
		);
	}

	private static boolean hasAny(String text, String[] words) {
		for (String word : words) {
			if (matchesWord(text, word)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesWord(String text, String word) {
		if (word == null || word.isEmpty()) {
			return false;
		}
		if (!word.startsWith(REGEX_PREFIX)) {
			return text.contains(word);
		}
		String regex = word.substring(REGEX_PREFIX.length());
		if (regex.isEmpty()) {
			return false;
		}
		try {
			return Pattern.compile(regex).matcher(text).find();
		} catch (PatternSyntaxException ignored) {
			return text.contains(regex);
		}
	}

	private static boolean hasLink(String text) {
		return text.contains("http://") || text.contains("https://") || text.contains("www.");
	}

	private static boolean hasSuspiciousPunctuation(String text) {
		return text.contains("!!!") || text.contains("??") || text.contains("$$");
	}

	private static String normalize(String text) {
		if (text == null) {
			return "";
		}
		return text.toLowerCase(Locale.ROOT);
	}

	private static double bool(boolean value) {
		return value ? 1.0 : 0.0;
	}

	private static double clamp01(double value) {
		if (Double.isNaN(value) || value <= 0.0) {
			return 0.0;
		}
		if (value >= 1.0) {
			return 1.0;
		}
		return value;
	}
}
