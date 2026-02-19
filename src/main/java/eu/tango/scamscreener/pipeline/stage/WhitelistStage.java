package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.whitelist.WhitelistManager;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class WhitelistStage {
	private final WhitelistManager whitelistManager;
	private final Function<String, UUID> uuidResolver;

	/**
	 * Filters out messages from players that are present in the whitelist.
	 */
	public WhitelistStage(WhitelistManager whitelistManager, Function<String, UUID> uuidResolver) {
		this.whitelistManager = whitelistManager;
		this.uuidResolver = uuidResolver;
	}

	/**
	 * Returns {@link Optional#empty()} when a message author is whitelisted.
	 */
	public Optional<MessageEvent> filter(MessageEvent event) {
		if (event == null) {
			return Optional.empty();
		}
		if (whitelistManager != null && whitelistManager.isWhitelisted(event.playerName(), uuidResolver)) {
			return Optional.empty();
		}
		return Optional.of(event);
	}
}
