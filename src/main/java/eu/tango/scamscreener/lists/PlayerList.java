package eu.tango.scamscreener.lists;

import java.util.UUID;

/**
 * Minimal shared contract for player-based allow and deny lists.
 */
public interface PlayerList {
    /**
     * Checks whether the given player matches an entry in this list.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return {@code true} when the player is present in the list
     */
    boolean contains(UUID playerUuid, String playerName);

    /**
     * Removes a matching player from this list.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return {@code true} when an entry was removed
     */
    boolean remove(UUID playerUuid, String playerName);

    /**
     * Clears the list contents.
     */
    void clear();

    /**
     * Indicates whether the list is currently empty.
     *
     * @return {@code true} when no entries are stored
     */
    boolean isEmpty();
}
