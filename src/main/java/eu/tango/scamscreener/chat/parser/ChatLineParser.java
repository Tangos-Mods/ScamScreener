package eu.tango.scamscreener.chat.parser;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatLineParser {
	private static final Pattern CHAT_LINE_PATTERN = Pattern.compile("^.*?([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$");
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§.");
	private static final Set<String> SYSTEM_LABELS = Set.of(
		"profile", "area", "server", "gems", "fairy", "party", "essence", "wither",
		"cookie", "active", "upgrades", "collection", "dungeons", "players", "info",
		"rng", "meter", "other", "bank", "interest", "unclaimed", "scamscreener"
	);

	private ChatLineParser() {
	}

	public static ParsedPlayerLine parsePlayerLine(String rawLine) {
		if (rawLine == null || rawLine.isBlank()) {
			return null;
		}

		String cleaned = COLOR_CODE_PATTERN.matcher(rawLine).replaceAll("").trim();
		if (cleaned.startsWith("[ScamScreener]")) {
			return null;
		}

		Matcher matcher = CHAT_LINE_PATTERN.matcher(cleaned);
		if (!matcher.matches()) {
			return null;
		}

		String playerName = matcher.group(1);
		String message = matcher.group(2);
		if (playerName == null || message == null) {
			return null;
		}

		String normalizedName = playerName.trim().toLowerCase(Locale.ROOT);
		if (normalizedName.isBlank() || SYSTEM_LABELS.contains(normalizedName)) {
			return null;
		}

		String trimmedMessage = message.trim();
		if (trimmedMessage.isBlank()) {
			return null;
		}

		return new ParsedPlayerLine(playerName.trim(), trimmedMessage);
	}

	public record ParsedPlayerLine(String playerName, String message) {
	}
}
