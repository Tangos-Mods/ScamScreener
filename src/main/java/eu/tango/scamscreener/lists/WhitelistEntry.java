package eu.tango.scamscreener.lists;

import java.util.UUID;

/**
 * Immutable public view of one whitelist entry.
 */
public record WhitelistEntry(
    UUID playerUuid,
    String playerName
) {
    public WhitelistEntry {
        playerName = playerName == null ? "" : playerName.trim();
    }

    /**
     * Indicates whether the entry has a non-empty player name.
     *
     * @return {@code true} when a player name is present
     */
    public boolean hasPlayerName() {
        return !playerName.isEmpty();
    }
}
