package eu.tango.scamscreener.pipeline.rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small safety wrapper around regex evaluation.
 *
 * <p>This prevents malformed or overly complex user-configured regex patterns
 * from crashing the client via {@link StackOverflowError} during matching.
 */
final class SafeRegex {
    private static final int MAX_INPUT_LENGTH = 512;

    private SafeRegex() {
    }

    static String firstMatch(Pattern pattern, String message) {
        return evaluate(pattern, message, matcher -> matcher.find() ? matcher.group() : null, null);
    }

    static boolean matches(Pattern pattern, String message) {
        return evaluate(pattern, message, Matcher::matches, false);
    }

    static <T> T evaluate(Pattern pattern, String message, RegexOperation<T> operation, T fallback) {
        if (pattern == null || message == null || message.isBlank() || message.length() > MAX_INPUT_LENGTH) {
            return fallback;
        }

        try {
            return operation.apply(pattern.matcher(message));
        } catch (StackOverflowError ignored) {
            return fallback;
        }
    }

    @FunctionalInterface
    interface RegexOperation<T> {
        T apply(Matcher matcher);
    }
}
