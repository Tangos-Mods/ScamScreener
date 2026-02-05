package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.List;

public final class TrendSignalStage {
	private final RuleConfig ruleConfig;
	private final TrendStore trendStore;

	public TrendSignalStage(RuleConfig ruleConfig, TrendStore trendStore) {
		this.ruleConfig = ruleConfig;
		this.trendStore = trendStore;
	}

	public List<Signal> collectSignals(MessageEvent event, List<Signal> existingSignals) {
		if (!ruleConfig.isEnabled(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN)) {
			trendStore.evaluate(event, existingSignals);
			return List.of();
		}

		TrendStore.TrendEvaluation evaluation = trendStore.evaluate(event, existingSignals);
		if (evaluation.detail() == null) {
			return List.of();
		}

		return List.of(new Signal(
			ScamRules.ScamRule.MULTI_MESSAGE_PATTERN.name(),
			SignalSource.TREND,
			evaluation.bonusScore(),
			evaluation.detail(),
			ScamRules.ScamRule.MULTI_MESSAGE_PATTERN,
			evaluation.evaluatedMessages()
		));
	}
}
