package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Runtime and pipeline settings screen using the classic v1 single-column layout.
 */
public final class RuntimeSettingsScreen extends BaseScreen {
    private ButtonWidget reviewThresholdButton;
    private ButtonWidget debugLoggingButton;

    /**
     * Creates the runtime settings screen.
     *
     * @param parent the parent screen to return to
     */
    public RuntimeSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener Runtime"), parent);
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

        reviewThresholdButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleReviewThreshold())
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
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rules Settings"), button -> this.client.setScreen(new RulesSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Review Settings"), button -> this.client.setScreen(new ReviewSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
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
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Pipeline");
        drawLine(context, left, CONTENT_TOP - 6, "Classic v1 layout, current v2 runtime controls.");
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
            reviewThresholdButton.setMessage(Text.literal("Review Threshold: " + config.pipeline().reviewThreshold()));
        }
        if (debugLoggingButton != null) {
            debugLoggingButton.setMessage(toggleText("Debug Logging: ", config.output().isDebugLogging()));
        }
    }
}
