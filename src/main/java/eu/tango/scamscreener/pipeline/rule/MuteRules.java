package eu.tango.scamscreener.pipeline.rule;

import java.util.regex.Pattern;

/**
 * Normalized early-bypass rules for the mute stage.
 *
 * @param harmlessMessagePattern compiled pattern for clearly harmless short chatter
 * @param harmlessMessageMaxLength maximum message length for harmless-message bypass
 * @param duplicateWindowMs cooldown window for duplicate bypass
 * @param duplicateCacheSize maximum number of duplicate keys to retain
 * @param duplicateMaxMessageLength maximum message length considered for duplicate bypass
 */
public record MuteRules(
    Pattern harmlessMessagePattern,
    int harmlessMessageMaxLength,
    long duplicateWindowMs,
    int duplicateCacheSize,
    int duplicateMaxMessageLength
) {
    private static final String SYSTEM_BYPASS_REASON = "MUTE_SYSTEM_BYPASS";
    private static final String HARMLESS_BYPASS_REASON = "MUTE_NOISE_BYPASS";
    private static final String DUPLICATE_BYPASS_REASON = "MUTE_DUPLICATE_BYPASS";

    /**
     * Indicates whether the message is clearly harmless short chatter.
     *
     * @param normalizedMessage the normalized message
     * @return {@code true} when the message should bypass the pipeline
     */
    public boolean matchesHarmlessMessage(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        if (normalizedMessage.length() > Math.max(0, harmlessMessageMaxLength)) {
            return false;
        }

        return SafeRegex.matches(harmlessMessagePattern, normalizedMessage);
    }

    /**
     * Indicates whether the message can participate in duplicate bypass checks.
     *
     * @param normalizedMessage the normalized message
     * @return {@code true} when duplicate tracking is enabled for this message
     */
    public boolean isDuplicateCandidate(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        return duplicateWindowMs > 0
            && duplicateCacheSize > 0
            && normalizedMessage.length() <= Math.max(0, duplicateMaxMessageLength);
    }

    /**
     * Returns the system bypass reason code.
     *
     * @return the reason code
     */
    public String systemBypassReason() {
        return SYSTEM_BYPASS_REASON;
    }

    /**
     * Returns the harmless-message bypass reason code.
     *
     * @return the reason code
     */
    public String harmlessBypassReason() {
        return HARMLESS_BYPASS_REASON;
    }

    /**
     * Returns the duplicate bypass reason code.
     *
     * @return the reason code
     */
    public String duplicateBypassReason() {
        return DUPLICATE_BYPASS_REASON;
    }
}
