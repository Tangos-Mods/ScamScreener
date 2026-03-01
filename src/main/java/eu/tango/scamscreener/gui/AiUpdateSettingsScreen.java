package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BiFunction;
import java.util.function.Supplier;

final class AiUpdateSettingsScreen extends ScamScreenerGUI {
	private final Runnable triggerAiUpdateHandler;
	private final Runnable triggerForceAiUpdateHandler;
	private final Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier;
	private final BiFunction<String, String, Integer> aiUpdateActionHandler;

	private Button statusButton;
	private Button checkButton;
	private Button forceCheckButton;
	private Button joinNotifyButton;
	private Button acceptButton;
	private Button mergeButton;
	private Button ignoreButton;

	AiUpdateSettingsScreen(
		Screen parent,
		Runnable triggerAiUpdateHandler,
		Runnable triggerForceAiUpdateHandler,
		Supplier<ModelUpdateService.PendingUpdateSnapshot> aiUpdateSnapshotSupplier,
		BiFunction<String, String, Integer> aiUpdateActionHandler
	) {
		super(Component.literal("ScamScreener Model Update"), parent);
		this.triggerAiUpdateHandler = triggerAiUpdateHandler;
		this.triggerForceAiUpdateHandler = triggerForceAiUpdateHandler;
		this.aiUpdateSnapshotSupplier = aiUpdateSnapshotSupplier;
		this.aiUpdateActionHandler = aiUpdateActionHandler;
	}

	@Override
	protected void init() {
		ColumnState column = defaultColumnState();
		int buttonWidth = column.buttonWidth();
		int x = column.x();
		int y = column.y();

		statusButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> refreshState())
			.bounds(x, y, buttonWidth, 20)
			.build());
		y += ROW_HEIGHT;

		int half = halfWidth(buttonWidth);
		checkButton = this.addRenderableWidget(Button.builder(Component.literal("Check / Download"), button -> {
			triggerCheck();
			refreshState();
		}).bounds(columnX(x, half, 0), y, half, 20).build());
		forceCheckButton = this.addRenderableWidget(Button.builder(Component.literal("Force Check"), button -> {
			triggerForceCheck();
			refreshState();
		}).bounds(columnX(x, half, 1), y, half, 20).build());
		forceCheckButton.active = triggerForceAiUpdateHandler != null;
		y += ROW_HEIGHT;

		joinNotifyButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setNotifyAiUpToDateOnJoin(!ScamRules.notifyAiUpToDateOnJoin());
			refreshState();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		int third = thirdWidth(buttonWidth);
		acceptButton = this.addRenderableWidget(Button.builder(Component.literal("Accept"), button -> applyAction("accept"))
			.bounds(columnX(x, third, 0), y, third, 20)
			.build());
		mergeButton = this.addRenderableWidget(Button.builder(Component.literal("Merge"), button -> applyAction("merge"))
			.bounds(columnX(x, third, 1), y, third, 20)
			.build());
		ignoreButton = this.addRenderableWidget(Button.builder(Component.literal("Ignore"), button -> applyAction("ignore"))
			.bounds(columnX(x, third, 2), y, third, 20)
			.build());

		addBackButton(buttonWidth);

		triggerCheck();
		refreshState();
	}

	@Override
	public void tick() {
		super.tick();
		if (isPeriodicTick(10)) {
			refreshState();
		}
	}

	private void triggerCheck() {
		if (triggerAiUpdateHandler != null) {
			triggerAiUpdateHandler.run();
		}
	}

	private void triggerForceCheck() {
		if (triggerForceAiUpdateHandler != null) {
			triggerForceAiUpdateHandler.run();
		}
	}

	private void refreshState() {
		refreshJoinNotifyButton();

		ModelUpdateService.PendingUpdateSnapshot snapshot = aiUpdateSnapshotSupplier == null ? null : aiUpdateSnapshotSupplier.get();
		if (snapshot == null) {
			statusButton.setMessage(Component.literal("No update ready. Press \"Check / Download\" or \"Force Check\"."));
			setActionButtons(false);
			return;
		}

		String version = snapshot.version() == null || snapshot.version().isBlank() ? "?" : snapshot.version();
		String shortId = snapshot.id() == null ? "?" : snapshot.id().substring(0, Math.min(8, snapshot.id().length()));
		if (!snapshot.downloaded()) {
			statusButton.setMessage(Component.literal("Update found (v" + version + ", " + shortId + "). Downloading..."));
			setActionButtons(false);
			return;
		}

		statusButton.setMessage(Component.literal("Update ready (v" + version + "). What next?"));
		setActionButtons(true);
	}

	private void applyAction(String action) {
		ModelUpdateService.PendingUpdateSnapshot snapshot = aiUpdateSnapshotSupplier == null ? null : aiUpdateSnapshotSupplier.get();
		if (snapshot == null || !snapshot.downloaded() || snapshot.id() == null || snapshot.id().isBlank()) {
			refreshState();
			return;
		}
		if (aiUpdateActionHandler != null) {
			aiUpdateActionHandler.apply(action, snapshot.id());
		}
		refreshState();
	}

	private void setActionButtons(boolean active) {
		if (acceptButton != null) {
			acceptButton.active = active;
		}
		if (mergeButton != null) {
			mergeButton.active = active;
		}
		if (ignoreButton != null) {
			ignoreButton.active = active;
		}
	}

	private void refreshJoinNotifyButton() {
		if (joinNotifyButton != null) {
			joinNotifyButton.setMessage(onOffLine("Join Up-to-Date Message: ", ScamRules.notifyAiUpToDateOnJoin()));
		}
	}
}
