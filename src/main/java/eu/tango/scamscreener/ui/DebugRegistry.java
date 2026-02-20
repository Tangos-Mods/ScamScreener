package eu.tango.scamscreener.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DebugRegistry {
	private static final List<String> KEYS = List.of("updater", "trade", "market", "mute", "chatcolor");

	private DebugRegistry() {
	}

	public static List<String> keys() {
		return KEYS;
	}

	public static String normalize(String key) {
		return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
	}

	public static Map<String, Boolean> withDefaults(Map<String, Boolean> states) {
		Map<String, Boolean> out = new LinkedHashMap<>();
		if (states != null) {
			for (Map.Entry<String, Boolean> entry : states.entrySet()) {
				String normalized = normalize(entry.getKey());
				if (!normalized.isBlank()) {
					out.put(normalized, Boolean.TRUE.equals(entry.getValue()));
				}
			}
		}
		for (String key : KEYS) {
			out.putIfAbsent(key, false);
		}
		return out;
	}
}
