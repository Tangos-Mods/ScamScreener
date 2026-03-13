package eu.tango.scamscreener.api;

/**
 * Stable public view of the current ScamScreener config schema versions.
 */
public interface ScamScreenerSchemaApi {
    /**
     * Returns the current schema version written to {@code runtime.json}.
     *
     * @return the runtime config schema version
     */
    int runtimeConfigVersion();

    /**
     * Returns the current schema version written to {@code rules.json}.
     *
     * @return the rules config schema version
     */
    int rulesConfigVersion();

    /**
     * Returns the current schema version written to {@code whitelist.json}.
     *
     * @return the whitelist config schema version
     */
    int whitelistConfigVersion();

    /**
     * Returns the current schema version written to {@code blacklist.json}.
     *
     * @return the blacklist config schema version
     */
    int blacklistConfigVersion();

    /**
     * Returns the current schema version written to {@code review.json}.
     *
     * @return the review config schema version
     */
    int reviewConfigVersion();
}
