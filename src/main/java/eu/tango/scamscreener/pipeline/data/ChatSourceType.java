package eu.tango.scamscreener.pipeline.data;

/**
 * High-level source classification for an inbound chat event.
 */
public enum ChatSourceType {
    /**
     * A message authored by a player.
     */
    PLAYER,
    /**
     * A system-like message that was visibly sent through a player chat line.
     */
    SYSTEM_PLAYER,
    /**
     * A message authored by the server, an NPC, or another system source.
     */
    SYSTEM,
    /**
     * A message whose source could not be determined.
     */
    UNKNOWN
}
