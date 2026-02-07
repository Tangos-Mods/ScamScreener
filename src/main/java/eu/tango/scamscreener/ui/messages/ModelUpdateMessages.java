package eu.tango.scamscreener.ui.messages;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ModelUpdateMessages extends StyledMessages {
	private ModelUpdateMessages() {
	}

	public static MutableComponent modelUpdateAvailable(MutableComponent link) {
		return prefixed()
			.append(link == null ? gray("Model update available.") : link);
	}

	public static MutableComponent modelUpdateUpToDate() {
		return prefixedGray("AI model is already up to date.");
	}

	public static MutableComponent modelUpdateCheckFailed(String errorMessage) {
		return error("Model update check failed.", "MU-CHECK-001", errorMessage);
	}

	public static MutableComponent modelUpdateDownloadLink(String command, String localVersion, String remoteVersion) {
		String localText = safe(localVersion, "unknown").trim();
		String remoteText = safe(remoteVersion, "unknown").trim();
		Style style = Style.EMPTY
			.withColor(ChatFormatting.YELLOW)
			.withClickEvent(new ClickEvent.RunCommand(safe(command)))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Download model update")));
		return Component.literal("A new AI Model is available. Click to update your local model. (" + localText + " -> " + remoteText + ")")
			.setStyle(style);
	}

	public static MutableComponent modelUpdateReady(MutableComponent actions) {
		MutableComponent line = prefixed().append(gray("Model update downloaded. "));
		if (actions != null) {
			line.append(actions);
		}
		return line;
	}

	public static MutableComponent modelUpdateApplied(String action) {
		String safeAction = safe(action, "applied");
		return prefixed()
			.append(gray("Model update "))
			.append(goldBold(safeAction))
			.append(gray("."));
	}

	public static MutableComponent modelUpdateFailed(String message) {
		return error("Model update failed.", "MU-UPDATE-001", message);
	}

	public static MutableComponent modelUpdateNotReady() {
		return error("Model update not downloaded yet.", "MU-DOWNLOAD-001", "missing downloaded payload");
	}

	public static MutableComponent modelUpdateNotFound() {
		return error("Model update not found.", "MU-LOOKUP-001", "unknown update id");
	}

	public static MutableComponent modelUpdateIgnored() {
		return prefixedGray("Model update ignored.");
	}
}

