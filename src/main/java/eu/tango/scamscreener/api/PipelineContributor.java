package eu.tango.scamscreener.api;

import java.util.Collection;
import java.util.List;

/**
 * Fabric entrypoint contract for mods that want to contribute pipeline stages.
 *
 * <p>Contributors declare additional stages, but do not get direct write access
 * to the internal engine or the core stage list.
 */
public interface PipelineContributor {
    String ENTRYPOINT_KEY = "scamscreener-pipeline";

    /**
     * Returns the contributed stages for this mod.
     *
     * @return the contributed stage descriptors, or an empty collection
     */
    default Collection<StageContribution> stageContributions() {
        // Empty by default so mods can implement only the hooks they need later.
        return List.of();
    }
}
