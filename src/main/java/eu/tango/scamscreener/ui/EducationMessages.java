package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.config.EducationConfig;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EducationMessages {
	public static final String EXTERNAL_PLATFORM_REDIRECT_ID = "external_platform_redirect";
	public static final String SUSPICIOUS_LINK_ID = "suspicious_link";
	public static final String UPFRONT_PAYMENT_ID = "upfront_payment";
	public static final String ACCOUNT_DATA_REQUEST_ID = "account_data_request";
	public static final String FAKE_MIDDLEMAN_CLAIM_ID = "fake_middleman_claim";
	public static final String PRESSURE_AND_URGENCY_ID = "pressure_and_urgency";
	public static final String TRUST_MANIPULATION_ID = "trust_manipulation";
	public static final String TOO_GOOD_TO_BE_TRUE_ID = "too_good_to_be_true";
	public static final String DISCORD_HANDLE_ID = "discord_handle";
	public static final String FUNNEL_SEQUENCE_PATTERN_ID = "funnel_sequence_pattern";

	private static final List<ScamRules.ScamRule> PRIORITY_RULES = List.of(
		ScamRules.ScamRule.ACCOUNT_DATA_REQUEST,
		ScamRules.ScamRule.UPFRONT_PAYMENT,
		ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM,
		ScamRules.ScamRule.SUSPICIOUS_LINK,
		ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH,
		ScamRules.ScamRule.DISCORD_HANDLE,
		ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN,
		ScamRules.ScamRule.TRUST_MANIPULATION,
		ScamRules.ScamRule.TOO_GOOD_TO_BE_TRUE,
		ScamRules.ScamRule.PRESSURE_AND_URGENCY
	);

	private static final Set<String> KNOWN_IDS = knownIds();

	private EducationMessages() {
	}

	public static Component followUpFor(DetectionResult result) {
		if (result == null || result.triggeredRules().isEmpty()) {
			return null;
		}

		for (ScamRules.ScamRule rule : PRIORITY_RULES) {
			if (!result.triggeredRules().containsKey(rule)) {
				continue;
			}
			Component followUp = followUpForRule(rule);
			if (followUp != null) {
				return followUp;
			}
		}
		return null;
	}

	public static int disableMessage(String messageId) {
		String normalized = normalize(messageId);
		if (!KNOWN_IDS.contains(normalized)) {
			MessageDispatcher.reply(Messages.educationMessageUnknown(messageId));
			return 0;
		}
		EducationConfig config = EducationConfig.loadOrCreate();
		config.disable(normalized);
		EducationConfig.save(config);
		MessageDispatcher.reply(Messages.educationMessageDisabled(normalized));
		return 1;
	}

	public static Set<String> knownMessageIds() {
		return KNOWN_IDS;
	}

	private static boolean isDisabled(String messageId) {
		EducationConfig config = EducationConfig.loadOrCreate();
		return config.isDisabled(messageId);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static Component followUpForRule(ScamRules.ScamRule rule) {
		String messageId = messageIdFor(rule);
		if (messageId == null || isDisabled(messageId)) {
			return null;
		}
		String disableCommand = "/scamscreener edu disable " + messageId;
		return switch (rule) {
			case EXTERNAL_PLATFORM_PUSH -> Messages.educationExternalPlatformWarning(disableCommand);
			case SUSPICIOUS_LINK -> Messages.educationSuspiciousLinkWarning(disableCommand);
			case UPFRONT_PAYMENT -> Messages.educationUpfrontPaymentWarning(disableCommand);
			case ACCOUNT_DATA_REQUEST -> Messages.educationAccountDataWarning(disableCommand);
			case FAKE_MIDDLEMAN_CLAIM -> Messages.educationFakeMiddlemanWarning(disableCommand);
			case PRESSURE_AND_URGENCY -> Messages.educationUrgencyWarning(disableCommand);
			case TRUST_MANIPULATION -> Messages.educationTrustManipulationWarning(disableCommand);
			case TOO_GOOD_TO_BE_TRUE -> Messages.educationTooGoodToBeTrueWarning(disableCommand);
			case DISCORD_HANDLE -> Messages.educationDiscordHandleWarning(disableCommand);
			case FUNNEL_SEQUENCE_PATTERN -> Messages.educationFunnelSequenceWarning(disableCommand);
			default -> null;
		};
	}

	private static String messageIdFor(ScamRules.ScamRule rule) {
		if (rule == null) {
			return null;
		}
		return switch (rule) {
			case EXTERNAL_PLATFORM_PUSH -> EXTERNAL_PLATFORM_REDIRECT_ID;
			case SUSPICIOUS_LINK -> SUSPICIOUS_LINK_ID;
			case UPFRONT_PAYMENT -> UPFRONT_PAYMENT_ID;
			case ACCOUNT_DATA_REQUEST -> ACCOUNT_DATA_REQUEST_ID;
			case FAKE_MIDDLEMAN_CLAIM -> FAKE_MIDDLEMAN_CLAIM_ID;
			case PRESSURE_AND_URGENCY -> PRESSURE_AND_URGENCY_ID;
			case TRUST_MANIPULATION -> TRUST_MANIPULATION_ID;
			case TOO_GOOD_TO_BE_TRUE -> TOO_GOOD_TO_BE_TRUE_ID;
			case DISCORD_HANDLE -> DISCORD_HANDLE_ID;
			case FUNNEL_SEQUENCE_PATTERN -> FUNNEL_SEQUENCE_PATTERN_ID;
			default -> null;
		};
	}

	private static Set<String> knownIds() {
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		ids.add(EXTERNAL_PLATFORM_REDIRECT_ID);
		ids.add(SUSPICIOUS_LINK_ID);
		ids.add(UPFRONT_PAYMENT_ID);
		ids.add(ACCOUNT_DATA_REQUEST_ID);
		ids.add(FAKE_MIDDLEMAN_CLAIM_ID);
		ids.add(PRESSURE_AND_URGENCY_ID);
		ids.add(TRUST_MANIPULATION_ID);
		ids.add(TOO_GOOD_TO_BE_TRUE_ID);
		ids.add(DISCORD_HANDLE_ID);
		ids.add(FUNNEL_SEQUENCE_PATTERN_ID);
		return Collections.unmodifiableSet(ids);
	}
}
