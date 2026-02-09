package eu.tango.scamscreener.security;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.lookup.PlayerLookup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CoopAddSafety {
	private static final Pattern COOP_ADD_PATTERN = Pattern.compile(
		"^coopadd\\s+([A-Za-z0-9_]{3,16})(?:\\s+.*)?$",
		Pattern.CASE_INSENSITIVE
	);

	private final BlacklistManager blacklist;
	private final PlayerLookup playerLookup;
	private final SafetyBypassStore store = new SafetyBypassStore(SafetyBypassStore.Kind.COOP_BLACKLIST, COOP_ADD_PATTERN);

	public CoopAddSafety(BlacklistManager blacklist, PlayerLookup playerLookup) {
		this.blacklist = blacklist;
		this.playerLookup = playerLookup;
	}

	public CoopBlockResult blockIfCoopAdd(String normalizedCommand) {
		if (normalizedCommand == null || normalizedCommand.isBlank()) {
			return null;
		}

		String trimmed = normalizedCommand.trim();
		Matcher matcher = COOP_ADD_PATTERN.matcher(trimmed);
		if (!matcher.matches()) {
			return null;
		}

		String playerName = matcher.group(1);
		if (playerName == null || playerName.isBlank()) {
			return null;
		}

		boolean blacklisted = blacklist != null
			&& playerLookup != null
			&& blacklist.isBlacklisted(playerName, playerLookup::findUuidByName);

		SafetyBypassStore.BlockResult blocked = store.blockIfMatch(trimmed, true);
		if (blocked == null) {
			return null;
		}
		return new CoopBlockResult(playerName, blocked.id(), blacklisted);
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

	public record CoopBlockResult(String playerName, String bypassId, boolean blacklisted) {
	}
}
