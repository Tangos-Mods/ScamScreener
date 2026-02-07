package eu.tango.scamscreener.security;

import eu.tango.scamscreener.ui.messages.CommandMessages;

import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.util.TextUtil;

public final class OutgoingMessageGuard {
	private final SafetyBypassStore emailSafety;
	private final SafetyBypassStore discordSafety;

	public OutgoingMessageGuard(SafetyBypassStore emailSafety, SafetyBypassStore discordSafety) {
		this.emailSafety = emailSafety;
		this.discordSafety = discordSafety;
	}

	public boolean allowChat(String message) {
		if (message == null || message.isBlank()) {
			return true;
		}
		if (emailSafety.consumeAllowOnce(message, false) || discordSafety.consumeAllowOnce(message, false)) {
			return true;
		}
		SafetyBypassStore.BlockResult block = emailSafety.blockIfMatch(message, false);
		if (block == null) {
			block = discordSafety.blockIfMatch(message, false);
		}
		if (block == null) {
			return true;
		}
		MessageDispatcher.reply(block.kind() == SafetyBypassStore.Kind.DISCORD_LINK
			? CommandMessages.discordSafetyBlocked(block.id())
			: CommandMessages.emailSafetyBlocked(block.id()));
		return false;
	}

	public boolean allowCommand(String command) {
		if (command == null || command.isBlank()) {
			return true;
		}
		String normalized = TextUtil.normalizeCommand(command, true);
		if (emailSafety.consumeAllowOnce(normalized, true) || discordSafety.consumeAllowOnce(normalized, true)) {
			return true;
		}
		SafetyBypassStore.BlockResult block = emailSafety.blockIfMatch(normalized, true);
		if (block == null) {
			block = discordSafety.blockIfMatch(normalized, true);
		}
		if (block == null) {
			return true;
		}
		MessageDispatcher.reply(block.kind() == SafetyBypassStore.Kind.DISCORD_LINK
			? CommandMessages.discordSafetyBlocked(block.id())
			: CommandMessages.emailSafetyBlocked(block.id()));
		return false;
	}
}

