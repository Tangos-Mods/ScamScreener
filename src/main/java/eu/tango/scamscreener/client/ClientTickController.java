package eu.tango.scamscreener.client;

import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.location.LocationService;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.client.Minecraft;

public final class ClientTickController {
	private final MutePatternManager mutePatternManager;
	private final DetectionPipeline detectionPipeline;
	private final Runnable openSettingsAction;
	private final LocationService locationService;
	private boolean checkedModelUpdate;
	private boolean openSettingsRequested;

	public ClientTickController(MutePatternManager mutePatternManager,
		DetectionPipeline detectionPipeline,
		Runnable openSettingsAction,
		LocationService locationService
	) {
		this.mutePatternManager = mutePatternManager;
		this.detectionPipeline = detectionPipeline;
		this.openSettingsAction = openSettingsAction;
		this.locationService = locationService;
	}

	public void requestOpenSettings() {
		openSettingsRequested = true;
	}

	public void onClientTick(Minecraft client, Runnable modelUpdateCheck) {
		if (openSettingsRequested) {
			openSettingsRequested = false;
			if (openSettingsAction != null) {
				openSettingsAction.run();
			}
		}

		if (client.player == null || client.getConnection() == null) {
			detectionPipeline.reset();
			if (locationService != null) {
				locationService.reset();
			}
			checkedModelUpdate = false;
			return;
		}
		if (!checkedModelUpdate) {
			checkedModelUpdate = true;
			modelUpdateCheck.run();
		}
		if (locationService != null) {
			locationService.onClientTick(client);
		}

		maybeNotifyBlockedMessages(client);
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
