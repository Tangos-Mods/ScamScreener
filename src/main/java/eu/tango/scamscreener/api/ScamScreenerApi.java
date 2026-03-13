package eu.tango.scamscreener.api;

/**
 * Public runtime API exposed by ScamScreener for other mods.
 *
 * <p>The API stays intentionally narrow: consumers can inspect stable contracts
 * and later query high-level services without mutating internal implementation details.
 */
public interface ScamScreenerApi {
    String ENTRYPOINT_KEY = "scamscreener-api";

    /**
     * Returns the public pipeline API.
     *
     * @return the stable read-only pipeline contract
     */
    ScamScreenerPipelineApi pipeline();

    /**
     * Returns the stable runtime-settings API exposed to other mods.
     *
     * @return the stable settings access contract
     */
    ScamScreenerSettingsApi settings();

    /**
     * Returns the current schema versions used by ScamScreener config files.
     *
     * @return the stable config-schema contract
     */
    ScamScreenerSchemaApi schemas();

    /**
     * Returns the shared whitelist used by the runtime pipeline.
     *
     * @return the shared whitelist access contract
     */
    WhitelistAccess whitelist();

    /**
     * Returns the shared blacklist used by the runtime pipeline.
     *
     * @return the shared blacklist access contract
     */
    BlacklistAccess blacklist();

    /**
     * Reloads runtime config and persisted list state from disk.
     */
    void reload();
}
