package eu.tango.scamscreener.util;

public final class TextUtil {
	private TextUtil() {
	}

	public static String normalizeForMatch(String input) {
		if (input == null) {
			return "";
		}
		return input.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}

	public static String normalizeCommand(String input, boolean isCommand) {
		if (!isCommand || input == null) {
			return input;
		}
		String trimmed = input.trim();
		if (trimmed.startsWith("/")) {
			return trimmed.substring(1);
		}
		return trimmed;
	}
}
