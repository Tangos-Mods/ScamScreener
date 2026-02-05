package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.detection.MutePatternManager;

import java.util.Optional;

public final class MuteStage {
	private final MutePatternManager mutePatternManager;

	public MuteStage(MutePatternManager mutePatternManager) {
		this.mutePatternManager = mutePatternManager;
	}

	public Optional<MessageEvent> filter(MessageEvent event) {
		if (event == null) {
			return Optional.empty();
		}
		if (mutePatternManager != null && mutePatternManager.shouldBlock(event.rawMessage())) {
			return Optional.empty();
		}
		return Optional.of(event);
	}
}
