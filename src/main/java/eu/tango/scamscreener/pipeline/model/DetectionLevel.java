package eu.tango.scamscreener.pipeline.model;

import eu.tango.scamscreener.rules.ScamRules;

public enum DetectionLevel {
	LOW,
	MEDIUM,
	HIGH,
	CRITICAL;

	public static DetectionLevel fromScore(double score) {
		if (score >= ScamRules.levelCriticalThreshold()) {
			return CRITICAL;
		}
		if (score >= ScamRules.levelHighThreshold()) {
			return HIGH;
		}
		if (score >= ScamRules.levelMediumThreshold()) {
			return MEDIUM;
		}
		return LOW;
	}

	public ScamRules.ScamRiskLevel toRiskLevel() {
		return switch (this) {
			case LOW -> ScamRules.ScamRiskLevel.LOW;
			case MEDIUM -> ScamRules.ScamRiskLevel.MEDIUM;
			case HIGH -> ScamRules.ScamRiskLevel.HIGH;
			case CRITICAL -> ScamRules.ScamRiskLevel.CRITICAL;
		};
	}

	public static ScamRules.ScamRiskLevel toRiskLevel(DetectionLevel level) {
		return level == null ? ScamRules.ScamRiskLevel.LOW : level.toRiskLevel();
	}
}
