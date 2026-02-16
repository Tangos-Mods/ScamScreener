package eu.tango.scamscreener.client;

import eu.tango.scamscreener.ai.TrainingUploadReminderService;
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
	private final TrainingUploadReminderService trainingUploadReminderService;
	private boolean checkedModelUpdate;
	private boolean openSettingsRequested;

	public ClientTickController(MutePatternManager mutePatternManager,
		DetectionPipeline detectionPipeline,
		Runnable openSettingsAction,
		LocationService locationService,
		TrainingUploadReminderService trainingUploadReminderService
	) {
		this.mutePatternManager = mutePatternManager;
		this.detectionPipeline = detectionPipeline;
		this.openSettingsAction = openSettingsAction;
		this.locationService = locationService;
		this.trainingUploadReminderService = trainingUploadReminderService;
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
			if (trainingUploadReminderService != null) {
				trainingUploadReminderService.reset();
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
		maybeNotifyTrainingUploadReminder(client);
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

	private void maybeNotifyTrainingUploadReminder(Minecraft client) {
		if (trainingUploadReminderService == null || client.player == null) {
			return;
		}
		TrainingUploadReminderService.ReminderDecision decision = trainingUploadReminderService.check(System.currentTimeMillis());
		if (!decision.shouldNotify()) {
			return;
		}
		client.player.displayClientMessage(Messages.trainingDataLargeUploadReminder(decision.entryCount()), false);
	}
}
