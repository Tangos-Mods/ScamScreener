package eu.tango.scamscreener.gui.data;

import eu.tango.scamscreener.lists.BlacklistEntry;

import java.util.UUID;

/**
 * GUI-facing row data for the blacklist screen.
 */
public record BlacklistRow(
    UUID playerUuid,
    String playerName,
    int score,
    String reason,
    String source
) {
    /**
     * Creates a GUI row from a blacklist entry.
     *
     * @param entry the source blacklist entry
     * @return a normalized GUI row
     */
    public static BlacklistRow fromEntry(BlacklistEntry entry) {
        if (entry == null) {
            return new BlacklistRow(null, "", 0, "", "");
        }

        return new BlacklistRow(
            entry.playerUuid(),
            entry.playerName(),
            entry.score(),
            entry.reason(),
            entry.source().name()
        );
    }

    /**
     * Returns the preferred display name for the row.
     *
     * @return the display name
     */
    public String displayName() {
        if (playerName != null && !playerName.isBlank()) {
            return playerName.trim();
        }

        if (playerUuid != null) {
            return "(UUID only)";
        }

        return "(empty)";
    }

    /**
     * Returns the main detail line for the row.
     *
     * @return the detail line
     */
    public String detailLine() {
        return "Score " + Math.max(0, score) + " | Source " + normalize(source);
    }

    /**
     * Returns the optional extra line shown for the row.
     *
     * @return the reason or UUID detail
     */
    public String extraLine() {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }

        if (playerUuid != null) {
            return playerUuid.toString();
        }

        return "No reason";
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
