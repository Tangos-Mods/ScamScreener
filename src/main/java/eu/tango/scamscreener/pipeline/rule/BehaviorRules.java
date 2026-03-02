package eu.tango.scamscreener.pipeline.rule;

/**
 * Normalized sender-local behavior rule settings.
 *
 * @param minRepeatMessageLength minimum message length for repeat checks
 * @param minBurstMessageLength minimum message length for burst checks
 * @param repeatedMessageThreshold prior repeat threshold
 * @param repeatedMessageScore repeat score contribution
 * @param burstContactThreshold prior burst threshold
 * @param burstContactScore burst score contribution
 * @param comboBonusMinimum minimum combo bonus
 * @param comboBonusDivisor divisor for combo bonus scaling
 * @param windowMs sender-local history window in milliseconds
 * @param maxHistory maximum stored sender-local messages
 */
public record BehaviorRules(
    int minRepeatMessageLength,
    int minBurstMessageLength,
    int repeatedMessageThreshold,
    int repeatedMessageScore,
    int burstContactThreshold,
    int burstContactScore,
    int comboBonusMinimum,
    int comboBonusDivisor,
    long windowMs,
    int maxHistory
) {
    /**
     * Returns the combined repeat-plus-burst bonus.
     *
     * @return the non-negative combo bonus
     */
    public int comboBonus() {
        int safeDivisor = Math.max(1, comboBonusDivisor);
        return Math.max(comboBonusMinimum, Math.min(repeatedMessageScore, burstContactScore) / safeDivisor);
    }

    /**
     * Formats the repeated-message reason text.
     *
     * @param messageCount the total repeated message count including the current message
     * @return the formatted reason text
     */
    public String repeatedMessageReason(int messageCount) {
        return "Repeated contact message x" + messageCount;
    }

    /**
     * Formats the burst-contact reason text.
     *
     * @param messageCount the total message count in the current burst including the current message
     * @return the formatted reason text
     */
    public String burstContactReason(int messageCount) {
        return "Burst contact: " + messageCount + " messages in short window";
    }

    /**
     * Returns the centralized combo reason text.
     *
     * @return the combo reason text
     */
    public String comboReason() {
        return "Behavior combo: repeated burst contact";
    }
}
