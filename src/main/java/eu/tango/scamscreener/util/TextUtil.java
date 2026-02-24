package eu.tango.scamscreener.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TextUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(TextUtil.class);
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\\u00A7.");
	private static final Pattern AT_NAME_PATTERN = Pattern.compile("@[A-Za-z0-9_]{3,16}");
	private static final Pattern COMMAND_TARGET_PATTERN = Pattern.compile(
		"(?i)((?:/msg|/w|/tell|/party\\s+invite|/p\\s+invite|/f\\s+add|/coopadd|/visit)\\s+)([A-Za-z0-9_]{3,16})\\b"
	);

	private TextUtil() {
	}

	public static String normalizeForMatch(String input) {
		if (input == null) {
			return "";
		}
		String lowered = input.toLowerCase(Locale.ROOT);
		return RegexSafety.safeStringReplaceAll(lowered, "[^a-z0-9]+", " ", LOGGER, "normalizeForMatch").trim();
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

	public static String anonymizeForAi(String input, String playerNameHint) {
		if (input == null || input.isBlank()) {
			return "";
		}
		String sanitized = RegexSafety.safePatternReplaceAll(COLOR_CODE_PATTERN, input, " ", LOGGER, "color code stripping");
		sanitized = RegexSafety.safePatternReplaceAll(AT_NAME_PATTERN, sanitized, "@player", LOGGER, "mention anonymization");
		sanitized = RegexSafety.safePatternReplaceAll(COMMAND_TARGET_PATTERN, sanitized, "$1player", LOGGER, "command target anonymization");

		if (playerNameHint != null && !playerNameHint.isBlank()) {
			String escaped = Pattern.quote(playerNameHint.trim());
			Pattern hintPattern = Pattern.compile("(?i)\\b" + escaped + "\\b");
			sanitized = RegexSafety.safePatternReplaceAll(hintPattern, sanitized, "player", LOGGER, "speaker hint anonymization");
		}
		return RegexSafety.safeStringReplaceAll(sanitized, "\\s+", " ", LOGGER, "whitespace normalization").trim();
	}

	public static String anonymizedSpeakerKey(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return "speaker-unknown";
		}
		String normalized = playerName.trim().toLowerCase(Locale.ROOT);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
			return "speaker-" + toHex(hashed, 8);
		} catch (NoSuchAlgorithmException ignored) {
			return "speaker-" + Integer.toUnsignedString(normalized.hashCode(), 36);
		}
	}

	private static String toHex(byte[] bytes, int maxBytes) {
		StringBuilder out = new StringBuilder(maxBytes * 2);
		int length = Math.min(maxBytes, bytes.length);
		for (int i = 0; i < length; i++) {
			out.append(String.format(Locale.ROOT, "%02x", bytes[i]));
		}
		return out.toString();
	}

}
