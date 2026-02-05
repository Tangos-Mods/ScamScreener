package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.ui.Messages;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class OutputStage {
	public void output(MessageEvent event, DetectionResult result, DetectionDecision decision, Consumer<Component> reply, Runnable warningSound) {
		if (decision == null || !decision.shouldWarn() || result == null) {
			return;
		}
		if (reply != null) {
			reply.accept(Messages.behaviorRiskWarning(event == null ? null : event.playerName(), result));
		}
		if (warningSound != null) {
			warningSound.run();
		}
	}
}
