package eu.tango.scamscreener.security;

import java.util.regex.Pattern;

public final class DiscordSafety {
	private static final Pattern DISCORD_LINK_PATTERN = Pattern.compile("(https?://)?(www\\.)?(discord\\.gg|discord\\.com/invite)/[a-z0-9-]+", Pattern.CASE_INSENSITIVE);
	private final SafetyBypassStore store = new SafetyBypassStore(SafetyBypassStore.Kind.DISCORD_LINK, DISCORD_LINK_PATTERN);

	public SafetyBypassStore.BlockResult blockIfDiscordLink(String message, boolean isCommand) {
		return store.blockIfMatch(message, isCommand);
	}

	public SafetyBypassStore.Pending takePending(String id) {
		return store.takePending(id);
	}

	public void allowOnce(String message, boolean isCommand) {
		store.allowOnce(message, isCommand);
	}

	public boolean consumeAllowOnce(String message, boolean isCommand) {
		return store.consumeAllowOnce(message, isCommand);
	}
}
