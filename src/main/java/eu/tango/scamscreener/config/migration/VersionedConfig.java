package eu.tango.scamscreener.config.migration;

/**
 * Common contract for persisted config payloads with schema versions.
 */
public interface VersionedConfig {
    /**
     * Returns the normalized schema version of the config payload.
     *
     * @return the non-negative schema version
     */
    int version();

    /**
     * Updates the raw schema version of the config payload.
     *
     * @param version the schema version to store
     */
    void setVersion(int version);
}
