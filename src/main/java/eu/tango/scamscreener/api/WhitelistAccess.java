package eu.tango.scamscreener.api;

import eu.tango.scamscreener.lists.WhitelistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read/write access contract for the shared whitelist.
 */
public interface WhitelistAccess {
    /**
     * Adds or updates a whitelist entry.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return {@code true} when the entry was stored
     */
    boolean add(UUID playerUuid, String playerName);

    /**
     * Looks up a whitelist entry by UUID.
     *
     * @param playerUuid the player UUID to look up
     * @return the matching whitelist entry, when present
     */
    Optional<WhitelistEntry> get(UUID playerUuid);

    /**
     * Looks up a whitelist entry by player name.
     *
     * @param playerName the player name to look up
     * @return the matching whitelist entry, when present
     */
    Optional<WhitelistEntry> findByName(String playerName);

    /**
     * Returns every unique whitelist entry.
     *
     * @return the stored whitelist entries in insertion order
     */
    List<WhitelistEntry> allEntries();

    /**
     * Checks whether the whitelist contains the given UUID.
     *
     * @param playerUuid the player UUID to check
     * @return {@code true} when a matching entry exists
     */
    boolean contains(UUID playerUuid);

    /**
     * Checks whether the whitelist contains the given player name.
     *
     * @param playerName the player name to check
     * @return {@code true} when a matching entry exists
     */
    boolean containsName(String playerName);

    /**
     * Removes a whitelist entry by UUID.
     *
     * @param playerUuid the player UUID to remove
     * @return {@code true} when an entry was removed
     */
    boolean remove(UUID playerUuid);

    /**
     * Removes a whitelist entry by player name.
     *
     * @param playerName the player name to remove
     * @return {@code true} when an entry was removed
     */
    boolean removeByName(String playerName);

    /**
     * Clears all whitelist entries.
     */
    void clear();

    /**
     * Indicates whether the whitelist is currently empty.
     *
     * @return {@code true} when no entries are stored
     */
    boolean isEmpty();
}
