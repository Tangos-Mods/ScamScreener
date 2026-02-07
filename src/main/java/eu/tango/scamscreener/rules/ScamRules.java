package eu.tango.scamscreener.rules;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.config.ScamRulesConfig;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@UtilityClass
public class ScamRules {
	// TODO(modularization): extract config parsing/persistence into a dedicated runtime-config service.
	private static final LocalAiScorer LOCAL_AI_SCORER = new LocalAiScorer();
	private static ScamRulesRuntime config = ScamRulesRuntime.fromConfig(ScamRulesConfig.loadOrCreate());

	public static void reloadConfig() {
		config = ScamRulesRuntime.fromConfig(ScamRulesConfig.loadOrCreate());
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

	public static int levelMediumThreshold() {
		return config.levelMediumThreshold();
	}

	public static int levelHighThreshold() {
		return config.levelHighThreshold();
	}

	public static int levelCriticalThreshold() {
		return config.levelCriticalThreshold();
	}

	public static int entropyBonusWeight() {
		return config.entropyBonusWeight();
	}

	public static double similarityRuleThreshold() {
		return config.similarityRuleThreshold();
	}

	public static double similarityTrainingThreshold() {
		return config.similarityTrainingThreshold();
	}

	public static double similarityTrainingMargin() {
		return config.similarityTrainingMargin();
	}

	public static int similarityRuleWeight() {
		return config.similarityRuleWeight();
	}

	public static int similarityTrainingWeight() {
		return config.similarityTrainingWeight();
	}

	public static int similarityMaxTrainingSamples() {
		return config.similarityMaxTrainingSamples();
	}

	public static int similarityMaxCompareLength() {
		return config.similarityMaxCompareLength();
	}

	public static int similarityMinMessageLength() {
		return config.similarityMinMessageLength();
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
		String normalized = ScamRulesRuntime.normalizeAutoCaptureSetting(setting);
		if (normalized == null) {
			return null;
		}

		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.autoCaptureAlertLevel = normalized;
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

	public enum ScamRule {
		SUSPICIOUS_LINK,
		PRESSURE_AND_URGENCY,
		UPFRONT_PAYMENT,
		ACCOUNT_DATA_REQUEST,
		EXTERNAL_PLATFORM_PUSH,
		DISCORD_HANDLE,
		FAKE_MIDDLEMAN_CLAIM,
		TOO_GOOD_TO_BE_TRUE,
		TRUST_MANIPULATION,
		SPAMMY_CONTACT_PATTERN,
		MULTI_MESSAGE_PATTERN,
		SIMILARITY_MATCH,
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

	public static record PatternSet(
		Pattern link,
		Pattern urgency,
		Pattern paymentFirst,
		Pattern accountData,
		Pattern tooGood,
		Pattern trustBait
	) { }

	public static record BehaviorPatternSet(
		Pattern externalPlatform,
		Pattern upfrontPayment,
		Pattern accountData,
		Pattern middlemanClaim
	) { }

	private record ScamRulesRuntime(
		PatternSet patterns,
		BehaviorPatternSet behaviorPatterns,
		boolean localAiEnabled,
		int localAiMaxScore,
		double localAiTriggerProbability,
		ScamRiskLevel minimumAlertRiskLevel,
		AutoCaptureAlertLevel autoCaptureAlertLevel,
		int levelMediumThreshold,
		int levelHighThreshold,
		int levelCriticalThreshold,
		int entropyBonusWeight,
		double similarityRuleThreshold,
		double similarityTrainingThreshold,
		double similarityTrainingMargin,
		int similarityRuleWeight,
		int similarityTrainingWeight,
		int similarityMaxTrainingSamples,
		int similarityMaxCompareLength,
		int similarityMinMessageLength,
		Set<ScamRule> disabledRules
	) {
		ScamRulesRuntime {
			disabledRules = disabledRules == null ? EnumSet.noneOf(ScamRule.class) : EnumSet.copyOf(disabledRules);
		}

		private static ScamRulesRuntime fromConfig(ScamRulesConfig config) {
			return new ScamRulesRuntime(
				new PatternSet(
					compileOrDefault(config.linkPattern, ScamRulesConfig.DEFAULT_LINK_PATTERN),
					compileOrDefault(config.urgencyPattern, ScamRulesConfig.DEFAULT_URGENCY_PATTERN),
					compileOrDefault(config.paymentFirstPattern, ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
					compileOrDefault(config.accountDataPattern, ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
					compileOrDefault(config.tooGoodPattern, ScamRulesConfig.DEFAULT_TOO_GOOD_PATTERN),
					compileOrDefault(config.trustBaitPattern, ScamRulesConfig.DEFAULT_TRUST_BAIT_PATTERN)
				),
				new BehaviorPatternSet(
					compileOrDefault(config.externalPlatformPattern, ScamRulesConfig.DEFAULT_EXTERNAL_PLATFORM_PATTERN),
					compileOrDefault(config.upfrontPaymentBehaviorPattern, ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
					compileOrDefault(config.accountDataBehaviorPattern, ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
					compileOrDefault(config.middlemanPattern, ScamRulesConfig.DEFAULT_MIDDLEMAN_PATTERN)
				),
				config.localAiEnabled,
				config.localAiMaxScore,
				config.localAiTriggerProbability,
				parseRiskLevelOrDefault(config.minAlertRiskLevel, ScamRiskLevel.HIGH),
				AutoCaptureAlertLevel.parseOrDefault(config.autoCaptureAlertLevel, AutoCaptureAlertLevel.OFF),
				config.levelMedium,
				config.levelHigh,
				config.levelCritical,
				config.entropyBonusWeight,
				config.similarityRuleThreshold,
				config.similarityTrainingThreshold,
				config.similarityTrainingMargin,
				config.similarityRuleWeight,
				config.similarityTrainingWeight,
				config.similarityMaxTrainingSamples,
				config.similarityMaxCompareLength,
				config.similarityMinMessageLength,
				parseDisabledRules(config.disabledRules)
			);
		}

		private static String normalizeAutoCaptureSetting(String raw) {
			AutoCaptureAlertLevel parsed = AutoCaptureAlertLevel.parse(raw);
			return parsed == null ? null : parsed.persistedValue();
		}

		private boolean isEnabled(ScamRule rule) {
			return !disabledRules.contains(rule);
		}

		private String autoCaptureAlertLevelSetting() {
			return autoCaptureAlertLevel.persistedValue();
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

		private enum AutoCaptureAlertLevel {
			OFF,
			LOW,
			MEDIUM,
			HIGH,
			CRITICAL;

			private ScamRiskLevel minimumLevel() {
				return switch (this) {
					case OFF, LOW -> ScamRiskLevel.LOW;
					case MEDIUM -> ScamRiskLevel.MEDIUM;
					case HIGH -> ScamRiskLevel.HIGH;
					case CRITICAL -> ScamRiskLevel.CRITICAL;
				};
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
	}

}

