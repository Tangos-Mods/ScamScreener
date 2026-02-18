package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.util.TextUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

final class ChatEchoDeduplicator {
	private final long windowMillis;
	private final Map<String, Deque<Long>> pendingOutgoingEchoes = new HashMap<>();

	ChatEchoDeduplicator(long windowMillis) {
		this.windowMillis = Math.max(1_000L, windowMillis);
	}

	synchronized void rememberOutgoing(String speakerNameOrKey, String message, String channel, long timestampMs) {
		prune(timestampMs);
		String key = keyOf(speakerNameOrKey, message, channel);
		if (key.isBlank()) {
			return;
		}
		Deque<Long> timestamps = pendingOutgoingEchoes.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		timestamps.addLast(timestampMs);
		while (timestamps.size() > 8) {
			timestamps.removeFirst();
		}
	}

	synchronized boolean consumeIncomingEcho(String speakerNameOrKey, String message, String channel, long timestampMs) {
		prune(timestampMs);
		String key = keyOf(speakerNameOrKey, message, channel);
		if (key.isBlank()) {
			return false;
		}
		Deque<Long> timestamps = pendingOutgoingEchoes.get(key);
		if (timestamps == null || timestamps.isEmpty()) {
			return false;
		}
		while (!timestamps.isEmpty()) {
			Long outgoingTimestamp = timestamps.peekFirst();
			if (outgoingTimestamp == null || timestampMs - outgoingTimestamp > windowMillis) {
				timestamps.removeFirst();
				continue;
			}
			if (timestampMs < outgoingTimestamp) {
				return false;
			}
			timestamps.removeFirst();
			if (timestamps.isEmpty()) {
				pendingOutgoingEchoes.remove(key);
			}
			return true;
		}
		pendingOutgoingEchoes.remove(key);
		return false;
	}

	private void prune(long nowMs) {
		Iterator<Map.Entry<String, Deque<Long>>> iterator = pendingOutgoingEchoes.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Deque<Long>> entry = iterator.next();
			Deque<Long> timestamps = entry.getValue();
			if (timestamps == null || timestamps.isEmpty()) {
				iterator.remove();
				continue;
			}
			while (!timestamps.isEmpty()) {
				Long timestamp = timestamps.peekFirst();
				if (timestamp == null || nowMs - timestamp > windowMillis) {
					timestamps.removeFirst();
					continue;
				}
				break;
			}
			if (timestamps.isEmpty()) {
				iterator.remove();
			}
		}
	}

	private static String keyOf(String speakerNameOrKey, String message, String channel) {
		String speakerKey = speakerKeyOf(speakerNameOrKey);
		if (speakerKey.isBlank()) {
			return "";
		}
		String text = cleanMessage(message);
		if (text.isBlank()) {
			return "";
		}
		String safeChannel = channel == null || channel.isBlank() ? "unknown" : channel.trim().toLowerCase(Locale.ROOT);
		return speakerKey + "|" + safeChannel + "|" + text;
	}

	private static String speakerKeyOf(String speakerNameOrKey) {
		if (speakerNameOrKey == null || speakerNameOrKey.isBlank()) {
			return "";
		}
		String value = speakerNameOrKey.trim();
		if (value.startsWith("speaker-")) {
			return value;
		}
		return TextUtil.anonymizedSpeakerKey(value);
	}

	private static String cleanMessage(String message) {
		if (message == null) {
			return "";
		}
		return message.replace('\n', ' ').replace('\r', ' ').trim().toLowerCase(Locale.ROOT);
	}
}
