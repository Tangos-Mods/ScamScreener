package eu.tango.scamscreener.ui.messages;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class TrainingMessages extends StyledMessages {
	private TrainingMessages() {
	}

	public static MutableComponent noChatToCapture() {
		return prefixedGray("No chat line captured yet. Wait for a message, then run the capture command.");
	}

	public static MutableComponent trainingSampleSaved(String path, int label) {
		return prefixed()
			.append(gray("Saved training sample with label "))
			.append(darkRedBold(String.valueOf(label)))
			.append(gray(" to "))
			.append(clickablePath(path))
			.append(gray("."));
	}

	public static MutableComponent trainingSampleFlagged(String labelText) {
		return prefixed()
			.append(gray("Saved as "))
			.append(gold(safe(labelText, "unknown")))
			.append(gray("."));
	}

	public static MutableComponent trainingSamplesSaved(String path, int label, int count) {
		return prefixed()
			.append(gray("Saved "))
			.append(goldBold(String.valueOf(count)))
			.append(gray(" training samples with label "))
			.append(darkRedBold(String.valueOf(label)))
			.append(gray(" to "))
			.append(clickablePath(path))
			.append(gray("."));
	}

	public static MutableComponent trainingSaveFailed(String errorMessage) {
		return error("Failed to save training sample.", "TR-SAVE-001", errorMessage);
	}

	public static MutableComponent trainingSamplesSaveFailed(String errorMessage) {
		return error("Failed to save training samples.", "TR-SAVE-002", errorMessage);
	}

	public static MutableComponent trainingDataMigrated(int updatedRows) {
		return prefixed()
			.append(gray("Training data migrated. Updated "))
			.append(goldBold(String.valueOf(Math.max(0, updatedRows))))
			.append(gray(" rows."));
	}

	public static MutableComponent trainingDataUpToDate() {
		return prefixedGray("Training data already up to date.");
	}

	public static MutableComponent trainingCompleted(int sampleCount, int positiveCount, String archivedFilename) {
		return prefixed()
			.append(gray("Trained local AI model with "))
			.append(goldBold(String.valueOf(sampleCount)))
			.append(gray(" samples ("))
			.append(darkRedBold(String.valueOf(positiveCount)))
			.append(gray(" scam). Archived data to "))
			.append(yellow(safe(archivedFilename, "unknown")))
			.append(gray("."));
	}

	public static MutableComponent trainingUnigramsIgnored(int count) {
		int safe = Math.max(0, count);
		return prefixed()
			.append(gray("Ignored "))
			.append(goldBold(String.valueOf(safe)))
			.append(gray(" unigram training messages."));
	}

	public static MutableComponent trainingAlreadyRunning() {
		return prefixedGray("Training is already running.");
	}

	public static MutableComponent trainingFailed(String errorMessage) {
		return error("Training failed.", "TR-TRAIN-001", errorMessage);
	}

	public static MutableComponent localAiModelReset() {
		return prefixedGray("Local AI model was reset to default weights.");
	}

	private static MutableComponent clickablePath(String path) {
		String safePath = safe(path);
		return Component.literal(safePath).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.OpenFile(safePath))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open this file in Explorer")))
		);
	}
}

