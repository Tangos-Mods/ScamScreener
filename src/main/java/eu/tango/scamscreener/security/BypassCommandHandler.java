package eu.tango.scamscreener.security;

import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.TextUtil;

public final class BypassCommandHandler {
	private final EmailSafety emailSafety;
	private final DiscordSafety discordSafety;
	private final CoopAddSafety coopAddSafety;

	public BypassCommandHandler(EmailSafety emailSafety, DiscordSafety discordSafety, CoopAddSafety coopAddSafety) {
		this.emailSafety = emailSafety;
		this.discordSafety = discordSafety;
		this.coopAddSafety = coopAddSafety;
	}

	public int handleEmailBypass(String id) {
		if (id == null || id.isBlank()) {
			MessageDispatcher.reply(Messages.emailBypassExpired());
			return 0;
		}
		SafetyBypassStore.Pending pending = emailSafety.takePending(id);
		if (pending == null) {
			pending = discordSafety.takePending(id);
		}
		if (pending == null && coopAddSafety != null) {
			pending = coopAddSafety.takePending(id);
		}
		if (pending == null || pending.message() == null || pending.message().isBlank()) {
			MessageDispatcher.reply(Messages.emailBypassExpired());
			return 0;
		}
		String message = TextUtil.normalizeCommand(pending.message(), pending.command());
		if (pending.kind() == SafetyBypassStore.Kind.DISCORD_LINK) {
			discordSafety.allowOnce(message, pending.command());
		} else if (pending.kind() == SafetyBypassStore.Kind.COOP_BLACKLIST && coopAddSafety != null) {
			coopAddSafety.allowOnce(message, pending.command());
		} else {
			emailSafety.allowOnce(message, pending.command());
		}
		if (pending.command()) {
			MessageDispatcher.sendCommand(message);
		} else {
			MessageDispatcher.sendChatMessage(message);
		}
		return 1;
	}
}
