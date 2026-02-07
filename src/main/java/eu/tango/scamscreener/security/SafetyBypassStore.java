package eu.tango.scamscreener.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SafetyBypassStore {
	private static final int MAX_PENDING = 5;
	private static final int MAX_ALLOW_ONCE = 5;
	private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b", Pattern.CASE_INSENSITIVE);
	private static final Pattern DISCORD_LINK_PATTERN = Pattern.compile("(https?://)?(www\\.)?(discord\\.gg|discord\\.com/invite)/[a-z0-9-]+", Pattern.CASE_INSENSITIVE);

	private final Kind kind;
	private final Pattern pattern;
	private final Map<String, Pending> pending = new LinkedHashMap<>();
	private final Deque<String> allowChatOnce = new ArrayDeque<>();
	private final Deque<String> allowCommandOnce = new ArrayDeque<>();

	public SafetyBypassStore(Kind kind, Pattern pattern) {
		this.kind = kind;
		this.pattern = pattern;
	}

	public static SafetyBypassStore emailStore() {
		return new SafetyBypassStore(Kind.EMAIL, EMAIL_PATTERN);
	}

	public static SafetyBypassStore discordLinkStore() {
		return new SafetyBypassStore(Kind.DISCORD_LINK, DISCORD_LINK_PATTERN);
	}

	public BlockResult blockIfMatch(String message, boolean isCommand) {
		if (message == null || message.isBlank()) {
			return null;
		}
		if (!pattern.matcher(message).find()) {
			return null;
		}
		String id = UUID.randomUUID().toString().replace("-", "");
		pending.put(id, new Pending(kind, isCommand, message));
		trimPending();
		return new BlockResult(id, kind);
	}

	public Pending takePending(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		return pending.remove(id);
	}

	public void allowOnce(String message, boolean isCommand) {
		if (message == null || message.isBlank()) {
			return;
		}
		Deque<String> queue = isCommand ? allowCommandOnce : allowChatOnce;
		queue.addLast(message);
		while (queue.size() > MAX_ALLOW_ONCE) {
			queue.removeFirst();
		}
	}

	public boolean consumeAllowOnce(String message, boolean isCommand) {
		if (message == null || message.isBlank()) {
			return false;
		}
		Deque<String> queue = isCommand ? allowCommandOnce : allowChatOnce;
		return queue.remove(message);
	}

	private void trimPending() {
		while (pending.size() > MAX_PENDING) {
			String oldest = pending.keySet().iterator().next();
			pending.remove(oldest);
		}
	}

	public record Pending(Kind kind, boolean command, String message) {
	}

	public record BlockResult(String id, Kind kind) {
	}

	public enum Kind {
		EMAIL,
		DISCORD_LINK
	}
}
