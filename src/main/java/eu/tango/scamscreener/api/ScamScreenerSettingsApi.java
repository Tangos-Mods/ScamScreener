package eu.tango.scamscreener.api;

/**
 * Stable public access to the user-facing ScamScreener runtime settings.
 *
 * <p>This keeps integrations off the raw config schema so welcome wizards or
 * companion mods can read and update a supported subset without rewriting
 * config files directly.
 */
public interface ScamScreenerSettingsApi {
    /**
     * Indicates whether visible risk warnings should play a ping.
     *
     * @return {@code true} when risk warnings ping locally
     */
    boolean pingOnRiskWarning();

    /**
     * Updates whether visible risk warnings should play a ping.
     *
     * @param enabled {@code true} to enable risk-warning pings
     */
    void setPingOnRiskWarning(boolean enabled);

    /**
     * Indicates whether blacklist warnings should play a ping.
     *
     * @return {@code true} when blacklist warnings ping locally
     */
    boolean pingOnBlacklistWarning();

    /**
     * Updates whether blacklist warnings should play a ping.
     *
     * @param enabled {@code true} to enable blacklist-warning pings
     */
    void setPingOnBlacklistWarning(boolean enabled);

    /**
     * Returns the minimum alert level that should be shown to the user.
     *
     * @return the configured minimum alert level
     */
    ScamScreenerAlertLevel alertMinimumRiskLevel();

    /**
     * Updates the minimum alert level that should be shown to the user.
     *
     * @param level the new minimum alert level
     */
    void setAlertMinimumRiskLevel(ScamScreenerAlertLevel level);
}
