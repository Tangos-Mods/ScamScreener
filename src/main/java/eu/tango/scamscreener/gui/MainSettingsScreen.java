package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ai.FunnelMetricsService;
import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MainSettingsScreen extends ScamScreenerGUI {
	private static final ScamRules.ScamRiskLevel[] ALERT_LEVELS = ScamRules.ScamRiskLevel.values();
	private static final String[] AUTO_CAPTURE_LEVELS = {"OFF", "LOW", "MEDIUM", "HIGH", "CRITICAL"};

	private final BlacklistManager blacklistManager;
	private final WhitelistManager whitelistManager;
	private final MutePatternManager mutePatternManager;
	private final Function<String, ResolvedTarget> targetResolver;
	private final Runnable refreshWhitelistNamesHandler;
	private final BooleanSupplier autoLeaveEnabledSupplier;
	private final Consumer<Boolean> setAutoLeaveEnabledHandler;
	private final Consumer<Boolean> setAllDebugHandler;
	private final BiConsumer<String, Boolean> setDebugKeyHandler;
	private final Supplier<Map<String, Boolean>> debugStateSupplier;
	private final Runnable triggerAiUpdateHandler;
	private final Runnable triggerForceAiUpdateHandler;
	private final Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier;
	private final BiFunction<String, String, Integer> aiUpdateActionHandler;
	private final Supplier<FunnelMetricsService.Snapshot> metricsSnapshotSupplier;
	private final Runnable openUploadRelaySettingsHandler;
	private final Runnable openTrainingCsvReviewHandler;
	private final Runnable uploadTrainingDataHandler;

	private Button alertLevelButton;
	private Button autoCaptureButton;
	private Button autoLeaveButton;
	private Button muteFilterButton;
	private Button localAiButton;

	public MainSettingsScreen(
		Screen parent,
		BlacklistManager blacklistManager,
		WhitelistManager whitelistManager,
		MutePatternManager mutePatternManager,
		Function<String, ResolvedTarget> targetResolver,
		Runnable refreshWhitelistNamesHandler,
		BooleanSupplier autoLeaveEnabledSupplier,
		Consumer<Boolean> setAutoLeaveEnabledHandler,
		Consumer<Boolean> setAllDebugHandler,
		BiConsumer<String, Boolean> setDebugKeyHandler,
		Supplier<Map<String, Boolean>> debugStateSupplier,
		Runnable triggerAiUpdateHandler,
		Runnable triggerForceAiUpdateHandler,
		Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier,
		BiFunction<String, String, Integer> aiUpdateActionHandler,
		Supplier<FunnelMetricsService.Snapshot> metricsSnapshotSupplier,
		Runnable openUploadRelaySettingsHandler,
		Runnable openTrainingCsvReviewHandler,
		Runnable uploadTrainingDataHandler
	) {
		super(Component.literal("ScamScreener Settings"), parent);
		this.blacklistManager = blacklistManager;
		this.whitelistManager = whitelistManager;
		this.mutePatternManager = mutePatternManager;
		this.targetResolver = targetResolver;
		this.refreshWhitelistNamesHandler = refreshWhitelistNamesHandler;
		this.autoLeaveEnabledSupplier = autoLeaveEnabledSupplier;
		this.setAutoLeaveEnabledHandler = setAutoLeaveEnabledHandler;
		this.setAllDebugHandler = setAllDebugHandler;
		this.setDebugKeyHandler = setDebugKeyHandler;
		this.debugStateSupplier = debugStateSupplier;
		this.triggerAiUpdateHandler = triggerAiUpdateHandler;
		this.triggerForceAiUpdateHandler = triggerForceAiUpdateHandler;
		this.aiUpdateSnapshotSupplier = aiUpdateSnapshotSupplier;
		this.aiUpdateActionHandler = aiUpdateActionHandler;
		this.metricsSnapshotSupplier = metricsSnapshotSupplier;
		this.openUploadRelaySettingsHandler = openUploadRelaySettingsHandler;
		this.openTrainingCsvReviewHandler = openTrainingCsvReviewHandler;
		this.uploadTrainingDataHandler = uploadTrainingDataHandler;
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(320, Math.max(180, this.width - 40));
		int x = (this.width - buttonWidth) / 2;
		int y = 36;

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

		int splitSpacing = 8;
		int thirdWidth = Math.max(0, (buttonWidth - splitSpacing * 2) / 3);
		int secondColumnX = x + thirdWidth + splitSpacing;
		int thirdColumnX = x + (thirdWidth + splitSpacing) * 2;

		this.addRenderableWidget(Button.builder(Component.literal("Rule Settings"), button -> {
			openScreen(new RuleSettingsScreen(this));
		}).bounds(x, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Debug Settings"), button -> {
			openScreen(new DebugSettingsScreen(this, setAllDebugHandler, setDebugKeyHandler, debugStateSupplier));
		}).bounds(secondColumnX, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Blacklist"), button -> {
			openScreen(new BlacklistSettingsScreen(this, blacklistManager));
		}).bounds(thirdColumnX, y, thirdWidth, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Message Settings"), button -> {
			openScreen(new MessageSettingsScreen(this));
		}).bounds(x, y + ROW_HEIGHT, thirdWidth, 20).build());

		Button metricsButton = this.addRenderableWidget(Button.builder(Component.literal("Metrics"), button -> {
			if (metricsSnapshotSupplier != null) {
				openScreen(new MetricsSettingsScreen(this, metricsSnapshotSupplier));
			}
		}).bounds(secondColumnX, y + ROW_HEIGHT, thirdWidth, 20).build());
		metricsButton.active = metricsSnapshotSupplier != null;

		this.addRenderableWidget(Button.builder(Component.literal("AI Update"), button -> {
			openScreen(new AiUpdateSettingsScreen(
				this,
				triggerAiUpdateHandler,
				triggerForceAiUpdateHandler,
				aiUpdateSnapshotSupplier,
				aiUpdateActionHandler
			));
		}).bounds(thirdColumnX, y + ROW_HEIGHT, thirdWidth, 20).build());
		y += ROW_HEIGHT * 2;

		this.addRenderableWidget(Button.builder(Component.literal("Whitelist"), button -> {
			if (refreshWhitelistNamesHandler != null) {
				refreshWhitelistNamesHandler.run();
			}
			openScreen(new WhitelistSettingsScreen(this, whitelistManager, targetResolver));
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		Button uploadAuthButton = this.addRenderableWidget(Button.builder(Component.literal("Upload Auth"), button -> {
			if (openUploadRelaySettingsHandler != null) {
				openUploadRelaySettingsHandler.run();
			}
		}).bounds(x, y, buttonWidth, 20).build());
		uploadAuthButton.active = openUploadRelaySettingsHandler != null;
		y += ROW_HEIGHT;

		int halfWidth = splitWidth(buttonWidth, 2, splitSpacing);
		int rightHalfX = x + halfWidth + splitSpacing;

		Button reviewTrainingCsvButton = this.addRenderableWidget(Button.builder(Component.literal("Review Training CSV"), button -> {
			if (openTrainingCsvReviewHandler != null) {
				openTrainingCsvReviewHandler.run();
			}
		}).bounds(x, y, halfWidth, 20).build());
		reviewTrainingCsvButton.active = openTrainingCsvReviewHandler != null;

		Button uploadTrainingButton = this.addRenderableWidget(Button.builder(Component.literal("Upload Training Data"), button -> {
			if (uploadTrainingDataHandler != null) {
				uploadTrainingDataHandler.run();
			}
		}).bounds(rightHalfX, y, halfWidth, 20).build());
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
