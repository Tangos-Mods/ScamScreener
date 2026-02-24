package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.util.RegexSafety;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Pattern;

public final class MessageEventParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageEventParser.class);
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\\u00A7.");

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
		ChannelContext channelContext = detectChannelContext(rawLine);

		return MessageEvent.from(
			parsed.playerName(),
			parsed.message(),
			timestampMs,
			channelContext.context(),
			channelContext.channel()
		);
	}

	private static ChannelContext detectChannelContext(String rawLine) {
		if (rawLine == null || rawLine.isBlank()) {
			return new ChannelContext(MessageContext.UNKNOWN, "unknown");
		}

		String cleaned = RegexSafety.safePatternReplaceAll(
			COLOR_CODE_PATTERN,
			rawLine,
			"",
			LOGGER,
			"message event color stripping"
		).trim().toLowerCase(Locale.ROOT);
		if (cleaned.isBlank()) {
			return new ChannelContext(MessageContext.UNKNOWN, "unknown");
		}
		if (cleaned.startsWith("party >")) {
			return new ChannelContext(MessageContext.PARTY, "party");
		}
		if (cleaned.startsWith("guild >")
			|| cleaned.startsWith("officer >")
			|| cleaned.startsWith("team >")
			|| cleaned.startsWith("co-op >")
			|| cleaned.startsWith("coop >")) {
			return new ChannelContext(MessageContext.TEAM, "team");
		}
		if (cleaned.startsWith("from ") || cleaned.startsWith("to ")
			|| cleaned.startsWith("whisper from ") || cleaned.startsWith("whisper to ")) {
			return new ChannelContext(MessageContext.GENERAL, "pm");
		}
		return new ChannelContext(MessageContext.GENERAL, "public");
	}

	private record ChannelContext(MessageContext context, String channel) {
	}
}
