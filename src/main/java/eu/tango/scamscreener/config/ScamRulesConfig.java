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
	public static final String DEFAULT_EXTERNAL_PLATFORM_PATTERN = "\\b(discord|telegram|t\\.me|dm me|add me)\\b";
	public static final String DEFAULT_MIDDLEMAN_PATTERN = "\\b(trusted middleman|legit middleman|middleman)\\b";
	public static final boolean DEFAULT_LOCAL_AI_ENABLED = true;
	public static final int DEFAULT_LOCAL_AI_MAX_SCORE = 22;
	public static final double DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY = 0.56;
	public static final String DEFAULT_MIN_ALERT_RISK_LEVEL = "HIGH";
	public static final String DEFAULT_AUTO_CAPTURE_ALERT_LEVEL = "OFF";
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

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-rules.json");
	private static final Path LEGACY_FILE_PATH = ScamScreenerPaths.inRootConfigDir("scam-screener-rules.json");

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
	public int localAiMaxScore = DEFAULT_LOCAL_AI_MAX_SCORE;
	public double localAiTriggerProbability = DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY;
	public String minAlertRiskLevel = DEFAULT_MIN_ALERT_RISK_LEVEL;
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
	public Set<String> disabledRules = new LinkedHashSet<>();

	public static ScamRulesConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			ScamRulesConfig migrated = loadFromPath(LEGACY_FILE_PATH);
			if (migrated != null) {
				ScamRulesConfig normalized = migrated.withDefaults();
				save(normalized);
				return normalized;
			}

			ScamRulesConfig defaults = new ScamRulesConfig();
			save(defaults);
			return defaults;
		}

		ScamRulesConfig loaded = loadFromPath(FILE_PATH);
		if (loaded == null) {
			return new ScamRulesConfig();
		}
		return loaded.withDefaults();
	}

	private static ScamRulesConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, ScamRulesConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(ScamRulesConfig config) {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
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
		localAiMaxScore = clampInt(localAiMaxScore, 0, 100, DEFAULT_LOCAL_AI_MAX_SCORE);
		localAiTriggerProbability = clampDouble(localAiTriggerProbability, 0.0, 1.0, DEFAULT_LOCAL_AI_TRIGGER_PROBABILITY);
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
}
