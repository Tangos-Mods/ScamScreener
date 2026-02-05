package eu.tango.scamscreener.detect;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WarningDeduplicator {
	private final Set<String> seen = new HashSet<>();

	public boolean shouldWarn(MessageEvent event, DetectionLevel level) {
		if (event == null || event.playerName() == null || event.playerName().isBlank() || level == null) {
			return false;
		}
		String key = "behavior-risk:" + event.playerName().toLowerCase(Locale.ROOT) + ":" + level.name();
		return seen.add(key);
	}

	public void reset() {
		seen.clear();
	}
}
