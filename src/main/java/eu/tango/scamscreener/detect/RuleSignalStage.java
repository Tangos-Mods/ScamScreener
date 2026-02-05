package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuleSignalStage {
	private final RuleConfig ruleConfig;

	public RuleSignalStage(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	public List<Signal> collectSignals(MessageEvent event) {
		if (event == null || event.normalizedMessage().isBlank()) {
			return List.of();
		}

		ScamRules.PatternSet patterns = ruleConfig.patterns();
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

		String urgencyMatch = firstMatch(patterns.urgency(), message);
		if (urgencyMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.PRESSURE_AND_URGENCY)) {
			signals.add(new Signal(
				ScamRules.ScamRule.PRESSURE_AND_URGENCY.name(),
				SignalSource.RULE,
				15,
				"Matched urgency wording: \"" + urgencyMatch + "\" (+15)",
				ScamRules.ScamRule.PRESSURE_AND_URGENCY,
				List.of()
			));
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

		String trustMatch = firstMatch(patterns.trustBait(), message);
		if (trustMatch != null && ruleConfig.isEnabled(ScamRules.ScamRule.TRUST_MANIPULATION)) {
			signals.add(new Signal(
				ScamRules.ScamRule.TRUST_MANIPULATION.name(),
				SignalSource.RULE,
				10,
				"Matched trust-bait wording: \"" + trustMatch + "\" (+10)",
				ScamRules.ScamRule.TRUST_MANIPULATION,
				List.of()
			));
		}

		return signals;
	}

	private static String firstMatch(Pattern pattern, String message) {
		Matcher matcher = pattern.matcher(message);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group();
	}
}
