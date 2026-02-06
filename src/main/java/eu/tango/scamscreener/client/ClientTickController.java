package eu.tango.scamscreener.client;

import eu.tango.scamscreener.blacklist.BlacklistAlertService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.ui.FlaggingController;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClientTickController {
	private final FlaggingController flaggingController;
	private final MutePatternManager mutePatternManager;
	private final DetectionPipeline detectionPipeline;
	private final BlacklistManager blacklistManager;
	private final BlacklistAlertService blacklistAlertService;
	private final PlayerLookup playerLookup;
	private final Set<UUID> currentlyDetected;
	private final Set<String> warnedContexts;
	private final int legitLabel;
	private final int scamLabel;
	private boolean checkedModelUpdate;

	public ClientTickController(FlaggingController flaggingController,
		MutePatternManager mutePatternManager,
		DetectionPipeline detectionPipeline,
		BlacklistManager blacklistManager,
		BlacklistAlertService blacklistAlertService,
		PlayerLookup playerLookup,
		Set<UUID> currentlyDetected,
		Set<String> warnedContexts,
		int legitLabel,
		int scamLabel
	) {
		this.flaggingController = flaggingController;
		this.mutePatternManager = mutePatternManager;
		this.detectionPipeline = detectionPipeline;
		this.blacklistManager = blacklistManager;
		this.blacklistAlertService = blacklistAlertService;
		this.playerLookup = playerLookup;
		this.currentlyDetected = currentlyDetected;
		this.warnedContexts = warnedContexts;
		this.legitLabel = legitLabel;
		this.scamLabel = scamLabel;
	}

	public void onClientTick(Minecraft client, Runnable modelUpdateCheck) {
		flaggingController.updateHoveredFlagTarget(client);
		flaggingController.handleFlagKeybinds(client, legitLabel, scamLabel);
		if (client.player == null || client.getConnection() == null) {
			currentlyDetected.clear();
			warnedContexts.clear();
			detectionPipeline.reset();
			checkedModelUpdate = false;
			return;
		}
		if (!checkedModelUpdate) {
			checkedModelUpdate = true;
			modelUpdateCheck.run();
		}

		maybeNotifyBlockedMessages(client);

		if (blacklistManager.isEmpty()) {
			currentlyDetected.clear();
			return;
		}

		Team ownTeam = client.player.getTeam();
		if (ownTeam == null) {
			currentlyDetected.clear();
			return;
		}

		Set<UUID> detectedNow = new HashSet<>();
		for (PlayerInfo entry : playerLookup.onlinePlayers()) {
			String playerName = entry.getProfile().name();
			UUID playerUuid = entry.getProfile().id();
			if (!blacklistManager.contains(playerUuid)) {
				continue;
			}

			Team otherTeam = entry.getTeam();
			if (otherTeam == null) {
				continue;
			}

			if (!ownTeam.getName().equals(otherTeam.getName())) {
				continue;
			}

			detectedNow.add(playerUuid);
			if (!currentlyDetected.contains(playerUuid)) {
				blacklistAlertService.sendWarning(playerName, playerUuid);
			}
		}

		currentlyDetected.clear();
		currentlyDetected.addAll(detectedNow);
	}

	private void maybeNotifyBlockedMessages(Minecraft client) {
		long now = System.currentTimeMillis();
		if (!mutePatternManager.shouldNotifyNow(now)) {
			return;
		}

		int blocked = mutePatternManager.consumeBlockedCount(now);
		if (blocked <= 0 || client.player == null) {
			return;
		}
		client.player.displayClientMessage(Messages.blockedMessagesSummary(blocked, mutePatternManager.notifyIntervalSeconds()), false);
	}
}
