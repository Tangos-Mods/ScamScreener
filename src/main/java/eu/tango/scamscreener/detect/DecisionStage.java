package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

public final class DecisionStage {
	private final WarningDeduplicator deduplicator;

	public DecisionStage(WarningDeduplicator deduplicator) {
		this.deduplicator = deduplicator;
	}

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

	public void reset() {
		if (deduplicator != null) {
			deduplicator.reset();
		}
	}
}
