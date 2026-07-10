package eu.tango.scamscreener.chat;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Central filter for visible player chat lines that should stay out of the pipeline.
 *
 * <p>Each entry represents the message body after the player prefix was removed.
 */
public final class PrePipelinePlayerMessageFilter {
    private static final Set<String> PREFIX_MESSAGES = Set.of(
        "[skyblocker]"
    );
    private static final Set<String> EXACT_MESSAGES = Set.of(
        "prince dead!",
        "mimic dead!",
        "0 (0.00%)",
        "./tptodonexpresso"
    );
    private static final List<Pattern> MESSAGE_PATTERNS = List.of(
        Pattern.compile("^\\d+ score reached!$"),
        Pattern.compile("^we only have [0-4] crypts out of 5, we need more!$"),
        Pattern.compile("^the livid color is (white|magenta|red|blue|lime|yellow|purple|green|gray)$"),
        Pattern.compile(".*\\bplease be mindful of discord links in chat as they may pose a security risk\\b.*")
    );

    private PrePipelinePlayerMessageFilter() {
    }

    public static boolean matches(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        for (String prefixMessage : PREFIX_MESSAGES) {
            if (normalizedMessage.startsWith(prefixMessage)) {
                return true;
            }
        }

        if (EXACT_MESSAGES.contains(normalizedMessage)) {
            return true;
        }

        for (Pattern pattern : MESSAGE_PATTERNS) {
            if (pattern.matcher(normalizedMessage).matches()) {
                return true;
            }
        }

        return false;
    }
}
