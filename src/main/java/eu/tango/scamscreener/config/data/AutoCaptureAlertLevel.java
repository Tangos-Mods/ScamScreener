package eu.tango.scamscreener.config.data;

/**
 * Auto-capture level setting matching the old v1 UI.
 */
public enum AutoCaptureAlertLevel {
    OFF(null),
    LOW(AlertRiskLevel.LOW),
    MEDIUM(AlertRiskLevel.MEDIUM),
    HIGH(AlertRiskLevel.HIGH),
    CRITICAL(AlertRiskLevel.CRITICAL);

    private final AlertRiskLevel minimumSeverity;

    AutoCaptureAlertLevel(AlertRiskLevel minimumSeverity) {
        this.minimumSeverity = minimumSeverity;
    }

    /**
     * Returns the next level in the v1 cycle order.
     *
     * @return the next auto-capture level
     */
    public AutoCaptureAlertLevel next() {
        AutoCaptureAlertLevel[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Indicates whether a given severity should be captured.
     *
     * @param severity the evaluated alert severity
     * @return {@code true} when the severity should be captured
     */
    public boolean captures(AlertRiskLevel severity) {
        if (this == OFF || severity == null) {
            return false;
        }

        return severity.isAtLeast(minimumSeverity);
    }
}
