package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.chat.mute.MutePatternManager;

import java.util.Optional;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class MuteStage {
	private final MutePatternManager mutePatternManager;

	/**
	 * Filters out messages that match user-defined mute patterns.
	 */
	public MuteStage(MutePatternManager mutePatternManager) {
		this.mutePatternManager = mutePatternManager;
	}

	/**
	 * Returns {@link Optional#empty()} if a message is blocked.
	 */
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
