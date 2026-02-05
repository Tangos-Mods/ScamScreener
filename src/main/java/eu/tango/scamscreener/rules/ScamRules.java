package eu.tango.scamscreener.rules;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.config.ScamRulesConfig;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ScamRules {
	private static final LocalAiScorer LOCAL_AI_SCORER = new LocalAiScorer();
	private static RuntimeConfig config = RuntimeConfig.from(ScamRulesConfig.loadOrCreate());

	private ScamRules() {
	}

	public static void reloadConfig() {
		config = RuntimeConfig.from(ScamRulesConfig.loadOrCreate());
		LOCAL_AI_SCORER.reloadModel();
	}

	public static ScamRiskLevel minimumAlertRiskLevel() {
		return config.minimumAlertRiskLevel();
	}

	public static PatternSet patternSet() {
		return config.patterns();
	}

	public static BehaviorPatternSet behaviorPatternSet() {
		return config.behaviorPatterns();
	}

	public static boolean localAiEnabled() {
		return config.localAiEnabled();
	}

	public static int localAiMaxScore() {
		return config.localAiMaxScore();
	}

	public static double localAiTriggerProbability() {
		return config.localAiTriggerProbability();
	}

	public static String autoCaptureAlertLevelSetting() {
		return config.autoCaptureAlertLevelSetting();
	}

	public static ScamRiskLevel setMinimumAlertRiskLevel(ScamRiskLevel level) {
		Objects.requireNonNull(level, "level");
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.minAlertRiskLevel = level.name();
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.minimumAlertRiskLevel();
	}

	public static String setAutoCaptureAlertLevelSetting(String setting) {
		AutoCaptureAlertLevel parsed = AutoCaptureAlertLevel.parse(setting);
		if (parsed == null) {
			return null;
		}

		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.autoCaptureAlertLevel = parsed.persistedValue();
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.autoCaptureAlertLevelSetting();
	}

	public static Set<ScamRule> disabledRules() {
		Set<ScamRule> disabled = config.disabledRules();
		return disabled.isEmpty() ? EnumSet.noneOf(ScamRule.class) : EnumSet.copyOf(disabled);
	}

	public static Set<ScamRule> allRules() {
		return EnumSet.allOf(ScamRule.class);
	}

	public static boolean isRuleEnabled(ScamRule rule) {
		Objects.requireNonNull(rule, "rule");
		return config.isEnabled(rule);
	}

	public static boolean disableRule(ScamRule rule) {
		Objects.requireNonNull(rule, "rule");
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		if (cfg.disabledRules == null) {
			cfg.disabledRules = new LinkedHashSet<>();
		}
		boolean changed = cfg.disabledRules.add(rule.name());
		if (changed) {
			ScamRulesConfig.save(cfg);
			reloadConfig();
		}
		return changed;
	}

	public static boolean enableRule(ScamRule rule) {
		Objects.requireNonNull(rule, "rule");
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		if (cfg.disabledRules == null || cfg.disabledRules.isEmpty()) {
			return false;
		}
		boolean changed = cfg.disabledRules.remove(rule.name());
		if (changed) {
			ScamRulesConfig.save(cfg);
			reloadConfig();
		}
		return changed;
	}

	public static boolean shouldWarn(ScamAssessment assessment) {
		if (assessment == null) {
			return false;
		}
		if (assessment.riskScore() <= 0 || assessment.triggeredRules() == null || assessment.triggeredRules().isEmpty()) {
			return false;
		}
		return assessment.riskLevel().ordinal() >= config.minimumAlertRiskLevel().ordinal();
	}

	public static boolean shouldAutoCaptureAlert(ScamAssessment assessment) {
		if (assessment == null) {
			return false;
		}
		if (assessment.riskScore() <= 0 || assessment.triggeredRules() == null || assessment.triggeredRules().isEmpty()) {
			return false;
		}
		AutoCaptureAlertLevel setting = config.autoCaptureAlertLevel();
		if (setting == AutoCaptureAlertLevel.OFF) {
			return false;
		}
		return assessment.riskLevel().ordinal() >= setting.minimumLevel().ordinal();
	}

	public static ScamAssessment assess(BehaviorContext context) {
		EnumSet<ScamRule> triggered = EnumSet.noneOf(ScamRule.class);
		LinkedHashMap<ScamRule, String> details = new LinkedHashMap<>();
		int score = 0;

		String message = normalize(context.message());
		if (!message.isEmpty()) {
			String linkMatch = firstMatch(config.patterns().link(), message);
			if (linkMatch != null && config.isEnabled(ScamRule.SUSPICIOUS_LINK)) {
				triggered.add(ScamRule.SUSPICIOUS_LINK);
				details.put(ScamRule.SUSPICIOUS_LINK, "Matched link pattern: \"" + linkMatch + "\" (+20)");
				score += 20;
			}
			String urgencyMatch = firstMatch(config.patterns().urgency(), message);
			if (urgencyMatch != null && config.isEnabled(ScamRule.PRESSURE_AND_URGENCY)) {
				triggered.add(ScamRule.PRESSURE_AND_URGENCY);
				details.put(ScamRule.PRESSURE_AND_URGENCY, "Matched urgency wording: \"" + urgencyMatch + "\" (+15)");
				score += 15;
			}
			String paymentMatch = firstMatch(config.patterns().paymentFirst(), message);
			if (paymentMatch != null && config.isEnabled(ScamRule.UPFRONT_PAYMENT)) {
				triggered.add(ScamRule.UPFRONT_PAYMENT);
				details.put(ScamRule.UPFRONT_PAYMENT, "Matched payment-first wording: \"" + paymentMatch + "\" (+25)");
				score += 25;
			}
			String accountMatch = firstMatch(config.patterns().accountData(), message);
			if (accountMatch != null && config.isEnabled(ScamRule.ACCOUNT_DATA_REQUEST)) {
				triggered.add(ScamRule.ACCOUNT_DATA_REQUEST);
				details.put(ScamRule.ACCOUNT_DATA_REQUEST, "Matched sensitive-account wording: \"" + accountMatch + "\" (+35)");
				score += 35;
			}
			String tooGoodMatch = firstMatch(config.patterns().tooGood(), message);
			if (tooGoodMatch != null && config.isEnabled(ScamRule.TOO_GOOD_TO_BE_TRUE)) {
				triggered.add(ScamRule.TOO_GOOD_TO_BE_TRUE);
				details.put(ScamRule.TOO_GOOD_TO_BE_TRUE, "Matched unrealistic-promise wording: \"" + tooGoodMatch + "\" (+15)");
				score += 15;
			}
			String trustMatch = firstMatch(config.patterns().trustBait(), message);
			if (trustMatch != null && config.isEnabled(ScamRule.TRUST_MANIPULATION)) {
				triggered.add(ScamRule.TRUST_MANIPULATION);
				details.put(ScamRule.TRUST_MANIPULATION, "Matched trust-bait wording: \"" + trustMatch + "\" (+10)");
				score += 10;
			}
		}

		if (context.pushesExternalPlatform() && config.isEnabled(ScamRule.EXTERNAL_PLATFORM_PUSH)) {
			triggered.add(ScamRule.EXTERNAL_PLATFORM_PUSH);
			details.put(ScamRule.EXTERNAL_PLATFORM_PUSH, "Behavior flag pushesExternalPlatform=true (+15)");
			score += 15;
		}
		if (context.demandsUpfrontPayment() && config.isEnabled(ScamRule.UPFRONT_PAYMENT)) {
			triggered.add(ScamRule.UPFRONT_PAYMENT);
			details.merge(
				ScamRule.UPFRONT_PAYMENT,
				"Behavior flag demandsUpfrontPayment=true (+25)",
				(existing, added) -> existing + "\n" + added
			);
			score += 25;
		}
		if (context.requestsSensitiveData() && config.isEnabled(ScamRule.ACCOUNT_DATA_REQUEST)) {
			triggered.add(ScamRule.ACCOUNT_DATA_REQUEST);
			details.merge(
				ScamRule.ACCOUNT_DATA_REQUEST,
				"Behavior flag requestsSensitiveData=true (+35)",
				(existing, added) -> existing + "\n" + added
			);
			score += 35;
		}
		if (context.claimsTrustedMiddlemanWithoutProof() && config.isEnabled(ScamRule.FAKE_MIDDLEMAN_CLAIM)) {
			triggered.add(ScamRule.FAKE_MIDDLEMAN_CLAIM);
			details.put(ScamRule.FAKE_MIDDLEMAN_CLAIM, "Behavior flag claimsTrustedMiddlemanWithoutProof=true (+20)");
			score += 20;
		}
		if (context.repeatedContactAttempts() >= 3 && config.isEnabled(ScamRule.SPAMMY_CONTACT_PATTERN)) {
			triggered.add(ScamRule.SPAMMY_CONTACT_PATTERN);
			details.put(ScamRule.SPAMMY_CONTACT_PATTERN, "Repeated contact attempts=" + context.repeatedContactAttempts() + " (threshold: 3, +10)");
			score += 10;
		}
		if (config.localAiEnabled()) {
			LocalAiScorer.AiResult aiResult = LOCAL_AI_SCORER.score(
				context,
				config.localAiMaxScore(),
				config.localAiTriggerProbability()
			);
			if (aiResult.triggered() && aiResult.score() > 0 && config.isEnabled(ScamRule.LOCAL_AI_RISK_SIGNAL)) {
				triggered.add(ScamRule.LOCAL_AI_RISK_SIGNAL);
				details.put(
					ScamRule.LOCAL_AI_RISK_SIGNAL,
					"Local AI probability=" + round(aiResult.probability()) + ", threshold=" + round(config.localAiTriggerProbability()) + " (+"
						+ aiResult.score() + ")\n"
						+ aiResult.explanation()
				);
				score += aiResult.score();
			}
		}

		return new ScamAssessment(
			score,
			mapLevel(score),
			triggered,
			details,
			context.message(),
			List.of(context.message())
		);
	}

	private static ScamRiskLevel mapLevel(int score) {
		if (score >= 70) {
			return ScamRiskLevel.CRITICAL;
		}
		if (score >= 40) {
			return ScamRiskLevel.HIGH;
		}
		if (score >= 20) {
			return ScamRiskLevel.MEDIUM;
		}
		return ScamRiskLevel.LOW;
	}

	private static String normalize(String message) {
		return message == null ? "" : message.toLowerCase(Locale.ROOT);
	}

	public enum ScamRule {
		SUSPICIOUS_LINK,
		PRESSURE_AND_URGENCY,
		UPFRONT_PAYMENT,
		ACCOUNT_DATA_REQUEST,
		EXTERNAL_PLATFORM_PUSH,
		FAKE_MIDDLEMAN_CLAIM,
		TOO_GOOD_TO_BE_TRUE,
		TRUST_MANIPULATION,
		SPAMMY_CONTACT_PATTERN,
		MULTI_MESSAGE_PATTERN,
		LOCAL_AI_RISK_SIGNAL
	}

	public enum ScamRiskLevel {
		LOW,
		MEDIUM,
		HIGH,
		CRITICAL
	}

	public record BehaviorContext(
		String message,
		boolean pushesExternalPlatform,
		boolean demandsUpfrontPayment,
		boolean requestsSensitiveData,
		boolean claimsTrustedMiddlemanWithoutProof,
		int repeatedContactAttempts
	) {
	}

	public record ScamAssessment(
		int riskScore,
		ScamRiskLevel riskLevel,
		Set<ScamRule> triggeredRules,
		Map<ScamRule, String> ruleDetails,
		String evaluatedMessage,
		List<String> evaluatedMessages
	) {
		public boolean shouldWarn() {
			return riskLevel == ScamRiskLevel.HIGH || riskLevel == ScamRiskLevel.CRITICAL;
		}

		public String detailFor(ScamRule rule) {
			if (ruleDetails == null) {
				return null;
			}
			return ruleDetails.get(rule);
		}

		public List<String> allEvaluatedMessages() {
			if (evaluatedMessages != null && !evaluatedMessages.isEmpty()) {
				return evaluatedMessages;
			}
			return evaluatedMessage == null || evaluatedMessage.isBlank() ? List.of() : List.of(evaluatedMessage);
		}
	}

	public static record PatternSet(
		Pattern link,
		Pattern urgency,
		Pattern paymentFirst,
		Pattern accountData,
		Pattern tooGood,
		Pattern trustBait
	) {
		private static PatternSet from(ScamRulesConfig config) {
			return new PatternSet(
				compileOrDefault(config.linkPattern, ScamRulesConfig.DEFAULT_LINK_PATTERN),
				compileOrDefault(config.urgencyPattern, ScamRulesConfig.DEFAULT_URGENCY_PATTERN),
				compileOrDefault(config.paymentFirstPattern, ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
				compileOrDefault(config.accountDataPattern, ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
				compileOrDefault(config.tooGoodPattern, ScamRulesConfig.DEFAULT_TOO_GOOD_PATTERN),
				compileOrDefault(config.trustBaitPattern, ScamRulesConfig.DEFAULT_TRUST_BAIT_PATTERN)
			);
		}
	}

	public static record BehaviorPatternSet(
		Pattern externalPlatform,
		Pattern upfrontPayment,
		Pattern accountData,
		Pattern middlemanClaim
	) {
		private static BehaviorPatternSet from(ScamRulesConfig config) {
			return new BehaviorPatternSet(
				compileOrDefault(config.externalPlatformPattern, ScamRulesConfig.DEFAULT_EXTERNAL_PLATFORM_PATTERN),
				compileOrDefault(config.upfrontPaymentBehaviorPattern, ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
				compileOrDefault(config.accountDataBehaviorPattern, ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
				compileOrDefault(config.middlemanPattern, ScamRulesConfig.DEFAULT_MIDDLEMAN_PATTERN)
			);
		}
	}

	private record RuntimeConfig(
		PatternSet patterns,
		BehaviorPatternSet behaviorPatterns,
		boolean localAiEnabled,
		int localAiMaxScore,
		double localAiTriggerProbability,
		ScamRiskLevel minimumAlertRiskLevel,
		AutoCaptureAlertLevel autoCaptureAlertLevel,
		Set<ScamRule> disabledRules
	) {
		private boolean isEnabled(ScamRule rule) {
			return !disabledRules.contains(rule);
		}

		private String autoCaptureAlertLevelSetting() {
			return autoCaptureAlertLevel.persistedValue();
		}

		private static RuntimeConfig from(ScamRulesConfig config) {
			return new RuntimeConfig(
				PatternSet.from(config),
				BehaviorPatternSet.from(config),
				config.localAiEnabled,
				config.localAiMaxScore,
				config.localAiTriggerProbability,
				parseRiskLevelOrDefault(config.minAlertRiskLevel, ScamRiskLevel.HIGH),
				AutoCaptureAlertLevel.parseOrDefault(config.autoCaptureAlertLevel, AutoCaptureAlertLevel.HIGH),
				parseDisabledRules(config.disabledRules)
			);
		}
	}

	private static Set<ScamRule> parseDisabledRules(Set<String> raw) {
		EnumSet<ScamRule> disabled = EnumSet.noneOf(ScamRule.class);
		if (raw == null || raw.isEmpty()) {
			return disabled;
		}
		for (String item : raw) {
			if (item == null || item.isBlank()) {
				continue;
			}
			try {
				disabled.add(ScamRule.valueOf(item.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return disabled;
	}

	private enum AutoCaptureAlertLevel {
		OFF(null),
		LOW(ScamRiskLevel.LOW),
		MEDIUM(ScamRiskLevel.MEDIUM),
		HIGH(ScamRiskLevel.HIGH),
		CRITICAL(ScamRiskLevel.CRITICAL);

		private final ScamRiskLevel minimumLevel;

		AutoCaptureAlertLevel(ScamRiskLevel minimumLevel) {
			this.minimumLevel = minimumLevel;
		}

		private ScamRiskLevel minimumLevel() {
			return minimumLevel;
		}

		private String persistedValue() {
			return name();
		}

		private static AutoCaptureAlertLevel parse(String raw) {
			if (raw == null || raw.isBlank()) {
				return null;
			}
			try {
				return AutoCaptureAlertLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		}

		private static AutoCaptureAlertLevel parseOrDefault(String raw, AutoCaptureAlertLevel fallback) {
			AutoCaptureAlertLevel parsed = parse(raw);
			return parsed == null ? fallback : parsed;
		}
	}

	private static ScamRiskLevel parseRiskLevelOrDefault(String raw, ScamRiskLevel fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		try {
			return ScamRiskLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	private static Pattern compileOrDefault(String candidate, String fallback) {
		try {
			return Pattern.compile(candidate);
		} catch (PatternSyntaxException ignored) {
			return Pattern.compile(fallback);
		}
	}

	private static String firstMatch(Pattern pattern, String message) {
		Matcher matcher = pattern.matcher(message);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group();
	}

	private static String round(double value) {
		return String.format(Locale.ROOT, "%.3f", value);
	}
}
