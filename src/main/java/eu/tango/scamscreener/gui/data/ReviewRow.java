package eu.tango.scamscreener.gui.data;

import eu.tango.scamscreener.review.ReviewCaseMessage;
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
            caseTitle(entry),
            caseSummary(entry),
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

        return "Case";
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

    private static String caseTitle(ReviewEntry entry) {
        if (entry == null) {
            return "Case";
        }

        String numericId = trailingNumericId(entry.getId());
        if (isManual(entry)) {
            return numericId.isBlank() ? "Manual Case" : "Manual Case #" + numericId;
        }

        return numericId.isBlank() ? "Case" : "Case #" + numericId;
    }

    private static String caseSummary(ReviewEntry entry) {
        if (entry == null) {
            return "";
        }

        int messageCount = 0;
        int signalCount = 0;
        if (entry.hasCaseMessages()) {
            for (ReviewCaseMessage caseMessage : entry.getCaseMessages()) {
                if (caseMessage == null || caseMessage.getCleanText().isBlank()) {
                    continue;
                }
                messageCount++;
                if (caseMessage.isSignalMessage()) {
                    signalCount++;
                }
            }
        }
        if (messageCount == 0 && entry.getMessage() != null && !entry.getMessage().isBlank()) {
            messageCount = 1;
        }

        String stageLabel = entry.getDecidedByStage() == null || entry.getDecidedByStage().isBlank()
            ? "Manual"
            : entry.getDecidedByStage().trim();
        return messageCount + " messages | " + signalCount + " signals | " + stageLabel;
    }

    private static boolean isManual(ReviewEntry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getDecidedByStage() == null || entry.getDecidedByStage().isBlank();
    }

    private static String trailingNumericId(String rowId) {
        if (rowId == null || rowId.isBlank()) {
            return "";
        }

        int separatorIndex = rowId.lastIndexOf('-');
        return separatorIndex < 0 ? rowId.trim() : rowId.substring(separatorIndex + 1).trim();
    }
}
