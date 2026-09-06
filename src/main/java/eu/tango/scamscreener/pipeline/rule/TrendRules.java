package eu.tango.scamscreener.pipeline.rule;

/**
 * Normalized cross-sender trend rule settings.
 *
 * @param minMessageLength minimum message length for trend checks
 * @param singleSenderRepeatScore score for a matching message from another sender
 * @param multiSenderWaveScore score for wave detection
 * @param multiSenderWaveThreshold sender count threshold for wave detection
 * @param windowMs global trend history window in milliseconds
 * @param maxHistory maximum stored global trend messages
 */
public record TrendRules(
    int minMessageLength,
    int singleSenderRepeatScore,
    int multiSenderWaveScore,
    int multiSenderWaveThreshold,
    long windowMs,
    int maxHistory
) {
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
     * Returns the single-repeat reason text.
     *
     * @return the formatted reason text
     */
    public String singleRepeatReason(int distinctSenderCount) {
        return "Cross-sender repeat: " + distinctSenderCount
            + " other sender" + (distinctSenderCount == 1 ? "" : "s")
            + " repeated the same message";
    }
}
