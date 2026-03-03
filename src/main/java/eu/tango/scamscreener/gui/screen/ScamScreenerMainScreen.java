package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Root settings hub using the compact v1 menu layout.
 */
public final class ScamScreenerMainScreen extends BaseScreen {
    private ButtonWidget reviewThresholdButton;
    private ButtonWidget reviewCaptureButton;
    private ButtonWidget modelStageButton;
    private ButtonWidget debugLoggingButton;

    /**
     * Creates the root ScamScreener screen.
     *
     * @param parent the parent screen to return to
     */
    public ScamScreenerMainScreen(Screen parent) {
        super(Text.literal("ScamScreener Settings"), parent);
    }

    /**
     * Builds the v1-style quick toggles and navigation grid.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y();

        reviewThresholdButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleReviewThreshold())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        reviewCaptureButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleReviewCapture())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        modelStageButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleModelStage())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        debugLoggingButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleDebugLogging())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int menuWidth = Math.min(500, Math.max(260, this.width - 40));
        int menuX = centeredX(menuWidth);
        int menuButtonWidth = splitWidth(menuWidth, 3, DEFAULT_SPLIT_GAP);

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rules"), button -> this.client.setScreen(new RulesSettingsScreen(this)))
                .dimensions(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Messages"), button -> this.client.setScreen(new MessageSettingsScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Review"), button -> this.client.setScreen(new ReviewSettingsScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Whitelist"), button -> this.client.setScreen(new WhitelistScreen(this)))
                .dimensions(menuX, y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Blacklist"), button -> this.client.setScreen(new BlacklistScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 1), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Queue"), button -> this.client.setScreen(new ReviewScreen(this)))
                .dimensions(columnX(menuX, menuButtonWidth, DEFAULT_SPLIT_GAP, 2), y, menuButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Reload Data"), button -> reloadRuntime())
                .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addCloseButton(buttonWidth);
        refreshButtons();
    }

    private void cycleReviewThreshold() {
        RuntimeConfig.PipelineSettings pipeline = ScamScreenerRuntime.getInstance().config().pipeline();
        pipeline.setReviewThreshold((pipeline.reviewThreshold() + 1) % 6);
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleReviewCapture() {
        RuntimeConfig.ReviewSettings review = ScamScreenerRuntime.getInstance().config().review();
        review.setCaptureEnabled(!review.isCaptureEnabled());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleModelStage() {
        RuntimeConfig.StageSettings stages = ScamScreenerRuntime.getInstance().config().stages();
        stages.setModelEnabled(!stages.isModelEnabled());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleDebugLogging() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setDebugLogging(!output.isDebugLogging());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void reloadRuntime() {
        ScamScreenerRuntime.getInstance().reload();
        refreshButtons();
    }

    private void refreshButtons() {
        RuntimeConfig config = ScamScreenerRuntime.getInstance().config();

        if (reviewThresholdButton != null) {
            reviewThresholdButton.setMessage(Text.literal("Review Threshold: " + config.pipeline().reviewThreshold()));
        }
        if (reviewCaptureButton != null) {
            reviewCaptureButton.setMessage(Text.literal("Review Capture: " + onOff(config.review().isCaptureEnabled())));
        }
        if (modelStageButton != null) {
            modelStageButton.setMessage(Text.literal("Model Stage: " + onOff(config.stages().isModelEnabled())));
        }
        if (debugLoggingButton != null) {
            debugLoggingButton.setMessage(Text.literal("Debug Logging: " + onOff(config.output().isDebugLogging())));
        }
    }
}
