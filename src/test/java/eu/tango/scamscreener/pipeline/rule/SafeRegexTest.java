package eu.tango.scamscreener.pipeline.rule;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SafeRegexTest {
    @Test
    void returnsFallbackWhenMatcherOverflowsStack() {
        String result = SafeRegex.evaluate(
            Pattern.compile("a"),
            "a",
            matcher -> {
                throw new StackOverflowError("boom");
            },
            "fallback"
        );

        assertEquals("fallback", result);
    }

    @Test
    void skipsRegexMatchingForOverlyLongMessages() {
        boolean matched = SafeRegex.matches(Pattern.compile("discord"), "a".repeat(600) + "discord");

        assertFalse(matched);
    }
}
