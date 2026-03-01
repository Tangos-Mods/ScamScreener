package eu.tango.scamscreener.api;

import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.BlacklistSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read/write access contract for the shared blacklist.
 */
public interface BlacklistAccess {
    /**
     * Adds or updates a blacklist entry.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @param score the score associated with the blacklist entry
     * @param reason the reason for the blacklist entry
     * @param source the origin of the blacklist entry
     * @return {@code true} when the entry was stored
     */
    boolean add(UUID playerUuid, String playerName, int score, String reason, BlacklistSource source);

    /**
     * Adds or updates a blacklist entry using the default player-driven source.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @param score the score associated with the blacklist entry
     * @param reason the reason for the blacklist entry
     * @return {@code true} when the entry was stored
     */
    default boolean add(UUID playerUuid, String playerName, int score, String reason) {
        return add(playerUuid, playerName, score, reason, BlacklistSource.PLAYER);
    }

    /**
     * Looks up a blacklist entry by UUID.
     *
     * @param playerUuid the player UUID to look up
     * @return the matching blacklist entry, when present
     */
    Optional<BlacklistEntry> get(UUID playerUuid);

    /**
     * Looks up a blacklist entry by player name.
     *
     * @param playerName the player name to look up
     * @return the matching blacklist entry, when present
     */
    Optional<BlacklistEntry> findByName(String playerName);

    /**
     * Returns every unique blacklist entry.
     *
     * @return the stored blacklist entries in insertion order
     */
    List<BlacklistEntry> allEntries();

    /**
     * Checks whether the blacklist contains the given UUID.
     *
     * @param playerUuid the player UUID to check
     * @return {@code true} when a matching entry exists
     */
    boolean contains(UUID playerUuid);

    /**
     * Checks whether the blacklist contains the given player name.
     *
     * @param playerName the player name to check
     * @return {@code true} when a matching entry exists
     */
    boolean containsName(String playerName);

    /**
     * Removes a blacklist entry by UUID.
     *
     * @param playerUuid the player UUID to remove
     * @return {@code true} when an entry was removed
     */
    boolean remove(UUID playerUuid);

    /**
     * Removes a blacklist entry by player name.
     *
     * @param playerName the player name to remove
     * @return {@code true} when an entry was removed
     */
    boolean removeByName(String playerName);

    /**
     * Clears all blacklist entries.
     */
    void clear();

    /**
     * Indicates whether the blacklist is currently empty.
     *
     * @return {@code true} when no entries are stored
     */
    boolean isEmpty();
}
