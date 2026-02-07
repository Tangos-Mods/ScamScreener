package eu.tango.scamscreener.blacklist;

import eu.tango.scamscreener.ui.messages.RiskMessages;

import eu.tango.scamscreener.chat.trigger.TriggerContext;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.NotificationService;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class BlacklistAlertService {
	private final BlacklistManager blacklist;
	private final PlayerLookup playerLookup;
	private final Set<String> warnedContexts;
	private final DebugReporter debugReporter;

	public BlacklistAlertService(BlacklistManager blacklist, PlayerLookup playerLookup, Set<String> warnedContexts, DebugReporter debugReporter) {
		this.blacklist = blacklist;
		this.playerLookup = playerLookup;
		this.warnedContexts = warnedContexts;
		this.debugReporter = debugReporter;
	}

	public void checkTriggerAndWarn(String message, TriggerContext context) {
		String playerName = context.matchPlayerName(message);
		if (playerName == null) {
			return;
		}

		UUID uuid = playerLookup.findUuidByName(playerName);
		boolean isBlacklisted = uuid != null && blacklist.contains(uuid);
		debugReporter.debugTrade("trade trigger " + context.name().toLowerCase(Locale.ROOT) + " player=" + playerName + " blacklisted=" + isBlacklisted);

		String dedupeKey = "trigger:" + context.name().toLowerCase(Locale.ROOT) + ":" + (uuid != null ? uuid : playerName.toLowerCase(Locale.ROOT));
		if (!warnedContexts.add(dedupeKey)) {
			debugReporter.debugTrade("trade trigger already warned: " + playerName);
			return;
		}

		sendBlacklistWarning(playerName, uuid, context.triggerReason());
	}

	public void sendWarning(String playerName, UUID uuid) {
		sendBlacklistWarning(playerName, uuid, "is in your team");
	}

	private void sendBlacklistWarning(String playerName, UUID uuid, String reason) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		BlacklistManager.ScamEntry entry = blacklist.get(uuid);
		client.player.displayClientMessage(RiskMessages.blacklistWarning(playerName, reason, entry), false);
		NotificationService.playWarningTone();
	}
}

