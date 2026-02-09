package eu.tango.scamscreener.chat.trigger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TriggerContext {
	TRADE_INCOMING(
		"incoming trade request",
		Pattern.compile("^([A-Za-z0-9_]{3,16}) has sent you a trade request\\.?$")
	),
	TRADE_OUTGOING(
		"outgoing trade request",
		Pattern.compile("^You have sent a trade request to ([A-Za-z0-9_]{3,16})\\.?$")
	),
	TRADE_SESSION(
		"active trade session",
		Pattern.compile("^You are trading with ([A-Za-z0-9_]{3,16})\\.?$")
	),
	PARTY_WITH_CONFIRMATION(
		"party join confirmation",
		Pattern.compile("^You'll be partying with: ([A-Za-z0-9_]{3,16})\\.?$")
	),
	PARTY_FINDER_DUNGEON_JOIN(
		"joined your dungeon group via party finder",
		Pattern.compile("^Party Finder > ([A-Za-z0-9_]{3,16}) joined the dungeon group(?:!.*)?$")
	);

	private final String triggerReason;
	private final Pattern pattern;

	TriggerContext(String triggerReason, Pattern pattern) {
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

	public String triggerReason() {
		return triggerReason;
	}
}
