package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.config.data.AutoCaptureAlertLevel;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Root settings hub for ScamScreener.
 */
public final class ScamScreenerMainScreen extends BaseScreen {
    private static final String TRAINING_HUB_URL = "https://scamscreener.creepans.net/";
    private static final Text AUTHOR_TEXT = Text.literal("Made by Pankraz01");
    private static final Text TRAINING_HUB_SOON_TEXT = Text.literal("Training Hub coming soon.");

    private ButtonWidget alertLevelButton;
    private ButtonWidget autoCaptureButton;
    private ButtonWidget autoLeaveButton;
    private ButtonWidget muteFilterButton;
    private ButtonWidget contributeTrainingButton;
    private int trainingHubNoteY;

    /**
     * Creates the root ScamScreener screen.
     *
     * @param parent the parent screen to return to
     */
    public ScamScreenerMainScreen(Screen parent) {
        super(Text.literal("ScamScreener Settings"), parent);
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

        alertLevelButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleAlertLevel())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        autoCaptureButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleAutoCaptureLevel())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        autoLeaveButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleAutoLeave())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        muteFilterButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleMuteFilter())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int menuWidth = buttonWidth;
        int menuX = x;
        int menuButtonWidth = splitWidth(menuWidth, 3, DEFAULT_SPLIT_GAP);

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rule Settings"), button -> this.client.setScreen(new RulesSettingsScreen(this)))
                .dimensions(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Debug Settings"), button -> this.client.setScreen(new DebugSettingsScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Blacklist"), button -> this.client.setScreen(new BlacklistScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Message Settings"), button -> this.client.setScreen(new MessageSettingsScreen(this)))
                .dimensions(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Observability"), button -> this.client.setScreen(new MetricsSettingsScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Runtime"), button -> this.client.setScreen(new RuntimeSettingsScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Whitelist"), button -> this.client.setScreen(new WhitelistScreen(this)))
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Case Review"), button -> this.client.setScreen(new ReviewScreen(this)))
                .dimensions(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        contributeTrainingButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Contribute Training Data"), button -> contributeTrainingData())
                .dimensions(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        trainingHubNoteY = y + ROW_HEIGHT;

        addCloseButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(0.5F, 0.5F);
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            AUTHOR_TEXT,
            this.width,
            (TITLE_Y + 12) * 2,
            opaqueColor(0x8C8C8C)
        );
        context.getMatrices().popMatrix();

        context.drawCenteredTextWithShadow(
            this.textRenderer,
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
        try {
            var exportResult = ScamScreenerRuntime.getInstance().trainingCaseExportService()
                .exportReviewedCases(ScamScreenerRuntime.getInstance().reviewStore().entries());
            MessageDispatcher.reply(ClientMessages.trainingCasesExported(exportResult));
        } catch (IllegalStateException exception) {
            MessageDispatcher.reply(ClientMessages.trainingCasesExportFailed(exception.getMessage()));
        }
    }

    private void contributeTrainingData() {
        exportTrainingCases();
        openTrainingHub();
    }

    private void openTrainingHub() {
        if (this.client == null) {
            MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed("Client unavailable."));
            return;
        }

        this.client.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                try {
                    Util.getOperatingSystem().open(TRAINING_HUB_URL);
                } catch (Exception exception) {
                    MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed(exception.getMessage()));
                }
            }
            this.client.setScreen(this);
        }, TRAINING_HUB_URL, true));
    }

    private void refreshButtons() {
        RuntimeConfig config = ScamScreenerRuntime.getInstance().config();

        if (alertLevelButton != null) {
            alertLevelButton.setMessage(Text.literal("Alert Threshold: " + config.alerts().minimumRiskLevel().name()));
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
}
