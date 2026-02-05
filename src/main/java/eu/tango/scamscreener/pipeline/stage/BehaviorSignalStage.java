package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.List;
import eu.tango.scamscreener.pipeline.core.RuleConfig;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;

public final class BehaviorSignalStage {
	private final RuleConfig ruleConfig;

	/**
	 * Converts {@link BehaviorAnalysis} flags into weighted signals.
	 */
	public BehaviorSignalStage(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	/**
	 * Returns one {@link Signal} per behavior flag that is enabled by rules.
	 */
	public List<Signal> collectSignals(BehaviorAnalysis analysis) {
		if (analysis == null) {
			return List.of();
		}

		List<Signal> signals = new ArrayList<>();
		if (analysis.pushesExternalPlatform() && ruleConfig.isEnabled(ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH)) {
			signals.add(new Signal(
				ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH.name(),
				SignalSource.BEHAVIOR,
				15,
				"Behavior flag pushesExternalPlatform=true (+15)",
				ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH,
				List.of()
			));
		}

		if (analysis.demandsUpfrontPayment() && ruleConfig.isEnabled(ScamRules.ScamRule.UPFRONT_PAYMENT)) {
			signals.add(new Signal(
				ScamRules.ScamRule.UPFRONT_PAYMENT.name(),
				SignalSource.BEHAVIOR,
				25,
				"Behavior flag demandsUpfrontPayment=true (+25)",
				ScamRules.ScamRule.UPFRONT_PAYMENT,
				List.of()
			));
		}

		if (analysis.requestsSensitiveData() && ruleConfig.isEnabled(ScamRules.ScamRule.ACCOUNT_DATA_REQUEST)) {
			signals.add(new Signal(
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST.name(),
				SignalSource.BEHAVIOR,
				35,
				"Behavior flag requestsSensitiveData=true (+35)",
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST,
				List.of()
			));
		}

		if (analysis.claimsTrustedMiddlemanWithoutProof() && ruleConfig.isEnabled(ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM)) {
			signals.add(new Signal(
				ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM.name(),
				SignalSource.BEHAVIOR,
				20,
				"Behavior flag claimsTrustedMiddlemanWithoutProof=true (+20)",
				ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM,
				List.of()
			));
		}

		if (analysis.repeatedContactAttempts() >= 3 && ruleConfig.isEnabled(ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN)) {
			signals.add(new Signal(
				ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN.name(),
				SignalSource.BEHAVIOR,
				10,
				"Repeated contact attempts=" + analysis.repeatedContactAttempts() + " (threshold: 3, +10)",
				ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN,
				List.of()
			));
		}

		return signals;
	}
}
