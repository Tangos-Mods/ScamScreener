package eu.tango.scamscreener.security;

import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.TextUtil;

public final class OutgoingMessageGuard {
	private final EmailSafety emailSafety;
	private final DiscordSafety discordSafety;
	private final CoopAddSafety coopAddSafety;

	public OutgoingMessageGuard(EmailSafety emailSafety, DiscordSafety discordSafety, CoopAddSafety coopAddSafety) {
		this.emailSafety = emailSafety;
		this.discordSafety = discordSafety;
		this.coopAddSafety = coopAddSafety;
	}

	public boolean allowChat(String message) {
		if (message == null || message.isBlank()) {
			return true;
		}
		if (emailSafety.consumeAllowOnce(message, false) || discordSafety.consumeAllowOnce(message, false)) {
			return true;
		}
		SafetyBypassStore.BlockResult block = emailSafety.blockIfEmail(message, false);
		if (block == null) {
			block = discordSafety.blockIfDiscordLink(message, false);
		}
		if (block == null) {
			return true;
		}
		MessageDispatcher.reply(block.kind() == SafetyBypassStore.Kind.DISCORD_LINK
			? Messages.discordSafetyBlocked(block.id())
			: Messages.emailSafetyBlocked(block.id()));
		return false;
	}

	public boolean allowCommand(String command) {
		if (command == null || command.isBlank()) {
			return true;
		}
		String normalized = TextUtil.normalizeCommand(command, true);
		if (emailSafety.consumeAllowOnce(normalized, true)
			|| discordSafety.consumeAllowOnce(normalized, true)
			|| (coopAddSafety != null && coopAddSafety.consumeAllowOnce(normalized, true))) {
			return true;
		}
		CoopAddSafety.CoopBlockResult coopBlocked = coopAddSafety == null ? null : coopAddSafety.blockIfCoopAdd(normalized);
		if (coopBlocked != null) {
			MessageDispatcher.reply(Messages.coopAddSafetyBlocked(
				coopBlocked.playerName(),
				coopBlocked.bypassId(),
				coopBlocked.blacklisted()
			));
			return false;
		}
		SafetyBypassStore.BlockResult block = emailSafety.blockIfEmail(normalized, true);
		if (block == null) {
			block = discordSafety.blockIfDiscordLink(normalized, true);
		}
		if (block == null) {
			return true;
		}
		MessageDispatcher.reply(block.kind() == SafetyBypassStore.Kind.DISCORD_LINK
			? Messages.discordSafetyBlocked(block.id())
			: Messages.emailSafetyBlocked(block.id()));
		return false;
	}
}
