package eu.tango.scamscreener.blacklist;

import eu.tango.scamscreener.chat.trigger.TriggerContext;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.NotificationService;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class BlacklistAlertService {
	private final BlacklistManager blacklist;
	private final PlayerLookup playerLookup;
	private final DebugReporter debugReporter;
	private final BooleanSupplier autoLeaveEnabledSupplier;

	public BlacklistAlertService(
		BlacklistManager blacklist,
		PlayerLookup playerLookup,
		DebugReporter debugReporter,
		BooleanSupplier autoLeaveEnabledSupplier
	) {
		this.blacklist = blacklist;
		this.playerLookup = playerLookup;
		this.debugReporter = debugReporter;
		this.autoLeaveEnabledSupplier = autoLeaveEnabledSupplier;
	}

	public void checkTriggerAndWarn(String message, TriggerContext context) {
		if (message == null || message.isBlank() || context == null) {
			return;
		}
		String playerName = context.matchPlayerName(message);
		if (playerName == null) {
			return;
		}

		UUID uuid = playerLookup.findUuidByName(playerName);
		boolean blacklisted = blacklist.isBlacklisted(playerName, playerLookup::findUuidByName);
		debugReporter.debugTrade("blacklist trigger " + context.name().toLowerCase(Locale.ROOT) + " player=" + playerName + " blacklisted=" + blacklisted);
		if (!blacklisted) {
			return;
		}

		sendBlacklistWarning(playerName, uuid, context.triggerReason());
	}

	private void sendBlacklistWarning(String playerName, UUID uuid, String reason) {
		Minecraft client = Minecraft.getInstance();
		var player = client.player;
		if (player == null) {
			return;
		}

		BlacklistManager.ScamEntry entry = uuid == null ? null : blacklist.get(uuid);
		if (entry == null && playerName != null && !playerName.isBlank()) {
			entry = blacklist.findByName(playerName);
		}
		if (ScamRules.showBlacklistWarningMessage()) {
			player.displayClientMessage(Messages.blacklistWarning(playerName, reason, entry), false);
		}
		if (ScamRules.pingOnBlacklistWarning()) {
			NotificationService.playWarningTone();
		}
		if (autoLeaveEnabledSupplier != null && autoLeaveEnabledSupplier.getAsBoolean()) {
			MessageDispatcher.sendCommand("p leave");
			if (ScamRules.showAutoLeaveMessage()) {
				player.displayClientMessage(Messages.autoLeaveExecuted(playerName), false);
			}
			debugReporter.debugTrade("auto leave triggered for blacklisted player=" + playerName + " uuid=" + uuid);
		}
	}
}
