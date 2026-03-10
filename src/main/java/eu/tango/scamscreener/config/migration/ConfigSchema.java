package eu.tango.scamscreener.config.migration;

/**
 * Central schema-version registry for all persisted ScamScreener configs.
 */
public enum ConfigSchema {
    RUNTIME(3),
    RULES(1),
    WHITELIST(1),
    BLACKLIST(1),
    REVIEW(1);

    private final int currentVersion;

    ConfigSchema(int currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Returns the current schema version for the config family.
     *
     * @return the non-negative schema version
     */
    public int currentVersion() {
        return Math.max(0, currentVersion);
    }
}
