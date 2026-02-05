package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.pipeline.core.DetectionScoring;
import eu.tango.scamscreener.pipeline.core.WarningDeduplicator;
import eu.tango.scamscreener.pipeline.model.DetectionDecision;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class DecisionStage {
	private final WarningDeduplicator deduplicator;

	/**
	 * Applies minimum risk thresholds and de-duplication before warning.
	 */
	public DecisionStage(WarningDeduplicator deduplicator) {
		this.deduplicator = deduplicator;
	}

	/**
	 * Returns whether a warning should be emitted for this result.
	 */
	public DetectionDecision decide(MessageEvent event, DetectionResult result) {
		if (result == null) {
			return new DetectionDecision(false);
		}
		if (result.totalScore() <= 0 || result.triggeredRules().isEmpty()) {
			return new DetectionDecision(false);
		}
		ScamRules.ScamRiskLevel minimum = ScamRules.minimumAlertRiskLevel();
		ScamRules.ScamRiskLevel actual = DetectionScoring.toScamRiskLevel(result.level());
		if (actual.ordinal() < minimum.ordinal()) {
			return new DetectionDecision(false);
		}

		boolean shouldWarn = deduplicator == null || deduplicator.shouldWarn(event, result.level());
		return new DetectionDecision(shouldWarn);
	}

	/**
	 * Resets de-duplication state.
	 */
	public void reset() {
		if (deduplicator != null) {
			deduplicator.reset();
		}
	}
}
