package eu.tango.scamscreener.api;

import java.util.List;

/**
 * Read-only public view of the ScamScreener pipeline contract.
 *
 * <p>This lets external mods inspect the stable core slots without gaining
 * direct control over the internal engine implementation.
 */
public interface ScamScreenerPipelineApi {
    /**
     * Returns the fixed order of the core pipeline slots.
     *
     * @return the stable core stage order exposed to external mods
     */
    List<StageSlot> coreStageOrder();

    /**
     * Indicates whether the given slot exists in the current public pipeline contract.
     *
     * @param slot the slot to check
     * @return {@code true} when the slot is part of the exposed core order
     */
    default boolean supports(StageSlot slot) {
        if (slot == null) {
            return false;
        }

        // Delegate to the exposed order so support checks stay consistent.
        return coreStageOrder().contains(slot);
    }
}
