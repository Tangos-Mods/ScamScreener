package eu.tango.scamscreener.util;

import java.util.UUID;

public final class UuidUtil {
	private UuidUtil() {
	}

	public static UUID parse(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(input.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
