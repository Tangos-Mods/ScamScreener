package eu.tango.scamscreener.chat.parser;

import eu.tango.scamscreener.util.TextUtil;

import java.util.Locale;

public final class OutgoingChatCommandParser {
	private OutgoingChatCommandParser() {
	}

	public static ParsedOutgoingChat parse(String command) {
		String normalized = TextUtil.normalizeCommand(command, true);
		if (normalized == null || normalized.isBlank()) {
			return null;
		}

		String[] tokens = normalized.trim().split("\\s+");
		if (tokens.length < 1) {
			return null;
		}
		String head = tokens[0].toLowerCase(Locale.ROOT);
		if (head.isBlank()) {
			return null;
		}

		if (isPmCommand(head)) {
			return build(tokens, 2, "pm");
		}
		if ("r".equals(head) || "reply".equals(head)) {
			return build(tokens, 1, "pm");
		}
		if ("pc".equals(head) || "pchat".equals(head) || "partychat".equals(head)) {
			return build(tokens, 1, "party");
		}
		if ("party".equals(head) && hasSecond(tokens, "chat")) {
			return build(tokens, 2, "party");
		}
		if ("gc".equals(head) || "gchat".equals(head)) {
			return build(tokens, 1, "team");
		}
		if ("guild".equals(head) && hasSecond(tokens, "chat")) {
			return build(tokens, 2, "team");
		}
		if ("cc".equals(head) || "coopchat".equals(head) || "co-opchat".equals(head)) {
			return build(tokens, 1, "team");
		}
		if ("ac".equals(head)) {
			return build(tokens, 1, "public");
		}
		return null;
	}

	private static ParsedOutgoingChat build(String[] tokens, int messageStart, String channel) {
		if (tokens == null || tokens.length <= messageStart) {
			return null;
		}
		StringBuilder message = new StringBuilder();
		for (int i = messageStart; i < tokens.length; i++) {
			if (tokens[i] == null || tokens[i].isBlank()) {
				continue;
			}
			if (message.length() > 0) {
				message.append(' ');
			}
			message.append(tokens[i]);
		}
		if (message.length() == 0) {
			return null;
		}
		return new ParsedOutgoingChat(channel, message.toString());
	}

	private static boolean isPmCommand(String head) {
		return "msg".equals(head)
			|| "m".equals(head)
			|| "tell".equals(head)
			|| "w".equals(head)
			|| "whisper".equals(head);
	}

	private static boolean hasSecond(String[] tokens, String expected) {
		return tokens.length >= 2 && expected.equalsIgnoreCase(tokens[1]);
	}

	public record ParsedOutgoingChat(String channel, String message) {
	}
}
