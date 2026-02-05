package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.List;
import eu.tango.scamscreener.pipeline.core.RuleConfig;
import eu.tango.scamscreener.pipeline.core.TrendStore;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;

public final class TrendSignalStage {
	private final RuleConfig ruleConfig;
	private final TrendStore trendStore;

	/**
	 * Evaluates multi-message trends per player via {@link TrendStore}.
	 */
	public TrendSignalStage(RuleConfig ruleConfig, TrendStore trendStore) {
		this.ruleConfig = ruleConfig;
		this.trendStore = trendStore;
	}

	/**
	 * If the trend rule is enabled, returns a single bonus {@link Signal}.
	 */
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
