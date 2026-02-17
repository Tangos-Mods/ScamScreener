package eu.tango.scamscreener.chat;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class IgnoredChatMessages {
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\\u00A7.");
	private static final List<String> SYSTEM_PREFIXES = List.of(
		"[scamscreener]",
		"[npc]",
		"[security]",
		"[hypixel]"
	);
	private static final List<Pattern> SYSTEM_MESSAGE_PATTERNS = List.of(
		Pattern.compile("^you'?ll be partying with: [A-Za-z0-9_]{3,16}\\.?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^party finder > [A-Za-z0-9_]{3,16} joined the dungeon group(?:!.*)?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^[A-Za-z0-9_]{3,16} has sent you a trade request\\.?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^you have sent a trade request to [A-Za-z0-9_]{3,16}\\.?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^you are trading with [A-Za-z0-9_]{3,16}\\.?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile(
			"^[A-Za-z0-9_]{3,16} (?:has )?(?:requested|asks|asked) to join your (?:skyblock )?co-?op!?$",
			Pattern.CASE_INSENSITIVE
		),
		Pattern.compile("^you invited [A-Za-z0-9_]{3,16} to your (?:skyblock )?co-?op!?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^[A-Za-z0-9_]{3,16} joined your (?:skyblock )?co-?op!?$", Pattern.CASE_INSENSITIVE),
		Pattern.compile(
			"^actions\\s*:\\s*\\[legit\\].*\\[scam\\].*\\[blacklist\\].*$",
			Pattern.CASE_INSENSITIVE
		),
		Pattern.compile("^latest\\s+update\\s*:\\s*.+$", Pattern.CASE_INSENSITIVE),
		Pattern.compile("^update\\s*:\\s*.*\\b(?:click|v\\d+\\.\\d+\\.\\d+)\\b.*$", Pattern.CASE_INSENSITIVE)
	);
	private static final List<String> MUTE_EXEMPT_PREFIXES = List.of("[scamscreener]", "====================================");
	private static final List<String> MUTE_EXEMPT_CONTAINS = List.of("risky message");

	private IgnoredChatMessages() {
	}

	public static String clean(String rawLine) {
		if (rawLine == null || rawLine.isBlank()) {
			return "";
		}
		return COLOR_CODE_PATTERN.matcher(rawLine).replaceAll("").trim();
	}

	public static boolean isSystemLine(String rawLine) {
		return isSystemLineCleaned(clean(rawLine));
	}

	public static boolean isMuteExemptLine(String rawLine) {
		String cleaned = clean(rawLine);
		if (cleaned.isBlank()) {
			return false;
		}
		if (startsWithAny(cleaned, MUTE_EXEMPT_PREFIXES)) {
			return true;
		}
		String normalized = cleaned.toLowerCase(Locale.ROOT);
		for (String snippet : MUTE_EXEMPT_CONTAINS) {
			if (normalized.contains(snippet)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSystemLineCleaned(String cleanedLine) {
		if (cleanedLine == null || cleanedLine.isBlank()) {
			return false;
		}
		if (startsWithAny(cleanedLine, SYSTEM_PREFIXES)) {
			return true;
		}
		for (Pattern pattern : SYSTEM_MESSAGE_PATTERNS) {
			if (pattern.matcher(cleanedLine).matches()) {
				return true;
			}
		}
		return false;
	}

	private static boolean startsWithAny(String value, List<String> prefixes) {
		String normalized = value.toLowerCase(Locale.ROOT);
		for (String prefix : prefixes) {
			if (normalized.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
