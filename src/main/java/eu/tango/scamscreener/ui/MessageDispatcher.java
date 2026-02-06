package eu.tango.scamscreener.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MessageDispatcher {
	private MessageDispatcher() {
	}

	public static void reply(Component text) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			if (client.player != null) {
				client.player.displayClientMessage(text, false);
			}
		});
	}

	public static void sendChatMessage(String message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			return;
		}
		client.getConnection().sendChat(message);
	}

	public static void sendCommand(String command) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			return;
		}
		client.getConnection().sendCommand(command);
	}
}
