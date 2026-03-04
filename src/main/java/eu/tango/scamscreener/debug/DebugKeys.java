package eu.tango.scamscreener.debug;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small shared registry for local debug toggles.
 */
public final class DebugKeys {
    private static final List<String> KEYS = List.of("updater", "trade", "mute", "chatcolor");

    private DebugKeys() {
    }

    /**
     * Returns the stable ordered debug keys.
     *
     * @return the configured debug keys
     */
    public static List<String> keys() {
        return KEYS;
    }

    /**
     * Normalizes a free-form debug key.
     *
     * @param key the raw key
     * @return the normalized key, or an empty string
     */
    public static String normalize(String key) {
        if (key == null) {
            return "";
        }

        return key.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a readable label for one debug key.
     *
     * @param key the debug key
     * @return the display label
     */
    public static String label(String key) {
        return switch (normalize(key)) {
            case "updater" -> "Updater";
            case "trade" -> "Trade";
            case "mute" -> "Mute";
            case "chatcolor" -> "Chat Color";
            default -> key == null ? "" : key.trim();
        };
    }

    /**
     * Applies defaults to a persisted debug-state map.
     *
     * @param values the persisted values
     * @return a normalized map containing every known key
     */
    public static Map<String, Boolean> withDefaults(Map<String, Boolean> values) {
        Map<String, Boolean> normalizedValues = new LinkedHashMap<>();
        for (String key : KEYS) {
            boolean enabled = values != null && Boolean.TRUE.equals(values.get(normalize(key)));
            normalizedValues.put(key, enabled);
        }

        return normalizedValues;
    }
}
