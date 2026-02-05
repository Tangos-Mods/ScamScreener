package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

public final class DetectionScoring {
	private DetectionScoring() {
	}

	public static DetectionLevel mapLevel(double score) {
		if (score >= 70) {
			return DetectionLevel.CRITICAL;
		}
		if (score >= 40) {
			return DetectionLevel.HIGH;
		}
		if (score >= 20) {
			return DetectionLevel.MEDIUM;
		}
		return DetectionLevel.LOW;
	}

	public static ScamRules.ScamRiskLevel toScamRiskLevel(DetectionLevel level) {
		if (level == null) {
			return ScamRules.ScamRiskLevel.LOW;
		}
		return switch (level) {
			case LOW -> ScamRules.ScamRiskLevel.LOW;
			case MEDIUM -> ScamRules.ScamRiskLevel.MEDIUM;
			case HIGH -> ScamRules.ScamRiskLevel.HIGH;
			case CRITICAL -> ScamRules.ScamRiskLevel.CRITICAL;
		};
	}
}
