package eu.tango.scamscreener.pipeline.rule;

/**
 * Normalized cross-sender trend rule settings.
 *
 * @param minMessageLength minimum message length for trend checks
 * @param singleSenderRepeatScore score for exactly one prior sender
 * @param multiSenderWaveScore score for wave detection
 * @param multiSenderWaveThreshold sender count threshold for wave detection
 * @param escalationBonusMinimum minimum escalation amount per extra sender
 * @param escalationBonusDivisor divisor for escalation scaling
 * @param windowMs global trend history window in milliseconds
 * @param maxHistory maximum stored global trend messages
 */
public record TrendRules(
    int minMessageLength,
    int singleSenderRepeatScore,
    int multiSenderWaveScore,
    int multiSenderWaveThreshold,
    int escalationBonusMinimum,
    int escalationBonusDivisor,
    long windowMs,
    int maxHistory
) {
    /**
     * Returns the escalation bonus for additional senders beyond the wave threshold.
     *
     * @param extraWaveSenders the number of senders above the threshold
     * @return the non-negative escalation bonus
     */
    public int escalationBonus(int extraWaveSenders) {
        if (extraWaveSenders <= 0) {
            return 0;
        }

        int safeDivisor = Math.max(1, escalationBonusDivisor);
        int perSender = Math.max(escalationBonusMinimum, singleSenderRepeatScore / safeDivisor);
        return extraWaveSenders * perSender;
    }

    /**
     * Formats the wave-detection reason text.
     *
     * @param distinctSenderCount the number of distinct matching prior senders
     * @return the formatted reason text
     */
    public String waveReason(int distinctSenderCount) {
        return "Trend wave: " + distinctSenderCount + " other senders repeated the same message";
    }

    /**
     * Formats the escalation reason text.
     *
     * @param intensityBonus the computed escalation bonus
     * @return the formatted reason text
     */
    public String escalationReason(int intensityBonus) {
        return "Trend escalation: +" + intensityBonus + " for wider repeat spread";
    }

    /**
     * Returns the single-repeat reason text.
     *
     * @return the formatted reason text
     */
    public String singleRepeatReason() {
        return "Cross-sender repeat: 1 other sender repeated the same message";
    }
}
