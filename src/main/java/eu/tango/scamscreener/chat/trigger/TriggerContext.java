package eu.tango.scamscreener.chat.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	),
	COOP_JOIN_REQUEST(
		"co-op join request",
		Pattern.compile("^([A-Za-z0-9_]{3,16}) (?:has )?(?:requested|asks|asked) to join your (?:SkyBlock )?co-?op!?$", Pattern.CASE_INSENSITIVE)
	),
	COOP_INVITE_SENT(
		"invited to your co-op",
		Pattern.compile("^You invited ([A-Za-z0-9_]{3,16}) to your (?:SkyBlock )?co-?op!?$", Pattern.CASE_INSENSITIVE)
	),
	COOP_MEMBER_JOINED(
		"joined your SkyBlock Co-op",
		Pattern.compile("^([A-Za-z0-9_]{3,16}) joined your (?:SkyBlock )?co-?op!?$", Pattern.CASE_INSENSITIVE)
	);

	private final String triggerReason;
	private final Pattern pattern;
	private static final Logger LOGGER = LoggerFactory.getLogger(TriggerContext.class);

	TriggerContext(String triggerReason, Pattern pattern) {
		this.triggerReason = triggerReason;
		this.pattern = pattern;
	}

	public String matchPlayerName(String message) {
		if (message == null || message.isBlank()) {
			return null;
		}
		try {
			Matcher matcher = pattern.matcher(message);
			if (!matcher.matches()) {
				return null;
			}
			return matcher.group(1);
		} catch (StackOverflowError error) {
			LOGGER.warn("Skipped trigger regex due to StackOverflowError ({})", triggerReason);
			return null;
		}
	}

	public String triggerReason() {
		return triggerReason;
	}
}
