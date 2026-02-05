package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.rules.ScamRules;

import java.util.Locale;

public final class AiScorer {
	private final LocalAiScorer localAiScorer;
	private final RuleConfig ruleConfig;

	public AiScorer(LocalAiScorer localAiScorer, RuleConfig ruleConfig) {
		this.localAiScorer = localAiScorer;
		this.ruleConfig = ruleConfig;
	}

	public Signal score(BehaviorAnalysis analysis) {
		if (analysis == null || !ScamRules.localAiEnabled()) {
			return null;
		}
		if (!ruleConfig.isEnabled(ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL)) {
			return null;
		}

		ScamRules.BehaviorContext context = new ScamRules.BehaviorContext(
			analysis.message(),
			analysis.pushesExternalPlatform(),
			analysis.demandsUpfrontPayment(),
			analysis.requestsSensitiveData(),
			analysis.claimsTrustedMiddlemanWithoutProof(),
			analysis.repeatedContactAttempts()
		);
		LocalAiScorer.AiResult result = localAiScorer.score(
			context,
			ScamRules.localAiMaxScore(),
			ScamRules.localAiTriggerProbability()
		);
		if (!result.triggered() || result.score() <= 0) {
			return null;
		}

		String probability = String.format(Locale.ROOT, "%.3f", result.probability());
		String threshold = String.format(Locale.ROOT, "%.3f", ScamRules.localAiTriggerProbability());
		String evidence = "Local AI probability=" + probability + ", threshold=" + threshold + " (+" + result.score() + ")\n" + result.explanation();

		return new Signal(
			ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL.name(),
			SignalSource.AI,
			result.score(),
			evidence,
			ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL,
			java.util.List.of()
		);
	}
}
