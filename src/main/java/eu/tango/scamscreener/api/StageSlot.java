package eu.tango.scamscreener.api;

/**
 * Stable extension slots for the core ScamScreener v2 pipeline.
 *
 * <p>External mods can target these slots when contributing custom stages,
 * without mutating the internal pipeline structure directly.
 */
public enum StageSlot {
    MUTE,
    PLAYER_LIST,
    RULE,
    LEVENSHTEIN,
    BEHAVIOR,
    TREND,
    FUNNEL,
    /**
     * Final extension slot, currently occupied by the built-in context-aware stage.
     */
    MODEL
}
