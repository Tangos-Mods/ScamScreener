package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.EducationMessages;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import eu.tango.scamscreener.pipeline.model.DetectionDecision;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class OutputStage {
	/**
	 * Emits the warning message and sound if the decision allows it.
	 */
	public void output(MessageEvent event, DetectionResult result, DetectionDecision decision, Consumer<Component> reply, Runnable warningSound) {
		if (decision == null || !decision.shouldWarn() || result == null) {
			return;
		}
		if (reply != null && ScamRules.showScamWarningMessage()) {
			reply.accept(Messages.behaviorRiskWarning(event == null ? null : event.playerName(), result));
			Component education = EducationMessages.followUpFor(result);
			if (education != null) {
				reply.accept(education);
			}
		}
		if (warningSound != null && ScamRules.pingOnScamWarning()) {
			warningSound.run();
		}
	}
}
