package eu.tango.scamscreener.message;

import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

/**
 * Shared alert-severity mapping used by the restored v1 settings flow.
 */
public enum AlertSeverity {
    LOW(AlertRiskLevel.LOW),
    MEDIUM(AlertRiskLevel.MEDIUM),
    HIGH(AlertRiskLevel.HIGH),
    CRITICAL(AlertRiskLevel.CRITICAL);

    private final AlertRiskLevel riskLevel;

    AlertSeverity(AlertRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    /**
     * Returns the user-facing risk level.
     *
     * @return the mapped risk level
     */
    public AlertRiskLevel riskLevel() {
        return riskLevel;
    }

    /**
     * Maps a pipeline decision to the v1 severity ladder.
     *
     * @param decision the pipeline decision
     * @return the mapped alert severity
     */
    public static AlertSeverity fromDecision(PipelineDecision decision) {
        if (decision != null && decision.getOutcome() == PipelineDecision.Outcome.BLOCK) {
            return CRITICAL;
        }

        int score = decision == null ? 0 : Math.max(0, decision.getTotalScore());
        if (score >= 75) {
            return CRITICAL;
        }
        if (score >= 50) {
            return HIGH;
        }
        if (score >= 25) {
            return MEDIUM;
        }

        return LOW;
    }
}
