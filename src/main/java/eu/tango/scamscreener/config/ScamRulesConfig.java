package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ScamRulesConfig {
	/**
	 * Warning: These thresholds and weights influence core detection behavior.
	 * Changing them without care can destabilize or degrade the mod's results.
	 */
	public static final String DEFAULT_LINK_PATTERN = "(https?://|www\\.|discord\\.gg/|t\\.me/)";
	public static final String DEFAULT_URGENCY_PATTERN = "\\b(now|quick|fast|urgent|sofort|jetzt)\\b";
	public static final String DEFAULT_PAYMENT_FIRST_PATTERN = "\\b(pay first|first payment|vorkasse|send first)\\b";
	public static final String DEFAULT_ACCOUNT_DATA_PATTERN = "\\b(password|passwort|2fa|code|email login)\\b";
	public static final String DEFAULT_TOO_GOOD_PATTERN = "\\b(free coins|free rank|dupe|100% safe|garantiert)\\b";
	public static final String DEFAULT_TRUST_BAIT_PATTERN = "\\b(trust me|vertrau mir|legit)\\b";
	public static final String LEGACY_EXTERNAL_PLATFORM_PATTERN = "\\b(discord|telegram|t\\.me|dm me|add me)\\b";
	public static final String DEFAULT_EXTERNAL_PLATFORM_PATTERN = "\\b(discord|telegram|t\\.me|dm me|add me|vc|voice chat|voice channel|call)\\b";
	public static final String DEFAULT_MIDDLEMAN_PATTERN = "\\b(trusted middleman|legit middleman|middleman)\\b";
	public static final boolean DEFAULT_LOCAL_AI_ENABLED = true;
	public static final boolean DEFAULT_UPLOAD_TOS_ACCEPTED = false;
	public static final int DEFAULT_LOCAL_AI_MAX_SCORE = 22;
	public static final double DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY = 0.620;
	public static final int DEFAULT_LOCAL_AI_FUNNEL_MAX_SCORE = 30;
	public static final double DEFAULT_LOCAL_AI_FUNNEL_THRESHOLD_BONUS = 0.05;
	public static final String DEFAULT_MIN_ALERT_RISK_LEVEL = "MEDIUM";
	public static final String DEFAULT_AUTO_CAPTURE_ALERT_LEVEL = "HIGH";
	public static final boolean DEFAULT_ALERT_THRESHOLD_MEDIUM_MIGRATION_DONE = false;
	public static final boolean DEFAULT_AUTO_LEAVE_ON_BLACKLIST = false;
	public static final boolean DEFAULT_SHOW_SCAM_WARNING_MESSAGE = true;
	public static final boolean DEFAULT_PING_ON_SCAM_WARNING = true;
	public static final boolean DEFAULT_SHOW_BLACKLIST_WARNING_MESSAGE = true;
	public static final boolean DEFAULT_PING_ON_BLACKLIST_WARNING = true;
	public static final boolean DEFAULT_SHOW_AUTO_LEAVE_MESSAGE = true;
	public static final boolean DEFAULT_NOTIFY_AI_UP_TO_DATE_ON_JOIN = true;
	public static final int DEFAULT_LEVEL_MEDIUM = 20;
	public static final int DEFAULT_LEVEL_HIGH = 40;
	public static final int DEFAULT_LEVEL_CRITICAL = 70;
	public static final int DEFAULT_ENTROPY_BONUS_WEIGHT = -3;
	public static final double DEFAULT_SIMILARITY_RULE_THRESHOLD = 0.87;
	public static final double DEFAULT_SIMILARITY_TRAINING_THRESHOLD = 0.88;
	public static final double DEFAULT_SIMILARITY_TRAINING_MARGIN = 0.04;
	public static final int DEFAULT_SIMILARITY_RULE_WEIGHT = 8;
	public static final int DEFAULT_SIMILARITY_TRAINING_WEIGHT = 14;
	public static final int DEFAULT_SIMILARITY_MAX_TRAINING_SAMPLES = 250;
	public static final int DEFAULT_SIMILARITY_MAX_COMPARE_LENGTH = 160;
	public static final int DEFAULT_SIMILARITY_MIN_MESSAGE_LENGTH = 6;
	public static final int DEFAULT_CAPTURED_CHAT_CACHE_SIZE = 1000;
	public static final String DEFAULT_FUNNEL_SERVICE_OFFER_PATTERN = "\\b(carry|service|offer|offering|sell|selling|helping)\\b";
	public static final String DEFAULT_FUNNEL_FREE_OFFER_PATTERN = "\\b(free|for free|giveaway|free carry)\\b";
	public static final String DEFAULT_FUNNEL_REP_REQUEST_PATTERN = "\\b(rep|reputation|vouch|voucher|feedback|rep me|vouch me)\\b";
	public static final String LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN = "\\b(discord|telegram|t\\.me|vc|voice chat|call|join vc)\\b";
	public static final String LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN_V2 = "\\b(discord|telegram|t\\.me|vc|voice chat|call|join vc|(?:go to|join) [a-z0-9 ]{2,40} channel)\\b";
	public static final String DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN = "\\b(discord|telegram|t\\.me|vc|voice chat|voice channel|call|join vc|(?:go to|join) [a-z0-9 ]{2,40} channel)\\b";
	public static final String DEFAULT_FUNNEL_INSTRUCTION_INJECTION_PATTERN = "\\b(go to|type|do rep|copy this|run this|use command|join and)\\b";
	public static final String DEFAULT_FUNNEL_COMMUNITY_ANCHOR_PATTERN = "\\b(sbz|hsb|sbm|skyblockz|hypixel skyblock)\\b";
	public static final String DEFAULT_FUNNEL_NEGATIVE_INTENT_PATTERN = "\\b(guild recruit|guild req|guild only|looking for members|lf members|recruiting)\\b";
	public static final int DEFAULT_FUNNEL_WINDOW_SIZE = 20;
	public static final long DEFAULT_FUNNEL_WINDOW_MILLIS = 180_000L;
	public static final long DEFAULT_FUNNEL_CONTEXT_TTL_MILLIS = 600_000L;
	public static final int DEFAULT_FUNNEL_FULL_SEQUENCE_WEIGHT = 28;
	public static final int DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT = 14;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public String linkPattern = DEFAULT_LINK_PATTERN;
	public String urgencyPattern = DEFAULT_URGENCY_PATTERN;
	public String paymentFirstPattern = DEFAULT_PAYMENT_FIRST_PATTERN;
	public String accountDataPattern = DEFAULT_ACCOUNT_DATA_PATTERN;
	public String tooGoodPattern = DEFAULT_TOO_GOOD_PATTERN;
	public String trustBaitPattern = DEFAULT_TRUST_BAIT_PATTERN;
	public String externalPlatformPattern = DEFAULT_EXTERNAL_PLATFORM_PATTERN;
	public String upfrontPaymentBehaviorPattern = DEFAULT_PAYMENT_FIRST_PATTERN;
	public String accountDataBehaviorPattern = DEFAULT_ACCOUNT_DATA_PATTERN;
	public String middlemanPattern = DEFAULT_MIDDLEMAN_PATTERN;
	public boolean localAiEnabled = DEFAULT_LOCAL_AI_ENABLED;
	public Boolean tos = DEFAULT_UPLOAD_TOS_ACCEPTED;
	public int localAiMaxScore = DEFAULT_LOCAL_AI_MAX_SCORE;
	public double localAiTriggerProbability = DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY;
	public Integer localAiFunnelMaxScore = DEFAULT_LOCAL_AI_FUNNEL_MAX_SCORE;
	public Double localAiFunnelThresholdBonus = DEFAULT_LOCAL_AI_FUNNEL_THRESHOLD_BONUS;
	public String minAlertRiskLevel = DEFAULT_MIN_ALERT_RISK_LEVEL;
	public Boolean alertThresholdMediumMigrationDone = DEFAULT_ALERT_THRESHOLD_MEDIUM_MIGRATION_DONE;
	public String autoCaptureAlertLevel = DEFAULT_AUTO_CAPTURE_ALERT_LEVEL;
	public boolean autoLeaveOnBlacklist = DEFAULT_AUTO_LEAVE_ON_BLACKLIST;
	public boolean showScamWarningMessage = DEFAULT_SHOW_SCAM_WARNING_MESSAGE;
	public boolean pingOnScamWarning = DEFAULT_PING_ON_SCAM_WARNING;
	public boolean showBlacklistWarningMessage = DEFAULT_SHOW_BLACKLIST_WARNING_MESSAGE;
	public boolean pingOnBlacklistWarning = DEFAULT_PING_ON_BLACKLIST_WARNING;
	public boolean showAutoLeaveMessage = DEFAULT_SHOW_AUTO_LEAVE_MESSAGE;
	public Boolean notifyAiUpToDateOnJoin = DEFAULT_NOTIFY_AI_UP_TO_DATE_ON_JOIN;
	public int levelMedium = DEFAULT_LEVEL_MEDIUM;
	public int levelHigh = DEFAULT_LEVEL_HIGH;
	public int levelCritical = DEFAULT_LEVEL_CRITICAL;
	public int entropyBonusWeight = DEFAULT_ENTROPY_BONUS_WEIGHT;
	public double similarityRuleThreshold = DEFAULT_SIMILARITY_RULE_THRESHOLD;
	public double similarityTrainingThreshold = DEFAULT_SIMILARITY_TRAINING_THRESHOLD;
	public double similarityTrainingMargin = DEFAULT_SIMILARITY_TRAINING_MARGIN;
	public int similarityRuleWeight = DEFAULT_SIMILARITY_RULE_WEIGHT;
	public int similarityTrainingWeight = DEFAULT_SIMILARITY_TRAINING_WEIGHT;
	public int similarityMaxTrainingSamples = DEFAULT_SIMILARITY_MAX_TRAINING_SAMPLES;
	public int similarityMaxCompareLength = DEFAULT_SIMILARITY_MAX_COMPARE_LENGTH;
	public int similarityMinMessageLength = DEFAULT_SIMILARITY_MIN_MESSAGE_LENGTH;
	public int capturedChatCacheSize = DEFAULT_CAPTURED_CHAT_CACHE_SIZE;
	public String funnelServiceOfferPattern = DEFAULT_FUNNEL_SERVICE_OFFER_PATTERN;
	public String funnelFreeOfferPattern = DEFAULT_FUNNEL_FREE_OFFER_PATTERN;
	public String funnelRepRequestPattern = DEFAULT_FUNNEL_REP_REQUEST_PATTERN;
	public String funnelPlatformRedirectPattern = DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN;
	public String funnelInstructionInjectionPattern = DEFAULT_FUNNEL_INSTRUCTION_INJECTION_PATTERN;
	public String funnelCommunityAnchorPattern = DEFAULT_FUNNEL_COMMUNITY_ANCHOR_PATTERN;
	public String funnelNegativeIntentPattern = DEFAULT_FUNNEL_NEGATIVE_INTENT_PATTERN;
	public int funnelWindowSize = DEFAULT_FUNNEL_WINDOW_SIZE;
	public long funnelWindowMillis = DEFAULT_FUNNEL_WINDOW_MILLIS;
	public long funnelContextTtlMillis = DEFAULT_FUNNEL_CONTEXT_TTL_MILLIS;
	public int funnelFullSequenceWeight = DEFAULT_FUNNEL_FULL_SEQUENCE_WEIGHT;
	public int funnelPartialSequenceWeight = DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT;
	public Set<String> disabledRules = new LinkedHashSet<>();

	public static ScamRulesConfig loadOrCreate() {
		Path rulesPath = filePath();
		if (!Files.exists(rulesPath)) {
			ScamRulesConfig migrated = loadFromPath(legacyFilePath());
			if (migrated != null) {
				ScamRulesConfig normalized = migrated.withDefaults();
				save(normalized);
				return normalized;
			}

			ScamRulesConfig defaults = new ScamRulesConfig();
			defaults.alertThresholdMediumMigrationDone = true;
			save(defaults);
			return defaults;
		}

		ScamRulesConfig loaded = loadFromPath(rulesPath);
		if (loaded == null) {
			ScamRulesConfig fallback = new ScamRulesConfig();
			fallback.alertThresholdMediumMigrationDone = true;
			return fallback;
		}
		String previousMinAlertRiskLevel = loaded.minAlertRiskLevel;
		Boolean previousAlertThresholdMigrationState = loaded.alertThresholdMediumMigrationDone;
		int previousCapturedChatCacheSize = loaded.capturedChatCacheSize;
		ScamRulesConfig normalized = loaded.withDefaults();
		if (!Objects.equals(previousMinAlertRiskLevel, normalized.minAlertRiskLevel)
			|| !Objects.equals(previousAlertThresholdMigrationState, normalized.alertThresholdMediumMigrationDone)
			|| previousCapturedChatCacheSize != normalized.capturedChatCacheSize) {
			save(normalized);
		}
		return normalized;
	}

	private static ScamRulesConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, ScamRulesConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(ScamRulesConfig config) {
		Path rulesPath = filePath();
		try {
			Files.createDirectories(rulesPath.getParent());
			try (Writer writer = Files.newBufferedWriter(rulesPath, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static Path filePath() {
		return ScamScreenerPaths.inModConfigDir("scam-screener-rules.json");
	}

	private static Path legacyFilePath() {
		return ScamScreenerPaths.inRootConfigDir("scam-screener-rules.json");
	}

	private ScamRulesConfig withDefaults() {
		if (isBlank(linkPattern)) {
			linkPattern = DEFAULT_LINK_PATTERN;
		}
		if (isBlank(urgencyPattern)) {
			urgencyPattern = DEFAULT_URGENCY_PATTERN;
		}
		if (isBlank(paymentFirstPattern)) {
			paymentFirstPattern = DEFAULT_PAYMENT_FIRST_PATTERN;
		}
		if (isBlank(accountDataPattern)) {
			accountDataPattern = DEFAULT_ACCOUNT_DATA_PATTERN;
		}
		if (isBlank(tooGoodPattern)) {
			tooGoodPattern = DEFAULT_TOO_GOOD_PATTERN;
		}
		if (isBlank(trustBaitPattern)) {
			trustBaitPattern = DEFAULT_TRUST_BAIT_PATTERN;
		}
		if (isBlank(externalPlatformPattern)) {
			externalPlatformPattern = DEFAULT_EXTERNAL_PLATFORM_PATTERN;
		} else if (LEGACY_EXTERNAL_PLATFORM_PATTERN.equals(externalPlatformPattern)) {
			externalPlatformPattern = DEFAULT_EXTERNAL_PLATFORM_PATTERN;
		}
		if (isBlank(upfrontPaymentBehaviorPattern)) {
			upfrontPaymentBehaviorPattern = DEFAULT_PAYMENT_FIRST_PATTERN;
		}
		if (isBlank(accountDataBehaviorPattern)) {
			accountDataBehaviorPattern = DEFAULT_ACCOUNT_DATA_PATTERN;
		}
		if (isBlank(middlemanPattern)) {
			middlemanPattern = DEFAULT_MIDDLEMAN_PATTERN;
		}
		if (tos == null) {
			tos = DEFAULT_UPLOAD_TOS_ACCEPTED;
		}
		if (isBlank(funnelServiceOfferPattern)) {
			funnelServiceOfferPattern = DEFAULT_FUNNEL_SERVICE_OFFER_PATTERN;
		}
		if (isBlank(funnelFreeOfferPattern)) {
			funnelFreeOfferPattern = DEFAULT_FUNNEL_FREE_OFFER_PATTERN;
		}
		if (isBlank(funnelRepRequestPattern)) {
			funnelRepRequestPattern = DEFAULT_FUNNEL_REP_REQUEST_PATTERN;
		}
		if (isBlank(funnelPlatformRedirectPattern)) {
			funnelPlatformRedirectPattern = DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN;
		} else if (LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN.equals(funnelPlatformRedirectPattern)
			|| LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN_V2.equals(funnelPlatformRedirectPattern)) {
			funnelPlatformRedirectPattern = DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN;
		}
		if (isBlank(funnelInstructionInjectionPattern)) {
			funnelInstructionInjectionPattern = DEFAULT_FUNNEL_INSTRUCTION_INJECTION_PATTERN;
		}
		if (isBlank(funnelCommunityAnchorPattern)) {
			funnelCommunityAnchorPattern = DEFAULT_FUNNEL_COMMUNITY_ANCHOR_PATTERN;
		}
		if (isBlank(funnelNegativeIntentPattern)) {
			funnelNegativeIntentPattern = DEFAULT_FUNNEL_NEGATIVE_INTENT_PATTERN;
		}
		localAiMaxScore = clampInt(localAiMaxScore, 0, 100, DEFAULT_LOCAL_AI_MAX_SCORE);
		localAiTriggerProbability = clampDouble(localAiTriggerProbability, 0.0, 1.0, DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY);
		if (localAiFunnelMaxScore == null) {
			localAiFunnelMaxScore = DEFAULT_LOCAL_AI_FUNNEL_MAX_SCORE;
		}
		if (localAiFunnelThresholdBonus == null) {
			localAiFunnelThresholdBonus = DEFAULT_LOCAL_AI_FUNNEL_THRESHOLD_BONUS;
		}
		localAiFunnelMaxScore = clampInt(localAiFunnelMaxScore, 0, 100, DEFAULT_LOCAL_AI_FUNNEL_MAX_SCORE);
		localAiFunnelThresholdBonus = clampDouble(localAiFunnelThresholdBonus, 0.0, 0.5, DEFAULT_LOCAL_AI_FUNNEL_THRESHOLD_BONUS);
		levelMedium = clampInt(levelMedium, 1, 100, DEFAULT_LEVEL_MEDIUM);
		levelHigh = clampInt(levelHigh, levelMedium + 1, 100, DEFAULT_LEVEL_HIGH);
		levelCritical = clampInt(levelCritical, levelHigh + 1, 100, DEFAULT_LEVEL_CRITICAL);
		entropyBonusWeight = clampInt(entropyBonusWeight, -10, 0, DEFAULT_ENTROPY_BONUS_WEIGHT);
		similarityRuleThreshold = clampDouble(similarityRuleThreshold, 0.0, 1.0, DEFAULT_SIMILARITY_RULE_THRESHOLD);
		similarityTrainingThreshold = clampDouble(similarityTrainingThreshold, 0.0, 1.0, DEFAULT_SIMILARITY_TRAINING_THRESHOLD);
		similarityTrainingMargin = clampDouble(similarityTrainingMargin, 0.0, 1.0, DEFAULT_SIMILARITY_TRAINING_MARGIN);
		similarityRuleWeight = clampInt(similarityRuleWeight, 0, 100, DEFAULT_SIMILARITY_RULE_WEIGHT);
		similarityTrainingWeight = clampInt(similarityTrainingWeight, 0, 100, DEFAULT_SIMILARITY_TRAINING_WEIGHT);
		similarityMaxTrainingSamples = clampInt(similarityMaxTrainingSamples, 10, 2000, DEFAULT_SIMILARITY_MAX_TRAINING_SAMPLES);
		similarityMaxCompareLength = clampInt(similarityMaxCompareLength, 40, 400, DEFAULT_SIMILARITY_MAX_COMPARE_LENGTH);
		similarityMinMessageLength = clampInt(similarityMinMessageLength, 2, 40, DEFAULT_SIMILARITY_MIN_MESSAGE_LENGTH);
		capturedChatCacheSize = clampInt(capturedChatCacheSize, 50, 20_000, DEFAULT_CAPTURED_CHAT_CACHE_SIZE);
		funnelWindowSize = clampInt(funnelWindowSize, 5, 60, DEFAULT_FUNNEL_WINDOW_SIZE);
		funnelWindowMillis = clampLong(funnelWindowMillis, 15_000L, 900_000L, DEFAULT_FUNNEL_WINDOW_MILLIS);
		funnelContextTtlMillis = clampLong(funnelContextTtlMillis, 60_000L, 7_200_000L, DEFAULT_FUNNEL_CONTEXT_TTL_MILLIS);
		funnelFullSequenceWeight = clampInt(funnelFullSequenceWeight, 1, 100, DEFAULT_FUNNEL_FULL_SEQUENCE_WEIGHT);
		funnelPartialSequenceWeight = clampInt(funnelPartialSequenceWeight, 1, 100, DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT);
		if (!Boolean.TRUE.equals(alertThresholdMediumMigrationDone)) {
			minAlertRiskLevel = DEFAULT_MIN_ALERT_RISK_LEVEL;
			alertThresholdMediumMigrationDone = true;
		}
		if (isBlank(minAlertRiskLevel)) {
			minAlertRiskLevel = DEFAULT_MIN_ALERT_RISK_LEVEL;
		}
		if (isBlank(autoCaptureAlertLevel)) {
			autoCaptureAlertLevel = DEFAULT_AUTO_CAPTURE_ALERT_LEVEL;
		}
		if (notifyAiUpToDateOnJoin == null) {
			notifyAiUpToDateOnJoin = DEFAULT_NOTIFY_AI_UP_TO_DATE_ON_JOIN;
		}
		if (disabledRules == null) {
			disabledRules = new LinkedHashSet<>();
		} else {
			Set<String> normalized = new LinkedHashSet<>();
			for (String rule : disabledRules) {
				if (rule == null || rule.isBlank()) {
					continue;
				}
				normalized.add(rule.trim().toUpperCase(Locale.ROOT));
			}
			disabledRules = normalized;
		}
		if (disabledRules.isEmpty()) {
			disabledRules.add("SPAMMY_CONTACT_PATTERN");
		}
		return this;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static int clampInt(int value, int min, int max, int fallback) {
		if (value < min || value > max) {
			return fallback;
		}
		return value;
	}

	private static double clampDouble(double value, double min, double max, double fallback) {
		if (Double.isNaN(value) || value < min || value > max) {
			return fallback;
		}
		return value;
	}

	private static long clampLong(long value, long min, long max, long fallback) {
		if (value < min || value > max) {
			return fallback;
		}
		return value;
	}
}
