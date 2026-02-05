package eu.tango.scamscreener.ui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
	private static KeyMapping flagLegitKey;
	private static KeyMapping flagScamKey;

	private Keybinds() {
	}

	public static void register() {
		if (flagLegitKey != null || flagScamKey != null) {
			return;
		}
		flagLegitKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.scamscreener.flag_legit",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_Y,
			KeyMapping.Category.MISC
		));
		flagScamKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.scamscreener.flag_scam",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KeyMapping.Category.MISC
		));
	}

	public static boolean consumeFlagLegit() {
		return flagLegitKey != null && flagLegitKey.consumeClick();
	}

	public static boolean consumeFlagScam() {
		return flagScamKey != null && flagScamKey.consumeClick();
	}

	public static boolean isControlDown(net.minecraft.client.Minecraft client) {
		if (client == null || client.getWindow() == null) {
			return false;
		}
		Long windowHandle = extractWindowHandle(client.getWindow());
		if (windowHandle == null) {
			return false;
		}
		return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
			|| GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
	}

	private static Long extractWindowHandle(Object window) {
		try {
			Object value = window.getClass().getMethod("getWindow").invoke(window);
			if (value instanceof Long) {
				return (Long) value;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		try {
			Object value = window.getClass().getMethod("getHandle").invoke(window);
			if (value instanceof Long) {
				return (Long) value;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		try {
			java.lang.reflect.Field field = window.getClass().getDeclaredField("window");
			field.setAccessible(true);
			Object value = field.get(window);
			if (value instanceof Long) {
				return (Long) value;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		try {
			java.lang.reflect.Field field = window.getClass().getDeclaredField("handle");
			field.setAccessible(true);
			Object value = field.get(window);
			if (value instanceof Long) {
				return (Long) value;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return null;
	}
}
