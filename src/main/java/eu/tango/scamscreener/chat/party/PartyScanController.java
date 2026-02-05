package eu.tango.scamscreener.chat.party;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyScanController {
	private static final Pattern ATTEMPTING_TO_JOIN_PARTY_PATTERN = Pattern.compile("^Attempting to add you to the party\\.\\.\\.$");
	private static final Pattern PARTY_FINDER_JOIN_PATTERN = Pattern.compile("^Party Finder > ([A-Za-z0-9_]{3,16}) joined the dungeon group!.*$");
	private static final Pattern LEFT_PARTY_PATTERN = Pattern.compile("^You left the party\\.$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENTERED_DUNGEON_PATTERN = Pattern.compile("^.* entered The Catacombs, Floor (I|II|III|IV|V|VI|VII)!$");

	private final int scanIntervalTicks;
	private boolean partyScanActive;
	private int ticksSinceStart;

	public PartyScanController(int scanIntervalTicks) {
		this.scanIntervalTicks = Math.max(1, scanIntervalTicks);
	}

	public void onTick() {
		ticksSinceStart++;
	}

	public void reset() {
		partyScanActive = false;
		ticksSinceStart = 0;
	}

	public boolean shouldScanPartyTab() {
		return partyScanActive && ticksSinceStart % scanIntervalTicks == 0;
	}

	public boolean updateStateFromMessage(String message, String ownPlayerName) {
		if (message == null || message.isBlank()) {
			return false;
		}

		if (ATTEMPTING_TO_JOIN_PARTY_PATTERN.matcher(message).matches()) {
			boolean changed = !partyScanActive;
			partyScanActive = true;
			return changed;
		}

		Matcher joinMatcher = PARTY_FINDER_JOIN_PATTERN.matcher(message);
		if (!partyScanActive && joinMatcher.matches() && ownPlayerName != null && joinMatcher.group(1).equalsIgnoreCase(ownPlayerName)) {
			partyScanActive = true;
			return true;
		}

		if (partyScanActive && (LEFT_PARTY_PATTERN.matcher(message).matches() || ENTERED_DUNGEON_PATTERN.matcher(message).matches())) {
			partyScanActive = false;
			return true;
		}

		return false;
	}
}
