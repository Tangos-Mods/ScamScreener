package eu.tango.scamscreener.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class DebugMessages {
	private static final String PREFIX = "[ScamScreener] ";
	private static final int PREFIX_LIGHT_RED = 0xFF5555;

	private DebugMessages() {
	}

	public static MutableComponent updater(String message) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Updater: ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(message == null ? "" : message).withStyle(ChatFormatting.GRAY));
	}
}
