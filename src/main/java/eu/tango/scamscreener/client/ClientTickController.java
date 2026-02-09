package eu.tango.scamscreener.client;

import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.ui.FlaggingController;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.client.Minecraft;

public final class ClientTickController {
	private final FlaggingController flaggingController;
	private final MutePatternManager mutePatternManager;
	private final DetectionPipeline detectionPipeline;
	private final int legitLabel;
	private final int scamLabel;
	private boolean checkedModelUpdate;

	public ClientTickController(FlaggingController flaggingController,
		MutePatternManager mutePatternManager,
		DetectionPipeline detectionPipeline,
		int legitLabel,
		int scamLabel
	) {
		this.flaggingController = flaggingController;
		this.mutePatternManager = mutePatternManager;
		this.detectionPipeline = detectionPipeline;
		this.legitLabel = legitLabel;
		this.scamLabel = scamLabel;
	}

	public void onClientTick(Minecraft client, Runnable modelUpdateCheck) {
		flaggingController.updateHoveredFlagTarget(client);
		flaggingController.handleFlagKeybinds(client, legitLabel, scamLabel);
		if (client.player == null || client.getConnection() == null) {
			detectionPipeline.reset();
			checkedModelUpdate = false;
			return;
		}
		if (!checkedModelUpdate) {
			checkedModelUpdate = true;
			modelUpdateCheck.run();
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
