package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class MessageEventParser {
	private MessageEventParser() {
	}

	/**
	 * Parses a raw chat line into a {@link MessageEvent} if it matches a player chat format.
	 */
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
