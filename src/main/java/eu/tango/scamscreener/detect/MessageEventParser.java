package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.detection.ChatLineParser;

public final class MessageEventParser {
	private MessageEventParser() {
	}

	public static MessageEvent parse(String rawLine, long timestampMs) {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(rawLine);
		if (parsed == null) {
			return null;
		}

		return MessageEvent.from(
			parsed.playerName(),
			parsed.message(),
			timestampMs,
			MessageContext.UNKNOWN,
			null
		);
	}
}
