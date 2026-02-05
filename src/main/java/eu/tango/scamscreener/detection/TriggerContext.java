package eu.tango.scamscreener.detection;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TriggerContext {
	TRADE_INCOMING(
		"trade-incoming",
		"incoming trade request",
		Pattern.compile("^([A-Za-z0-9_]{3,16}) has sent you a trade request\\.?$")
	),
	TRADE_OUTGOING(
		"trade-outgoing",
		"outgoing trade request",
		Pattern.compile("^You have sent a trade request to ([A-Za-z0-9_]{3,16})\\.?$")
	),
	TRADE_SESSION(
		"trade-session",
		"active trade session",
		Pattern.compile("^You are trading with ([A-Za-z0-9_]{3,16})\\.?$")
	),
	PARTY_FINDER_JOIN(
		"party-finder-join",
		"joined your party finder group",
		Pattern.compile("^Party Finder > ([A-Za-z0-9_]{3,16}) joined the dungeon group!.*$")
	);

	private final String dedupePrefix;
	private final String triggerReason;
	private final Pattern pattern;

	TriggerContext(String dedupePrefix, String triggerReason, Pattern pattern) {
		this.dedupePrefix = dedupePrefix;
		this.triggerReason = triggerReason;
		this.pattern = pattern;
	}

	public String matchPlayerName(String message) {
		Matcher matcher = pattern.matcher(message);
		if (!matcher.matches()) {
			return null;
		}
		return matcher.group(1);
	}

	public String dedupeKey(UUID uuid) {
		return dedupePrefix + ":" + uuid;
	}

	public String triggerReason() {
		return triggerReason;
	}
}
