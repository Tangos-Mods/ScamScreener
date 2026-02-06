package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.config.DebugConfig;

public final class DebugReporter {
	private final DebugConfig debugConfig;

	public DebugReporter(DebugConfig debugConfig) {
		this.debugConfig = debugConfig;
	}

	public void debugTrade(String message) {
		if (!debugConfig.isEnabled("trade")) {
			return;
		}
		MessageDispatcher.reply(DebugMessages.debug("Trade", message));
	}

	public void debugMute(String message) {
		if (!debugConfig.isEnabled("mute")) {
			return;
		}
		MessageDispatcher.reply(DebugMessages.debug("Mute", message));
	}

	public void debugChatColor(String message) {
		if (!debugConfig.isEnabled("chatcolor")) {
			return;
		}
		MessageDispatcher.reply(DebugMessages.debug("ChatColor", message));
	}
}
