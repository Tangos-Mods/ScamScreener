package eu.tango.scamscreener.lists;

import java.util.UUID;

/**
 * Immutable public view of one blacklist entry.
 */
public record BlacklistEntry(
    UUID playerUuid,
    String playerName,
    int score,
    String reason,
    BlacklistSource source
) {
    public BlacklistEntry {
        playerName = playerName == null ? "" : playerName.trim();
        score = Math.max(0, score);
        reason = reason == null ? "" : reason.trim();
        source = source == null ? BlacklistSource.PLAYER : source;
    }

    /**
     * Indicates whether the entry has a non-empty reason.
     *
     * @return {@code true} when a reason is present
     */
    public boolean hasReason() {
        return !reason.isEmpty();
    }
}
