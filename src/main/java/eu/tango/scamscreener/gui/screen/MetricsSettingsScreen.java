package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Small observability summary using the currently available runtime counters.
 */
public final class MetricsSettingsScreen extends BaseScreen {
    private static final int METRICS_LINE_GAP = 14;
    private static final int METRICS_FIRST_LINE_Y = CONTENT_TOP + 18;
    private static final int METRICS_LINE_COUNT = 5;
    private static final int BUTTON_SECTION_TOP = METRICS_FIRST_LINE_Y + (METRICS_LINE_GAP * METRICS_LINE_COUNT) + 8;

    private Button profilerButton;

    /**
     * Creates the metrics screen.
     *
     * @param parent the parent screen
     */
    public MetricsSettingsScreen(Screen parent) {
        super(Component.literal("ScamScreener Observability"), parent);
    }

    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = BUTTON_SECTION_TOP;

        profilerButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleProfilerHud())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Reset State"), button -> {
                ScamScreenerRuntime.getInstance().resetDetectionState();
            }).bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        );

        addBackButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Observability");
        drawLine(context, left, CONTENT_TOP - 6, "Live runtime counters and profiler controls.");
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        int y = METRICS_FIRST_LINE_Y;
        drawLine(context, left, y, "Review Queue: " + runtime.reviewStore().entries().size() + " / " + runtime.reviewStore().maxEntries());
        y += METRICS_LINE_GAP;
        drawLine(
            context,
            left,
            y,
            "Behavior Store: " + runtime.behaviorStore().trackedSenderCount()
                + " senders | " + runtime.behaviorStore().trackedMessageCount() + " messages"
        );
        y += METRICS_LINE_GAP;
        drawLine(context, left, y, "Trend Store: " + runtime.trendStore().trackedMessageCount() + " buffered messages");
        y += METRICS_LINE_GAP;
        drawLine(
            context,
            left,
            y,
            "Funnel Store: " + runtime.funnelStore().trackedSenderCount()
                + " senders | " + runtime.funnelStore().trackedStepCount() + " steps"
        );
        y += METRICS_LINE_GAP;
        drawLine(
            context,
            left,
            y,
            "Profiler HUD: " + (runtime.config().profiler().isHudEnabled() ? "ON" : "OFF")
                + " | use /ss profiler on/off for the live overlay"
        );
    }

    private void toggleProfilerHud() {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        boolean enabled = !runtime.config().profiler().isHudEnabled();
        runtime.config().profiler().setHudEnabled(enabled);
        runtime.saveConfig();
        ScamScreenerProfiler.getInstance().onEnabledStateChanged(enabled);
        refreshButtons();
    }

    private void refreshButtons() {
        if (profilerButton != null) {
            profilerButton.setMessage(toggleText("Profiler HUD: ", ScamScreenerProfiler.getInstance().isEnabled()));
        }
    }
}
