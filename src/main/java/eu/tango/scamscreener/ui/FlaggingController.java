package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.ai.TrainingCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;

public final class FlaggingController {
	private final TrainingCommandHandler trainingHandler;

	public FlaggingController(TrainingCommandHandler trainingHandler) {
		this.trainingHandler = trainingHandler;
	}

	public void updateHoveredFlagTarget(Minecraft client) {
		MessageFlagging.clearHovered();
		if (!(client.screen instanceof ChatScreen)) {
			return;
		}
		ChatComponent chat = client.gui.getChat();
		if (chat == null) {
			return;
		}
		double mouseX = scaledMouseX(client);
		double mouseY = scaledMouseY(client);
		Style style = chat.getClickedComponentStyleAt(mouseX, mouseY);
		if (style == null) {
			return;
		}
		ClickEvent clickEvent = style.getClickEvent();
		String id = MessageFlagging.extractId(clickEvent);
		if (id != null) {
			MessageFlagging.setHoveredId(id);
		}
	}

	public void handleFlagKeybinds(Minecraft client, int legitLabel, int scamLabel) {
		if (client.player == null || client.screen == null) {
			return;
		}
		if (!(client.screen instanceof ChatScreen)) {
			return;
		}
		if (!Keybinds.isControlDown(client)) {
			return;
		}
		if (Keybinds.consumeFlagLegit()) {
			trainingHandler.flagHoveredMessage(legitLabel);
		}
		if (Keybinds.consumeFlagScam()) {
			trainingHandler.flagHoveredMessage(scamLabel);
		}
	}

	private static double scaledMouseX(Minecraft client) {
		double raw = client.mouseHandler.xpos();
		return raw * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
	}

	private static double scaledMouseY(Minecraft client) {
		double raw = client.mouseHandler.ypos();
		return raw * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();
	}
}
