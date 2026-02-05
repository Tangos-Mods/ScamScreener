package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.rules.ScamRules;

import java.util.Locale;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;

public final class AiScorer {
	private final LocalAiScorer localAiScorer;
	private final RuleConfig ruleConfig;

	/**
	 * Uses the local model to turn a {@link BehaviorAnalysis} into a single AI {@link Signal}.
	 */
	public AiScorer(LocalAiScorer localAiScorer, RuleConfig ruleConfig) {
		this.localAiScorer = localAiScorer;
		this.ruleConfig = ruleConfig;
	}

	/**
	 * Returns {@code null} when the AI is disabled or does not trigger.
	 * See {@link eu.tango.scamscreener.rules.ScamRules#localAiEnabled()} and
	 * {@link eu.tango.scamscreener.rules.ScamRules#localAiTriggerProbability()}.
	 */
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
