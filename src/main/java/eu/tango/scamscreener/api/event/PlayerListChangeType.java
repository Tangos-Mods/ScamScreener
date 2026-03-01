package eu.tango.scamscreener.api.event;

/**
 * High-level change types emitted by whitelist and blacklist events.
 */
public enum PlayerListChangeType {
    /**
     * A new entry was added to the list.
     */
    ADDED,
    /**
     * An existing entry was replaced or updated.
     */
    UPDATED,
    /**
     * A single entry was removed from the list.
     */
    REMOVED,
    /**
     * The full list was cleared.
     */
    CLEARED,
    /**
     * The list was reloaded from persisted storage.
     */
    RELOADED
}
