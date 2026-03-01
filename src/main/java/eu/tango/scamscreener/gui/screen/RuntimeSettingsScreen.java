package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Runtime and pipeline settings screen.
 */
public final class RuntimeSettingsScreen extends BaseScreen {
    private ButtonWidget reviewThresholdButton;
    private ButtonWidget modelStageButton;
    private ButtonWidget debugLoggingButton;

    /**
     * Creates the runtime settings screen.
     *
     * @param parent the parent screen to return to
     */
    public RuntimeSettingsScreen(Screen parent) {
        super(Text.literal("Runtime Settings"), parent);
    }

    /**
     * Builds the settings buttons.
     */
    @Override
    protected void init() {
        int contentWidth = defaultContentWidth();
        int x = centeredX(contentWidth);
        int y = CONTENT_TOP + 8;

        reviewThresholdButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleReviewThreshold())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        modelStageButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleModelStage())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        debugLoggingButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleDebugLogging())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Message Settings"), button -> this.client.setScreen(new MessageSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addCloseButton(contentWidth);
        refreshButtons();
    }

    /**
     * Draws a small context header above the settings.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultContentWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Pipeline");
        drawLine(context, left, CONTENT_TOP - 6, "Changes save immediately and rebuild the engine.");
    }

    private void cycleReviewThreshold() {
        RuntimeConfig.PipelineSettings pipeline = ScamScreenerRuntime.getInstance().config().pipeline();
        int nextValue = (pipeline.reviewThreshold() + 1) % 6;
        pipeline.setReviewThreshold(nextValue);
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

    private void refreshButtons() {
        RuntimeConfig config = ScamScreenerRuntime.getInstance().config();

        if (reviewThresholdButton != null) {
            reviewThresholdButton.setMessage(Text.literal("Review Threshold: " + config.pipeline().reviewThreshold()));
        }
        if (modelStageButton != null) {
            modelStageButton.setMessage(Text.literal("Model Stage: " + onOff(config.stages().isModelEnabled())));
        }
        if (debugLoggingButton != null) {
            debugLoggingButton.setMessage(Text.literal("Debug Logging: " + onOff(config.output().isDebugLogging())));
        }
    }
}
