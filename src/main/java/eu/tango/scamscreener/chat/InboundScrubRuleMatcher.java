package eu.tango.scamscreener.chat;

import java.util.regex.Pattern;

/**
 * Matches inbound chat lines that should be dropped before ScamScreener processing.
 */
public final class InboundScrubRuleMatcher {
    private static final String SKYBLOCKER_PREFIX = "[skyblocker]";
    private static final String LIVID_COLOR_PHRASE = "the livid color";
    private static final String SCORE_REACHED_PHRASE = "score reached!";
    private static final Pattern CRYPTS_MESSAGE =
        Pattern.compile("^we only have [0-4] crypts out of 5, we need more!$");

    private InboundScrubRuleMatcher() {
    }

    public static boolean matches(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        return normalizedMessage.startsWith(SKYBLOCKER_PREFIX)
            || normalizedMessage.contains(LIVID_COLOR_PHRASE)
            || normalizedMessage.contains(SCORE_REACHED_PHRASE)
            || CRYPTS_MESSAGE.matcher(normalizedMessage).matches();
    }
}
