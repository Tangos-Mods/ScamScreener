package eu.tango.scamscreener.api;

import eu.tango.scamscreener.ScamScreenerRuntime;

import java.util.List;

/**
 * Public Fabric entrypoint that exposes the stable ScamScreener API surface.
 *
 * <p>The actual engine wiring will evolve, but the public contract can already
 * expose the stable core stage order for extension discovery.
 */
public final class ScamScreenerApiEntrypoint implements ScamScreenerApi {
    private static final ScamScreenerPipelineApi PIPELINE_API = () -> List.of(
        StageSlot.MUTE,
        StageSlot.PLAYER_LIST,
        StageSlot.RULE,
        StageSlot.LEVENSHTEIN,
        StageSlot.BEHAVIOR,
        StageSlot.TREND,
        StageSlot.FUNNEL,
        StageSlot.MODEL
    );

    /**
     * Returns the current public pipeline contract.
     *
     * @return the read-only pipeline API implementation
     */
    @Override
    public ScamScreenerPipelineApi pipeline() {
        // Keep the runtime view immutable until the real engine is wired in.
        return PIPELINE_API;
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
