package eu.tango.scamscreener.api;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.pipeline.core.ScamScreenerPipelineFactory;

/**
 * Public Fabric entrypoint that exposes the stable ScamScreener API surface.
 *
 * <p>The public contract exposes the stable core stage order and the live
 * runtime-backed services used by ScamScreener itself.
 */
public final class ScamScreenerApiEntrypoint implements ScamScreenerApi {
    private static final ScamScreenerPipelineApi PIPELINE_API = ScamScreenerPipelineFactory::coreStageOrder;
    private static final ScamScreenerSettingsApi SETTINGS_API = new RuntimeConfigSettingsApi(
        () -> ScamScreenerRuntime.getInstance().config(),
        () -> ScamScreenerRuntime.getInstance().saveConfig()
    );
    private static final ScamScreenerSchemaApi SCHEMA_API = new StaticSchemaApi();

    /**
     * Returns the current public pipeline contract.
     *
     * @return the read-only pipeline API implementation
     */
    @Override
    public ScamScreenerPipelineApi pipeline() {
        return PIPELINE_API;
    }

    /**
     * Returns the stable public settings contract.
     *
     * @return the runtime-backed settings API
     */
    @Override
    public ScamScreenerSettingsApi settings() {
        return SETTINGS_API;
    }

    /**
     * Returns the current config schema versions exposed publicly.
     *
     * @return the config-schema API
     */
    @Override
    public ScamScreenerSchemaApi schemas() {
        return SCHEMA_API;
    }

    /**
     * Returns the shared runtime whitelist.
     *
     * @return the shared whitelist access contract
     */
    @Override
    public WhitelistAccess whitelist() {
        return ScamScreenerRuntime.getInstance().whitelist();
    }

    /**
     * Returns the shared runtime blacklist.
     *
     * @return the shared blacklist access contract
     */
    @Override
    public BlacklistAccess blacklist() {
        return ScamScreenerRuntime.getInstance().blacklist();
    }

    /**
     * Reloads runtime config and persisted list state from disk.
     */
    @Override
    public void reload() {
        ScamScreenerRuntime.getInstance().reload();
    }
}
