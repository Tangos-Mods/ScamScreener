package eu.tango.scamscreener.chat.mute;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Lightweight restored mute-pattern manager for the v1 mute filter flow.
 */
public final class MutePatternManager {
    private final Map<String, Pattern> compiledPatterns = new LinkedHashMap<>();

    /**
     * Reloads the compiled patterns from the persisted runtime config.
     */
    public synchronized void reloadFromConfig(RuntimeConfig runtimeConfig) {
        compiledPatterns.clear();
        if (runtimeConfig == null) {
            return;
        }

        for (String pattern : runtimeConfig.safety().mutePatterns()) {
            String normalizedPattern = normalizePattern(pattern);
            if (normalizedPattern.isEmpty()) {
                continue;
            }

            try {
                compiledPatterns.putIfAbsent(normalizedPattern, Pattern.compile(normalizedPattern, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ignored) {
                // Keep loading valid patterns even if an older persisted pattern is broken.
            }
        }
    }

    /**
     * Indicates whether the mute filter is enabled.
     *
     * @return {@code true} when inbound mute filtering is enabled
     */
    public boolean isEnabled() {
        return ScamScreenerRuntime.getInstance().config().safety().isMuteFilterEnabled();
    }

    /**
     * Updates the mute-filter enabled flag and persists it.
     *
     * @param enabled the new enabled value
     */
    public void setEnabled(boolean enabled) {
        ScamScreenerRuntime.getInstance().config().safety().setMuteFilterEnabled(enabled);
        ScamScreenerRuntime.getInstance().saveConfig();
    }

    /**
     * Adds a new regex pattern to the mute filter.
     *
     * @param pattern the raw regex pattern
     * @return the add result
     */
    public synchronized AddResult addPattern(String pattern) {
        String normalizedPattern = normalizePattern(pattern);
        if (normalizedPattern.isEmpty()) {
            return AddResult.INVALID;
        }

        if (compiledPatterns.containsKey(normalizedPattern)) {
            return AddResult.ALREADY_EXISTS;
        }

        try {
            Pattern compiledPattern = Pattern.compile(normalizedPattern, Pattern.CASE_INSENSITIVE);
            compiledPatterns.put(normalizedPattern, compiledPattern);
            persistPatterns();
            return AddResult.ADDED;
        } catch (PatternSyntaxException ignored) {
            return AddResult.INVALID;
        }
    }

    /**
     * Removes one stored mute pattern.
     *
     * @param pattern the raw pattern
     * @return {@code true} when the pattern was removed
     */
    public synchronized boolean removePattern(String pattern) {
        String normalizedPattern = normalizePattern(pattern);
        if (normalizedPattern.isEmpty()) {
            return false;
        }

        boolean removed = compiledPatterns.remove(normalizedPattern) != null;
        if (removed) {
            persistPatterns();
        }

        return removed;
    }

    /**
     * Returns the configured mute patterns in insertion order.
     *
     * @return the configured mute patterns
     */
    public synchronized List<String> allPatterns() {
        return List.copyOf(compiledPatterns.keySet());
    }

    /**
     * Indicates whether a chat line should be filtered.
     *
     * @param message the inbound chat line
     * @return {@code true} when the line matches a mute pattern
     */
    public synchronized boolean shouldBlock(String message) {
        if (!isEnabled() || message == null || message.isBlank()) {
            return false;
        }

        if (message.length() > 512) {
            return false;
        }

        for (Pattern pattern : compiledPatterns.values()) {
            try {
                if (pattern.matcher(message).find()) {
                    return true;
                }
            } catch (StackOverflowError ignored) {
                // Treat pathological regexes as non-matches instead of crashing the client.
            }
        }

        return false;
    }

    private void persistPatterns() {
        ScamScreenerRuntime.getInstance().config().safety().setMutePatterns(new ArrayList<>(compiledPatterns.keySet()));
        ScamScreenerRuntime.getInstance().saveConfig();
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null) {
            return "";
        }

        return pattern.trim();
    }

    /**
     * Result codes for mute-pattern insertion.
     */
    public enum AddResult {
        ADDED,
        ALREADY_EXISTS,
        INVALID
    }
}
