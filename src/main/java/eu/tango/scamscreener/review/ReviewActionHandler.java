package eu.tango.scamscreener.review;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.lists.BlacklistSource;

/**
 * Shared runtime actions that mutate one review entry.
 */
public final class ReviewActionHandler {
    private ReviewActionHandler() {
    }

    /**
     * Sets the verdict of one review entry.
     *
     * @param entry the target review entry
     * @param verdict the target verdict
     * @return {@code true} when the entry was updated
     */
    public static boolean setVerdict(ReviewEntry entry, ReviewVerdict verdict) {
        if (entry == null) {
            return false;
        }

        return ScamScreenerRuntime.getInstance().reviewStore().setVerdict(entry.getId(), verdict);
    }

    /**
     * Adds the reviewed sender to the blacklist and marks the entry as risk.
     *
     * @param entry the target review entry
     * @return {@code true} when the action was applied
     */
    public static boolean addToBlacklist(ReviewEntry entry) {
        if (!hasPlayerTarget(entry)) {
            return false;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.whitelist().remove(entry.getSenderUuid(), entry.getSenderName());
        boolean changed = runtime.blacklist().add(
            entry.getSenderUuid(),
            entry.getSenderName(),
            entry.getScore(),
            blacklistReason(entry),
            BlacklistSource.PLAYER
        );
        runtime.reviewStore().setVerdict(entry.getId(), ReviewVerdict.RISK);
        return changed;
    }

    /**
     * Adds the reviewed sender to the whitelist and marks the entry as safe.
     *
     * @param entry the target review entry
     * @return {@code true} when the action was applied
     */
    public static boolean addToWhitelist(ReviewEntry entry) {
        if (!hasPlayerTarget(entry)) {
            return false;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.blacklist().remove(entry.getSenderUuid(), entry.getSenderName());
        boolean changed = runtime.whitelist().add(entry.getSenderUuid(), entry.getSenderName());
        runtime.reviewStore().setVerdict(entry.getId(), ReviewVerdict.SAFE);
        return changed;
    }

    /**
     * Removes one review entry from the queue.
     *
     * @param entry the target review entry
     * @return {@code true} when the entry was removed
     */
    public static boolean remove(ReviewEntry entry) {
        if (entry == null) {
            return false;
        }

        return ScamScreenerRuntime.getInstance().reviewStore().remove(entry.getId());
    }

    /**
     * Indicates whether one review entry has a usable sender target.
     *
     * @param entry the target review entry
     * @return {@code true} when UUID or sender name is present
     */
    public static boolean hasPlayerTarget(ReviewEntry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getSenderUuid() != null || !entry.getSenderName().isBlank();
    }

    private static String blacklistReason(ReviewEntry entry) {
        if (entry == null) {
            return "Review queue action";
        }
        if (!entry.getDecidedByStage().isBlank()) {
            return "Review queue action (" + entry.getDecidedByStage() + ")";
        }

        return "Review queue action";
    }
}
