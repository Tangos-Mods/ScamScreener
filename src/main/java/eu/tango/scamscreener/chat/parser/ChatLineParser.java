package eu.tango.scamscreener.chat.parser;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.tango.scamscreener.chat.IgnoredChatMessages;

public final class ChatLineParser {
	private static final String CHAT_PREFIX_PATTERN = "(?:(?:\\[[^\\]]+\\]|[^\\r\\n])\\s*)*";
	private static final Pattern DIRECT_CHAT_PATTERN = Pattern.compile(
		"^" + CHAT_PREFIX_PATTERN + "(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$"
	);
	private static final Pattern CHANNEL_CHAT_PATTERN = Pattern.compile(
		"^(?:party|guild|officer|team|co-?op|all|public)\\s*>\\s*" + CHAT_PREFIX_PATTERN + "(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern WHISPER_CHAT_PATTERN = Pattern.compile(
		"^(?:from|to|whisper from|whisper to)\\s+" + CHAT_PREFIX_PATTERN + "(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$",
		Pattern.CASE_INSENSITIVE
	);
	private static final Set<String> SYSTEM_LABELS = Set.of(
		"profile", "area", "server", "gems", "fairy", "essence", "wither",
		"cookie", "active", "upgrades", "collection", "dungeons", "players", "info",
		"rng", "meter", "other", "bank", "interest", "unclaimed", "scamscreener",
		"auction", "bazaar", "rewards", "party", "guild", "friend", "friends",
		"booster", "store", "profileviewer", "warning", "note", "tip", "announcement"
	);

	private ChatLineParser() {
	}

	public static ParsedPlayerLine parsePlayerLine(String rawLine) {
		if (rawLine == null || rawLine.isBlank()) {
			return null;
		}

		String cleaned = IgnoredChatMessages.clean(rawLine);
		if (IgnoredChatMessages.isSystemLineCleaned(cleaned)) {
			return null;
		}

		Matcher matcher = matchPlayerChat(cleaned);
		if (matcher == null) {
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

	public static boolean isSystemLine(String rawLine) {
		return IgnoredChatMessages.isSystemLine(rawLine);
	}

	private static Matcher matchPlayerChat(String cleaned) {
		Matcher direct = DIRECT_CHAT_PATTERN.matcher(cleaned);
		if (direct.matches()) {
			return direct;
		}
		Matcher channel = CHANNEL_CHAT_PATTERN.matcher(cleaned);
		if (channel.matches()) {
			return channel;
		}
		Matcher whisper = WHISPER_CHAT_PATTERN.matcher(cleaned);
		if (whisper.matches()) {
			return whisper;
		}
		return null;
	}

	public record ParsedPlayerLine(String playerName, String message) {
	}
}
