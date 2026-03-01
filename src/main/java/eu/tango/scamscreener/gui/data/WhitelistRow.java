package eu.tango.scamscreener.gui.data;

import eu.tango.scamscreener.lists.WhitelistEntry;

import java.util.UUID;

/**
 * GUI-facing row data for the whitelist screen.
 */
public record WhitelistRow(
    UUID playerUuid,
    String playerName
) {
    /**
     * Creates a GUI row from a whitelist entry.
     *
     * @param entry the source whitelist entry
     * @return a normalized GUI row
     */
    public static WhitelistRow fromEntry(WhitelistEntry entry) {
        if (entry == null) {
            return new WhitelistRow(null, "");
        }

        return new WhitelistRow(entry.playerUuid(), entry.playerName());
    }

    /**
     * Returns the primary text shown in the list.
     *
     * @return the preferred display name
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
     * Returns the secondary detail line shown below the main row text.
     *
     * @return the detail line for this row
     */
    public String detailLine() {
        if (playerUuid != null) {
            return playerUuid.toString();
        }

        return "Name-only entry";
    }
}
