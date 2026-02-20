package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ui.DebugRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DebugSettingsScreen extends ScamScreenerGUI {
	private final Consumer<Boolean> setAllDebugHandler;
	private final BiConsumer<String, Boolean> setDebugKeyHandler;
	private final Supplier<Map<String, Boolean>> debugStateSupplier;

	private Button allButton;
	private final Map<String, Button> debugButtons = new LinkedHashMap<>();

	DebugSettingsScreen(
		Screen parent,
		Consumer<Boolean> setAllDebugHandler,
		BiConsumer<String, Boolean> setDebugKeyHandler,
		Supplier<Map<String, Boolean>> debugStateSupplier
	) {
		super(Component.literal("ScamScreener Debug"), parent);
		this.setAllDebugHandler = setAllDebugHandler;
		this.setDebugKeyHandler = setDebugKeyHandler;
		this.debugStateSupplier = debugStateSupplier;
	}

	@Override
	protected void init() {
		debugButtons.clear();
		ColumnState column = defaultColumnState();
		int buttonWidth = column.buttonWidth();
		int x = column.x();
		int y = column.y();

		allButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			Map<String, Boolean> states = currentStates();
			boolean allEnabled = allEnabled(states);
			setAllDebugHandler.accept(!allEnabled);
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		for (String key : DebugRegistry.keys()) {
			Button button = this.addRenderableWidget(Button.builder(Component.empty(), clicked -> {
				Map<String, Boolean> states = currentStates();
				boolean enabled = states.getOrDefault(DebugRegistry.normalize(key), false);
				setDebugKeyHandler.accept(key, !enabled);
				refreshButtons();
			}).bounds(x, y, buttonWidth, 20).build());
			y += ROW_HEIGHT;
			debugButtons.put(key, button);
		}

		addBackButton(buttonWidth);
		refreshButtons();
	}

	private void refreshButtons() {
		Map<String, Boolean> states = currentStates();
		if (allButton != null) {
			allButton.setMessage(onOffLine("All Debug: ", allEnabled(states)));
		}
		for (Map.Entry<String, Button> entry : debugButtons.entrySet()) {
			String key = entry.getKey();
			boolean enabled = states.getOrDefault(DebugRegistry.normalize(key), false);
			entry.getValue().setMessage(onOffLine(readableDebugName(key) + ": ", enabled));
		}
	}

	private Map<String, Boolean> currentStates() {
		return DebugRegistry.withDefaults(debugStateSupplier.get());
	}

	private static boolean allEnabled(Map<String, Boolean> states) {
		if (states == null || states.isEmpty()) {
			return false;
		}
		for (String key : DebugRegistry.keys()) {
			if (!states.getOrDefault(DebugRegistry.normalize(key), false)) {
				return false;
			}
		}
		return true;
	}

	private static String readableDebugName(String key) {
		return switch (DebugRegistry.normalize(key)) {
			case "updater" -> "Updater";
			case "trade" -> "Trade";
			case "market" -> "Market";
			case "mute" -> "Mute";
			case "chatcolor" -> "Chat Color";
			default -> key == null ? "" : key;
		};
	}
}
