package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Runtime and pipeline settings screen.
 */
public final class RuntimeSettingsScreen extends BaseScreen {
    private Button reviewThresholdButton;
    private Button debugLoggingButton;

    /**
     * Creates the runtime settings screen.
     *
     * @param parent the parent screen to return to
     */
    public RuntimeSettingsScreen(Screen parent) {
        super(Component.literal("ScamScreener Runtime"), parent);
    }

    /**
     * Builds the settings buttons.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int contentWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + 12;

        reviewThresholdButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> cycleReviewThreshold())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        debugLoggingButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleDebugLogging())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Message Settings"), button -> this.minecraft.setScreen(new MessageSettingsScreen(this)))
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Rules Settings"), button -> this.minecraft.setScreen(new RulesSettingsScreen(this)))
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Review Settings"), button -> this.minecraft.setScreen(new ReviewSettingsScreen(this)))
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(contentWidth);
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Pipeline");
        drawLine(context, left, CONTENT_TOP - 6, "Runtime thresholds and debug output controls.");
    }

    private void cycleReviewThreshold() {
        RuntimeConfig.PipelineSettings pipeline = ScamScreenerRuntime.getInstance().config().pipeline();
        int nextValue = (pipeline.reviewThreshold() + 1) % 6;
        pipeline.setReviewThreshold(nextValue);
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
            reviewThresholdButton.setMessage(Component.literal("Review Threshold: " + config.pipeline().reviewThreshold()));
        }
        if (debugLoggingButton != null) {
            debugLoggingButton.setMessage(toggleText("Debug Logging: ", config.output().isDebugLogging()));
        }
    }
}
