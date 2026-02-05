package eu.tango.scamscreener.pipeline.core;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class WarningDeduplicator {
	private final Set<String> seen = new HashSet<>();

	/**
	 * Ensures each player and risk level is only warned once per session.
	 */
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
