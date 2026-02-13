package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MainSettingsScreen extends GUI {
	private static final ScamRules.ScamRiskLevel[] ALERT_LEVELS = ScamRules.ScamRiskLevel.values();
	private static final String[] AUTO_CAPTURE_LEVELS = {"OFF", "LOW", "MEDIUM", "HIGH", "CRITICAL"};

	private final BlacklistManager blacklistManager;
	private final MutePatternManager mutePatternManager;
	private final BooleanSupplier autoLeaveEnabledSupplier;
	private final Consumer<Boolean> setAutoLeaveEnabledHandler;
	private final Consumer<Boolean> setAllDebugHandler;
	private final BiConsumer<String, Boolean> setDebugKeyHandler;
	private final Supplier<Map<String, Boolean>> debugStateSupplier;
	private final Runnable triggerAiUpdateHandler;
	private final Runnable triggerForceAiUpdateHandler;
	private final Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier;
	private final BiFunction<String, String, Integer> aiUpdateActionHandler;
	private final Runnable uploadTrainingDataHandler;

	private Button alertLevelButton;
	private Button autoCaptureButton;
	private Button autoLeaveButton;
	private Button muteFilterButton;
	private Button localAiButton;

	public MainSettingsScreen(
		Screen parent,
		BlacklistManager blacklistManager,
		MutePatternManager mutePatternManager,
		BooleanSupplier autoLeaveEnabledSupplier,
		Consumer<Boolean> setAutoLeaveEnabledHandler,
		Consumer<Boolean> setAllDebugHandler,
		BiConsumer<String, Boolean> setDebugKeyHandler,
		Supplier<Map<String, Boolean>> debugStateSupplier,
		Runnable triggerAiUpdateHandler,
		Runnable triggerForceAiUpdateHandler,
		Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier,
		BiFunction<String, String, Integer> aiUpdateActionHandler,
		Runnable uploadTrainingDataHandler
	) {
		super(Component.literal("ScamScreener Settings"), parent);
		this.blacklistManager = blacklistManager;
		this.mutePatternManager = mutePatternManager;
		this.autoLeaveEnabledSupplier = autoLeaveEnabledSupplier;
		this.setAutoLeaveEnabledHandler = setAutoLeaveEnabledHandler;
		this.setAllDebugHandler = setAllDebugHandler;
		this.setDebugKeyHandler = setDebugKeyHandler;
		this.debugStateSupplier = debugStateSupplier;
		this.triggerAiUpdateHandler = triggerAiUpdateHandler;
		this.triggerForceAiUpdateHandler = triggerForceAiUpdateHandler;
		this.aiUpdateSnapshotSupplier = aiUpdateSnapshotSupplier;
		this.aiUpdateActionHandler = aiUpdateActionHandler;
		this.uploadTrainingDataHandler = uploadTrainingDataHandler;
	}

	@Override
	protected void init() {
		ColumnLayout layout = defaultColumnLayout();
		int buttonWidth = layout.width();
		int x = layout.x();
		int y = layout.startY();

		alertLevelButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			cycleAlertLevel();
			refreshMainButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		autoCaptureButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			cycleAutoCaptureLevel();
			refreshMainButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		autoLeaveButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			setAutoLeaveEnabledHandler.accept(!autoLeaveEnabledSupplier.getAsBoolean());
			refreshMainButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		muteFilterButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			mutePatternManager.setEnabled(!mutePatternManager.isEnabled());
			refreshMainButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		localAiButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setLocalAiEnabled(!ScamRules.localAiEnabled());
			refreshMainButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		int thirdWidth = (buttonWidth - 16) / 3;
		this.addRenderableWidget(Button.builder(Component.literal("Rule Settings"), button -> {
			openScreen(new RuleSettingsScreen(this));
		}).bounds(x, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Debug Settings"), button -> {
			openScreen(new DebugSettingsScreen(this, setAllDebugHandler, setDebugKeyHandler, debugStateSupplier));
		}).bounds(x + thirdWidth + 8, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Blacklist"), button -> {
			openScreen(new BlacklistSettingsScreen(this, blacklistManager));
		}).bounds(x + (thirdWidth + 8) * 2, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Message Settings"), button -> {
			openScreen(new MessageSettingsScreen(this));
		}).bounds(x, y + ROW_HEIGHT, thirdWidth * 2 + 8, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("AI Update"), button -> {
			openScreen(new AiUpdateSettingsScreen(
				this,
				triggerAiUpdateHandler,
				triggerForceAiUpdateHandler,
				aiUpdateSnapshotSupplier,
				aiUpdateActionHandler
			));
		}).bounds(x + (thirdWidth + 8) * 2, y + ROW_HEIGHT, thirdWidth, 20).build());
		y += ROW_HEIGHT * 2;

		Button uploadTrainingButton = this.addRenderableWidget(Button.builder(Component.literal("Upload Training Data"), button -> {
			if (uploadTrainingDataHandler != null) {
				uploadTrainingDataHandler.run();
			}
		}).bounds(x, y, buttonWidth, 20).build());
		uploadTrainingButton.active = uploadTrainingDataHandler != null;

		addCloseButton(buttonWidth);
		refreshMainButtons();
	}

	private void refreshMainButtons() {
		if (alertLevelButton != null) {
			alertLevelButton.setMessage(Component.literal("Alert Threshold: " + ScamRules.minimumAlertRiskLevel().name()));
		}
		if (autoCaptureButton != null) {
			String level = normalizeLevel(ScamRules.autoCaptureAlertLevelSetting());
			boolean enabled = !"OFF".equals(level);
			var message = Component.literal("AI Auto-Capture: ").append(onOffComponent(enabled));
			if (enabled) {
				message = message.append(Component.literal(" (" + level + ")"));
			}
			autoCaptureButton.setMessage(message);
		}
		if (autoLeaveButton != null) {
			autoLeaveButton.setMessage(onOffLine("Auto /p leave on blacklist: ", autoLeaveEnabledSupplier.getAsBoolean()));
		}
		if (muteFilterButton != null) {
			muteFilterButton.setMessage(onOffLine("Mute Filter: ", mutePatternManager.isEnabled()));
		}
		if (localAiButton != null) {
			localAiButton.setMessage(onOffLine("Local AI Signal: ", ScamRules.localAiEnabled()));
		}
	}

	private static String normalizeLevel(String rawLevel) {
		if (rawLevel == null || rawLevel.isBlank()) {
			return "OFF";
		}
		return rawLevel.trim().toUpperCase(Locale.ROOT);
	}

	private void cycleAlertLevel() {
		ScamRules.ScamRiskLevel current = ScamRules.minimumAlertRiskLevel();
		int index = current == null ? 0 : current.ordinal();
		ScamRules.ScamRiskLevel next = ALERT_LEVELS[(index + 1) % ALERT_LEVELS.length];
		ScamRules.setMinimumAlertRiskLevel(next);
	}

	private void cycleAutoCaptureLevel() {
		String current = normalizeLevel(ScamRules.autoCaptureAlertLevelSetting());
		int index = indexOf(AUTO_CAPTURE_LEVELS, current);
		String next = AUTO_CAPTURE_LEVELS[(index + 1) % AUTO_CAPTURE_LEVELS.length];
		ScamRules.setAutoCaptureAlertLevelSetting(next);
	}

	private static int indexOf(String[] options, String value) {
		if (value == null) {
			return 0;
		}
		for (int i = 0; i < options.length; i++) {
			if (options[i].equalsIgnoreCase(value)) {
				return i;
			}
		}
		return 0;
	}
}
