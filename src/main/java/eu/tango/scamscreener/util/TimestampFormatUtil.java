package eu.tango.scamscreener.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimestampFormatUtil {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	private TimestampFormatUtil() {
	}

	public static String formatIsoOrRaw(String input) {
		if (input == null || input.isBlank()) {
			return "n/a";
		}
		try {
			return TIMESTAMP_FORMATTER.format(Instant.parse(input));
		} catch (Exception ignored) {
			return input;
		}
	}
}
