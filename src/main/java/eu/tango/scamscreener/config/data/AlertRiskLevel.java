package eu.tango.scamscreener.config.data;

/**
 * User-facing alert severity levels from the v1 settings flow.
 */
public enum AlertRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Returns the next level in the v1 cycle order.
     *
     * @return the next alert risk level
     */
    public AlertRiskLevel next() {
        AlertRiskLevel[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Indicates whether this level is at least as severe as the given threshold.
     *
     * @param threshold the minimum threshold
     * @return {@code true} when this level passes the threshold
     */
    public boolean isAtLeast(AlertRiskLevel threshold) {
        if (threshold == null) {
            return true;
        }

        return ordinal() >= threshold.ordinal();
    }
}
