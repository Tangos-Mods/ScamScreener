package eu.tango.scamscreener.rules;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.config.ScamRulesConfig;
import lombok.experimental.UtilityClass;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@UtilityClass
public class ScamRules {
	private static final LocalAiScorer LOCAL_AI_SCORER = new LocalAiScorer();
	private static RuntimeConfig config = RuntimeConfig.from(ScamRulesConfig.loadOrCreate());

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

	public static FunnelConfig funnelConfig() {
		return config.funnelConfig();
	}

	public static boolean localAiEnabled() {
		return config.localAiEnabled();
	}

	public static boolean setLocalAiEnabled(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.localAiEnabled = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.localAiEnabled();
	}

	public static boolean uploadTosAccepted() {
		return config.uploadTosAccepted();
	}

	public static boolean setUploadTosAccepted(boolean accepted) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.tos = accepted;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.uploadTosAccepted();
	}

	public static int localAiMaxScore() {
		return config.localAiMaxScore();
	}

	public static double localAiTriggerProbability() {
		return config.localAiTriggerProbability();
	}

	public static int localAiFunnelMaxScore() {
		return config.localAiFunnelMaxScore();
	}

	public static double localAiFunnelThresholdBonus() {
		return config.localAiFunnelThresholdBonus();
	}

	public static String autoCaptureAlertLevelSetting() {
		return config.autoCaptureAlertLevelSetting();
	}

	public static boolean showScamWarningMessage() {
		return config.showScamWarningMessage();
	}

	public static boolean pingOnScamWarning() {
		return config.pingOnScamWarning();
	}

	public static boolean showBlacklistWarningMessage() {
		return config.showBlacklistWarningMessage();
	}

	public static boolean pingOnBlacklistWarning() {
		return config.pingOnBlacklistWarning();
	}

	public static boolean showAutoLeaveMessage() {
		return config.showAutoLeaveMessage();
	}

	public static boolean notifyAiUpToDateOnJoin() {
		return config.notifyAiUpToDateOnJoin();
	}

	public static boolean marketSafetyEnabled() {
		return config.marketSafetyEnabled();
	}

	public static String marketSafetyProfile() {
		return config.marketSafetyProfile().name();
	}

	public static int marketConfirmClicksRequired() {
		return config.marketConfirmClicksRequired();
	}

	public static int marketConfirmWindowSeconds() {
		return config.marketConfirmWindowSeconds();
	}

	public static double marketAhOverbidWarnMultiple() {
		return config.marketAhOverbidWarnMultiple();
	}

	public static double marketAhOverbidBlockMultiple() {
		return config.marketAhOverbidBlockMultiple();
	}

	public static double marketInflatedWarnMultiple30d() {
		return config.marketInflatedWarnMultiple30d();
	}

	public static double marketInflatedSevereMultiple30d() {
		return config.marketInflatedSevereMultiple30d();
	}

	public static double marketNpcWarnMultiple() {
		return config.marketNpcWarnMultiple();
	}

	public static double marketNpcBlockMultiple() {
		return config.marketNpcBlockMultiple();
	}

	public static double marketRareUnderpriceWarnRatio() {
		return config.marketRareUnderpriceWarnRatio();
	}

	public static double marketRareUnderpriceBlockRatio() {
		return config.marketRareUnderpriceBlockRatio();
	}

	public static boolean marketRareTradeProtectionEnabled() {
		return config.marketRareTradeProtectionEnabled();
	}

	public static boolean marketTooltipHighlightEnabled() {
		return config.marketTooltipHighlightEnabled();
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

	public static boolean setShowScamWarningMessage(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.showScamWarningMessage = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.showScamWarningMessage();
	}

	public static boolean setPingOnScamWarning(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.pingOnScamWarning = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.pingOnScamWarning();
	}

	public static boolean setShowBlacklistWarningMessage(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.showBlacklistWarningMessage = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.showBlacklistWarningMessage();
	}

	public static boolean setPingOnBlacklistWarning(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.pingOnBlacklistWarning = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.pingOnBlacklistWarning();
	}

	public static boolean setShowAutoLeaveMessage(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.showAutoLeaveMessage = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.showAutoLeaveMessage();
	}

	public static boolean setNotifyAiUpToDateOnJoin(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.notifyAiUpToDateOnJoin = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.notifyAiUpToDateOnJoin();
	}

	public static boolean setMarketSafetyEnabled(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketSafetyEnabled = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketSafetyEnabled();
	}

	public static String setMarketSafetyProfile(String profile) {
		MarketSafetyProfile parsed = MarketSafetyProfile.parseOrDefault(profile, MarketSafetyProfile.BALANCED);
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketSafetyProfile = parsed.name();
		applyMarketProfileDefaults(cfg, parsed);
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketSafetyProfile().name();
	}

	public static int setMarketConfirmClicksRequired(int value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketConfirmClicksRequired = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketConfirmClicksRequired();
	}

	public static int setMarketConfirmWindowSeconds(int value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketConfirmWindowSeconds = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketConfirmWindowSeconds();
	}

	public static double setMarketAhOverbidWarnMultiple(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketAhOverbidWarnMultiple = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketAhOverbidWarnMultiple();
	}

	public static double setMarketAhOverbidBlockMultiple(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketAhOverbidBlockMultiple = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketAhOverbidBlockMultiple();
	}

	public static double setMarketInflatedWarnMultiple30d(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketInflatedWarnMultiple30d = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketInflatedWarnMultiple30d();
	}

	public static double setMarketInflatedSevereMultiple30d(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketInflatedSevereMultiple30d = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketInflatedSevereMultiple30d();
	}

	public static double setMarketNpcWarnMultiple(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketNpcWarnMultiple = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketNpcWarnMultiple();
	}

	public static double setMarketNpcBlockMultiple(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketNpcBlockMultiple = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketNpcBlockMultiple();
	}

	public static double setMarketRareUnderpriceWarnRatio(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketRareUnderpriceWarnRatio = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketRareUnderpriceWarnRatio();
	}

	public static double setMarketRareUnderpriceBlockRatio(double value) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketRareUnderpriceBlockRatio = value;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketRareUnderpriceBlockRatio();
	}

	public static boolean setMarketRareTradeProtectionEnabled(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketRareTradeProtectionEnabled = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketRareTradeProtectionEnabled();
	}

	public static boolean setMarketTooltipHighlightEnabled(boolean enabled) {
		ScamRulesConfig cfg = ScamRulesConfig.loadOrCreate();
		cfg.marketTooltipHighlightEnabled = enabled;
		ScamRulesConfig.save(cfg);
		reloadConfig();
		return config.marketTooltipHighlightEnabled();
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
		FUNNEL_SEQUENCE_PATTERN,
		SIMILARITY_MATCH,
		LOCAL_AI_RISK_SIGNAL,
		LOCAL_AI_FUNNEL_SIGNAL
	}

	public enum ScamRiskLevel {
		LOW,
		MEDIUM,
		HIGH,
		CRITICAL
	}

	public enum MarketSafetyProfile {
		CONSERVATIVE,
		BALANCED,
		AGGRESSIVE;

		private static MarketSafetyProfile parseOrDefault(String raw, MarketSafetyProfile fallback) {
			if (raw == null || raw.isBlank()) {
				return fallback;
			}
			try {
				return MarketSafetyProfile.valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				return fallback;
			}
		}
	}

	public record BehaviorContext(
		String message,
		String channel,
		long deltaMs,
		boolean pushesExternalPlatform,
		boolean demandsUpfrontPayment,
		boolean requestsSensitiveData,
		boolean claimsTrustedMiddlemanWithoutProof,
		int repeatedContactAttempts,
		boolean tooGoodToBeTrue,
		boolean isSpam,
		boolean asksForStuff,
		boolean advertising,
		boolean intentOffer,
		boolean intentRep,
		boolean intentRedirect,
		boolean intentInstruction,
		boolean intentPaymentUpfront,
		boolean intentCommunityAnchor,
		int funnelStepIndex,
		double funnelSequenceScore,
		boolean funnelFullChain,
		boolean funnelPartialChain,
		int ruleHits,
		int similarityHits,
		int behaviorHits,
		int trendHits,
		int funnelHits
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

	public static record FunnelConfig(
		Pattern serviceOfferPattern,
		Pattern freeOfferPattern,
		Pattern repRequestPattern,
		Pattern platformRedirectPattern,
		Pattern instructionInjectionPattern,
		Pattern communityAnchorPattern,
		Pattern negativeIntentPattern,
		int windowSize,
		long windowMillis,
		long contextTtlMillis,
		int fullSequenceWeight,
		int partialSequenceWeight
	) {
		private static FunnelConfig from(ScamRulesConfig config) {
			return new FunnelConfig(
				compileOrDefault(config.funnelServiceOfferPattern, ScamRulesConfig.DEFAULT_FUNNEL_SERVICE_OFFER_PATTERN),
				compileOrDefault(config.funnelFreeOfferPattern, ScamRulesConfig.DEFAULT_FUNNEL_FREE_OFFER_PATTERN),
				compileOrDefault(config.funnelRepRequestPattern, ScamRulesConfig.DEFAULT_FUNNEL_REP_REQUEST_PATTERN),
				compileOrDefault(config.funnelPlatformRedirectPattern, ScamRulesConfig.DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN),
				compileOrDefault(config.funnelInstructionInjectionPattern, ScamRulesConfig.DEFAULT_FUNNEL_INSTRUCTION_INJECTION_PATTERN),
				compileOrDefault(config.funnelCommunityAnchorPattern, ScamRulesConfig.DEFAULT_FUNNEL_COMMUNITY_ANCHOR_PATTERN),
				compileOrDefault(config.funnelNegativeIntentPattern, ScamRulesConfig.DEFAULT_FUNNEL_NEGATIVE_INTENT_PATTERN),
				config.funnelWindowSize,
				config.funnelWindowMillis,
				config.funnelContextTtlMillis,
				config.funnelFullSequenceWeight,
				config.funnelPartialSequenceWeight
			);
		}
	}

	private record RuntimeConfig(
		PatternSet patterns,
		BehaviorPatternSet behaviorPatterns,
		FunnelConfig funnelConfig,
		boolean localAiEnabled,
		boolean uploadTosAccepted,
		int localAiMaxScore,
		double localAiTriggerProbability,
		int localAiFunnelMaxScore,
		double localAiFunnelThresholdBonus,
		ScamRiskLevel minimumAlertRiskLevel,
		AutoCaptureAlertLevel autoCaptureAlertLevel,
		boolean showScamWarningMessage,
		boolean pingOnScamWarning,
		boolean showBlacklistWarningMessage,
		boolean pingOnBlacklistWarning,
		boolean showAutoLeaveMessage,
		boolean notifyAiUpToDateOnJoin,
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
		boolean marketSafetyEnabled,
		MarketSafetyProfile marketSafetyProfile,
		int marketConfirmClicksRequired,
		int marketConfirmWindowSeconds,
		double marketAhOverbidWarnMultiple,
		double marketAhOverbidBlockMultiple,
		double marketInflatedWarnMultiple30d,
		double marketInflatedSevereMultiple30d,
		double marketNpcWarnMultiple,
		double marketNpcBlockMultiple,
		double marketRareUnderpriceWarnRatio,
		double marketRareUnderpriceBlockRatio,
		boolean marketRareTradeProtectionEnabled,
		boolean marketTooltipHighlightEnabled,
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
				FunnelConfig.from(config),
				config.localAiEnabled,
				Boolean.TRUE.equals(config.tos),
				config.localAiMaxScore,
				config.localAiTriggerProbability,
				config.localAiFunnelMaxScore,
				config.localAiFunnelThresholdBonus,
				parseRiskLevelOrDefault(config.minAlertRiskLevel, ScamRiskLevel.MEDIUM),
				AutoCaptureAlertLevel.parseOrDefault(config.autoCaptureAlertLevel, AutoCaptureAlertLevel.HIGH),
				config.showScamWarningMessage,
				config.pingOnScamWarning,
				config.showBlacklistWarningMessage,
				config.pingOnBlacklistWarning,
				config.showAutoLeaveMessage,
				Boolean.TRUE.equals(config.notifyAiUpToDateOnJoin),
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
				config.marketSafetyEnabled,
				MarketSafetyProfile.parseOrDefault(config.marketSafetyProfile, MarketSafetyProfile.BALANCED),
				config.marketConfirmClicksRequired,
				config.marketConfirmWindowSeconds,
				config.marketAhOverbidWarnMultiple,
				config.marketAhOverbidBlockMultiple,
				config.marketInflatedWarnMultiple30d,
				config.marketInflatedSevereMultiple30d,
				config.marketNpcWarnMultiple,
				config.marketNpcBlockMultiple,
				config.marketRareUnderpriceWarnRatio,
				config.marketRareUnderpriceBlockRatio,
				config.marketRareTradeProtectionEnabled,
				config.marketTooltipHighlightEnabled,
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

	private static void applyMarketProfileDefaults(ScamRulesConfig cfg, MarketSafetyProfile profile) {
		if (cfg == null || profile == null) {
			return;
		}
		switch (profile) {
			case CONSERVATIVE -> {
				cfg.marketAhOverbidWarnMultiple = 2.0;
				cfg.marketAhOverbidBlockMultiple = 3.0;
				cfg.marketInflatedWarnMultiple30d = 2.5;
				cfg.marketInflatedSevereMultiple30d = 4.5;
				cfg.marketNpcWarnMultiple = 15.0;
				cfg.marketNpcBlockMultiple = 60.0;
				cfg.marketRareUnderpriceWarnRatio = 0.75;
				cfg.marketRareUnderpriceBlockRatio = 0.55;
			}
			case BALANCED -> {
				cfg.marketAhOverbidWarnMultiple = ScamRulesConfig.DEFAULT_MARKET_AH_OVERBID_WARN_MULTIPLE;
				cfg.marketAhOverbidBlockMultiple = ScamRulesConfig.DEFAULT_MARKET_AH_OVERBID_BLOCK_MULTIPLE;
				cfg.marketInflatedWarnMultiple30d = ScamRulesConfig.DEFAULT_MARKET_INFLATED_WARN_MULTIPLE_30D;
				cfg.marketInflatedSevereMultiple30d = ScamRulesConfig.DEFAULT_MARKET_INFLATED_SEVERE_MULTIPLE_30D;
				cfg.marketNpcWarnMultiple = ScamRulesConfig.DEFAULT_MARKET_NPC_WARN_MULTIPLE;
				cfg.marketNpcBlockMultiple = ScamRulesConfig.DEFAULT_MARKET_NPC_BLOCK_MULTIPLE;
				cfg.marketRareUnderpriceWarnRatio = ScamRulesConfig.DEFAULT_MARKET_RARE_UNDERPRICE_WARN_RATIO;
				cfg.marketRareUnderpriceBlockRatio = ScamRulesConfig.DEFAULT_MARKET_RARE_UNDERPRICE_BLOCK_RATIO;
			}
			case AGGRESSIVE -> {
				cfg.marketAhOverbidWarnMultiple = 3.0;
				cfg.marketAhOverbidBlockMultiple = 5.0;
				cfg.marketInflatedWarnMultiple30d = 4.0;
				cfg.marketInflatedSevereMultiple30d = 8.0;
				cfg.marketNpcWarnMultiple = 30.0;
				cfg.marketNpcBlockMultiple = 120.0;
				cfg.marketRareUnderpriceWarnRatio = 0.55;
				cfg.marketRareUnderpriceBlockRatio = 0.35;
			}
		}
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

}
