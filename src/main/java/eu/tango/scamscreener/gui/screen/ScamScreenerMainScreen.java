package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.config.data.AutoCaptureAlertLevel;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.concurrent.CompletionException;

/**
 * Root settings hub for ScamScreener.
 */
public final class ScamScreenerMainScreen extends BaseScreen {
    private static final String TRAINING_HUB_URL = "https://scamscreener.creepans.net/";
    private static final Component AUTHOR_TEXT = Component.literal("Made by Pankraz01");
    private static final Component TRAINING_HUB_SOON_TEXT = Component.literal("Training Hub coming soon.");

    private Button alertLevelButton;
    private Button autoCaptureButton;
    private Button autoLeaveButton;
    private Button muteFilterButton;
    private Button contributeTrainingButton;
    private int trainingHubNoteY;

    /**
     * Creates the root ScamScreener screen.
     *
     * @param parent the parent screen to return to
     */
    public ScamScreenerMainScreen(Screen parent) {
        super(Component.literal("ScamScreener Settings"), parent);
    }

    /**
     * Builds the quick toggles and navigation grid.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y();

        alertLevelButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> cycleAlertLevel())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        autoCaptureButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> cycleAutoCaptureLevel())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        autoLeaveButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleAutoLeave())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        muteFilterButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleMuteFilter())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int menuWidth = buttonWidth;
        int menuX = x;
        int menuButtonWidth = splitWidth(menuWidth, 3, DEFAULT_SPLIT_GAP);

        addRenderableWidget(
            Button.builder(Component.literal("Rule Settings"), button -> this.minecraft.setScreen(new RulesSettingsScreen(this)))
                .bounds(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Debug Settings"), button -> this.minecraft.setScreen(new DebugSettingsScreen(this)))
                .bounds(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Blacklist"), button -> this.minecraft.setScreen(new BlacklistScreen(this)))
                .bounds(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Message Settings"), button -> this.minecraft.setScreen(new MessageSettingsScreen(this)))
                .bounds(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Observability"), button -> this.minecraft.setScreen(new MetricsSettingsScreen(this)))
                .bounds(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.literal("Runtime"), button -> this.minecraft.setScreen(new RuntimeSettingsScreen(this)))
                .bounds(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Whitelist"), button -> this.minecraft.setScreen(new WhitelistScreen(this)))
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        addRenderableWidget(
            Button.builder(Component.literal("Case Review"), button -> this.minecraft.setScreen(new ReviewScreen(this)))
                .bounds(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        contributeTrainingButton = addRenderableWidget(
            Button.builder(Component.literal("Contribute Training Data"), button -> contributeTrainingData())
                .bounds(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        trainingHubNoteY = y + ROW_HEIGHT;

        addCloseButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        context.pose().pushMatrix();
        context.pose().scale(0.5F, 0.5F);
        context.centeredText(
            this.font,
            AUTHOR_TEXT,
            this.width,
            (TITLE_Y + 12) * 2,
            opaqueColor(0x8C8C8C)
        );
        context.pose().popMatrix();

        context.centeredText(
            this.font,
            TRAINING_HUB_SOON_TEXT,
            this.width / 2,
            trainingHubNoteY,
            opaqueColor(0xB8B8B8)
        );
    }

    private void cycleAlertLevel() {
        RuntimeConfig.AlertSettings alerts = ScamScreenerRuntime.getInstance().config().alerts();
        AlertRiskLevel nextLevel = alerts.minimumRiskLevel().next();
        alerts.setMinimumRiskLevel(nextLevel);
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void cycleAutoCaptureLevel() {
        RuntimeConfig.ReviewSettings review = ScamScreenerRuntime.getInstance().config().review();
        RuntimeConfig.AlertSettings alerts = ScamScreenerRuntime.getInstance().config().alerts();
        AutoCaptureAlertLevel nextLevel = alerts.autoCaptureLevel().next();
        alerts.setAutoCaptureLevel(nextLevel);
        review.setCaptureEnabled(nextLevel != AutoCaptureAlertLevel.OFF);
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleAutoLeave() {
        RuntimeConfig.SafetySettings safety = ScamScreenerRuntime.getInstance().config().safety();
        safety.setAutoLeaveOnBlacklist(!safety.isAutoLeaveOnBlacklist());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleMuteFilter() {
        ScamScreenerRuntime.getInstance().mutePatternManager().setEnabled(!ScamScreenerRuntime.getInstance().mutePatternManager().isEnabled());
        refreshButtons();
    }

    private void exportTrainingCases() {
        MessageDispatcher.reply(ClientMessages.trainingCasesExportStarted());
        ScamScreenerRuntime.getInstance().trainingCaseExportService()
            .exportReviewedCasesAsync(ScamScreenerRuntime.getInstance().reviewStore().entries())
            .whenComplete((exportResult, throwable) -> {
                if (throwable != null) {
                    MessageDispatcher.reply(ClientMessages.trainingCasesExportFailed(rootCauseMessage(throwable)));
                    return;
                }

                MessageDispatcher.reply(ClientMessages.trainingCasesExported(exportResult));
            });
    }

    private void contributeTrainingData() {
        exportTrainingCases();
        openTrainingHub();
    }

    private void openTrainingHub() {
        if (this.minecraft == null) {
            MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed("Client unavailable."));
            return;
        }

        this.minecraft.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                try {
                    Util.getPlatform().openUri(TRAINING_HUB_URL);
                } catch (Exception exception) {
                    MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed(exception.getMessage()));
                }
            }
            this.minecraft.setScreen(this);
        }, TRAINING_HUB_URL, true));
    }

    private void refreshButtons() {
        RuntimeConfig config = ScamScreenerRuntime.getInstance().config();

        if (alertLevelButton != null) {
            alertLevelButton.setMessage(Component.literal("Alert Threshold: " + config.alerts().minimumRiskLevel().name()));
        }
        if (autoCaptureButton != null) {
            AutoCaptureAlertLevel autoCaptureLevel = config.alerts().autoCaptureLevel();
            boolean enabled = autoCaptureLevel != AutoCaptureAlertLevel.OFF;
            String detail = enabled ? " (" + autoCaptureLevel.name() + "+)" : "";
            autoCaptureButton.setMessage(toggleText("Auto-Capture Cases: ", enabled, detail));
        }
        if (autoLeaveButton != null) {
            autoLeaveButton.setMessage(toggleText("Auto /p leave on blacklist: ", config.safety().isAutoLeaveOnBlacklist()));
        }
        if (muteFilterButton != null) {
            muteFilterButton.setMessage(toggleText("Mute Filter: ", ScamScreenerRuntime.getInstance().mutePatternManager().isEnabled()));
        }
        if (contributeTrainingButton != null) {
            contributeTrainingButton.active = false;
        }
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause instanceof CompletionException && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause == null ? null : rootCause.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }
}
