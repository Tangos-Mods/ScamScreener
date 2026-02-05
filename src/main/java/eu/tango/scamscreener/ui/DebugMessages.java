package eu.tango.scamscreener.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DebugMessages {
	private static final String PREFIX = "[ScamScreener] ";
	private static final int PREFIX_LIGHT_RED = 0xFF5555;
	private static final ChatFormatting LABEL_COLOR = ChatFormatting.DARK_GRAY;
	private static final ChatFormatting MESSAGE_COLOR = ChatFormatting.GRAY;
	private static final ChatFormatting ACTIVE_COLOR = ChatFormatting.GREEN;

	private DebugMessages() {
	}

	public static MutableComponent updater(String message) {
		return labeled("Updater", message);
	}

	public static MutableComponent party(String message) {
		return labeled("Party", message);
	}

	public static MutableComponent trade(String message) {
		return labeled("Trade", message);
	}

	public static MutableComponent mute(String message) {
		return labeled("Mute", message);
	}

	public static MutableComponent debugStatus(String status) {
		return labeled("Debug", status);
	}

	public static MutableComponent debugStatus(Map<String, Boolean> states) {
		if (states == null || states.isEmpty()) {
			return debugStatus("no debug states");
		}
		Map<String, Boolean> ordered = new LinkedHashMap<>(states);
		MutableComponent line = Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Debug: ").withStyle(LABEL_COLOR));

		boolean first = true;
		for (Map.Entry<String, Boolean> entry : ordered.entrySet()) {
			if (!first) {
				line.append(Component.literal(", ").withStyle(MESSAGE_COLOR));
			}
			first = false;
			String key = entry.getKey();
			boolean enabled = Boolean.TRUE.equals(entry.getValue());
			String label = key + "=" + (enabled ? "on" : "off");
			String command = "/scamscreener debug " + (!enabled) + " " + key;
			Style style = Style.EMPTY
				.withColor(enabled ? ACTIVE_COLOR : MESSAGE_COLOR)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to " + (enabled ? "disable" : "enable")).withStyle(ChatFormatting.YELLOW)));
			line.append(Component.literal(label).withStyle(style));
		}

		return line;
	}

	private static MutableComponent labeled(String label, String message) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(label + ": ").withStyle(LABEL_COLOR))
			.append(Component.literal(message == null ? "" : message).withStyle(MESSAGE_COLOR));
	}
}
