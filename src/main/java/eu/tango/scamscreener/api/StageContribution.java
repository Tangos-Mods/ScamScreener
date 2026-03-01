package eu.tango.scamscreener.api;

import eu.tango.scamscreener.pipeline.core.Stage;
import lombok.Getter;
import lombok.NonNull;

/**
 * Declares a single external stage contribution.
 *
 * <p>A contribution is attached relative to a stable {@link StageSlot} so
 * other mods can extend the pipeline without replacing the core stages.
 */
@Getter
public final class StageContribution {
    private final String id;
    private final StageSlot slot;
    private final Position position;
    private final Stage stage;

    /**
     * Creates a new stage contribution descriptor.
     *
     * @param id a stable identifier for debugging and conflict reporting
     * @param slot the core slot this contribution targets
     * @param position whether the contribution runs before or after the slot
     * @param stage the contributed stage instance
     */
    public StageContribution(
        @NonNull String id,
        @NonNull StageSlot slot,
        @NonNull Position position,
        @NonNull Stage stage
    ) {
        this.id = id.trim();
        this.slot = slot;
        this.position = position;
        this.stage = stage;
    }

    /**
     * Indicates where the contributed stage should run relative to the slot.
     */
    public enum Position {
        BEFORE,
        AFTER
    }
}
