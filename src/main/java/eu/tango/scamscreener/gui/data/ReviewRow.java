package eu.tango.scamscreener.gui.data;

import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;

/**
 * GUI-facing review row data.
 */
public record ReviewRow(
    String rowId,
    String playerName,
    String message,
    int score,
    ReviewVerdict verdict
) {
    public ReviewRow {
        rowId = rowId == null ? "" : rowId.trim();
        playerName = playerName == null ? "" : playerName.trim();
        message = normalizeMessage(message);
        score = Math.max(0, score);
        verdict = verdict == null ? ReviewVerdict.PENDING : verdict;
    }

    /**
     * Creates a GUI review row from a stored review entry.
     *
     * @param entry the stored review entry
     * @return a normalized GUI row
     */
    public static ReviewRow fromEntry(ReviewEntry entry) {
        if (entry == null) {
            return new ReviewRow("", "", "", 0, ReviewVerdict.PENDING);
        }

        return new ReviewRow(
            entry.getId(),
            entry.getSenderName(),
            entry.getMessage(),
            entry.getScore(),
            entry.getVerdict()
        );
    }

    /**
     * Returns the visible player label for this row.
     *
     * @return the visible player label
     */
    public String displayName() {
        if (!playerName.isBlank()) {
            return playerName;
        }

        return "Unknown Sender";
    }

    /**
     * Returns the visible message snippet for this row.
     *
     * @return the visible review snippet
     */
    public String compactMessage() {
        if (message.length() <= 72) {
            return message;
        }

        return message.substring(0, 69) + "...";
    }

    private static String normalizeMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }

        return rawMessage.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
