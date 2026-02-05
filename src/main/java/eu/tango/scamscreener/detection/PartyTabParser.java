package eu.tango.scamscreener.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyTabParser {
	private static final Pattern MC_COLOR_CODE = Pattern.compile("(?i)§[0-9A-FK-OR]");
	private static final Pattern BRACKET_PREFIX = Pattern.compile("^\\s*\\[[^\\]]*\\]\\s*");
	private static final Pattern USERNAME_SCAN_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])[A-Za-z0-9_]{3,16}(?![A-Za-z0-9_])");
	private static final int MAX_PARTY_LINES = 5;

	public List<String> extractPartyMembers(String tabFooterText) {
		if (tabFooterText == null || tabFooterText.isBlank()) {
			return List.of();
		}

		String[] lines = tabFooterText.split("\\R");
		int partyLine = findPartyLine(lines);
		if (partyLine < 0) {
			return List.of();
		}

		String partyLineText = lines[partyLine].trim().toLowerCase(Locale.ROOT);
		if (partyLineText.contains("no party")) {
			return List.of();
		}

		List<String> members = new ArrayList<>();
		for (int i = partyLine + 1; i < lines.length && i <= partyLine + MAX_PARTY_LINES; i++) {
			String rawLine = lines[i] == null ? "" : lines[i].trim();
			if (rawLine.isEmpty()) {
				break;
			}

			String memberName = extractNameFromLine(rawLine);
			if (memberName != null) {
				members.add(memberName);
			}
		}
		return members;
	}

	private static int findPartyLine(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			String normalized = lines[i] == null ? "" : lines[i].trim().toLowerCase(Locale.ROOT);
			if (normalized.startsWith("party:")) {
				return i;
			}
		}
		return -1;
	}

	private static String extractNameFromLine(String rawLine) {
		String cleaned = MC_COLOR_CODE.matcher(rawLine).replaceAll("");
		cleaned = BRACKET_PREFIX.matcher(cleaned).replaceFirst("");
		cleaned = cleaned.replaceAll("[^A-Za-z0-9_\\s]", " ");
		cleaned = cleaned.trim();
		if (cleaned.isEmpty()) {
			return null;
		}

		Matcher matcher = USERNAME_SCAN_PATTERN.matcher(cleaned);
		while (matcher.find()) {
			String candidate = matcher.group();
			if (candidate != null && !candidate.isBlank()) {
				return candidate;
			}
		}
		return null;
	}
}
