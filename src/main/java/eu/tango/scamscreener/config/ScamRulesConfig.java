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
	public static final String DEFAULT_AUTO_CAPTURE_ALERT_LEVEL = "HIGH";

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
		if (isBlank(minAlertRiskLevel)) {
			minAlertRiskLevel = DEFAULT_MIN_ALERT_RISK_LEVEL;
		}
		if (isBlank(autoCaptureAlertLevel)) {
			autoCaptureAlertLevel = DEFAULT_AUTO_CAPTURE_ALERT_LEVEL;
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
