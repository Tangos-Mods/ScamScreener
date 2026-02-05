package eu.tango.scamscreener.pipeline.model;

import java.util.Locale;

public record MessageEvent(
	String playerName,
	String rawMessage,
	String normalizedMessage,
	long timestampMs,
	MessageContext context,
	String channel
) {
	public static MessageEvent from(String playerName, String rawMessage, long timestampMs, MessageContext context, String channel) {
		String normalized = normalizeMessage(rawMessage);
		return new MessageEvent(playerName, rawMessage, normalized, timestampMs, context == null ? MessageContext.UNKNOWN : context, channel);
	}

	public static String normalizeMessage(String message) {
		if (message == null) {
			return "";
		}
		return message.trim().toLowerCase(Locale.ROOT);
	}
}
