package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Small observability summary using the currently available runtime counters.
 */
public final class MetricsSettingsScreen extends BaseScreen {
    /**
     * Creates the metrics screen.
     *
     * @param parent the parent screen
     */
    public MetricsSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener Observability"), parent);
    }

    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + 88;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset State"), button -> {
                ScamScreenerRuntime.getInstance().resetDetectionState();
            }).dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT).build()
        );

        addBackButton(buttonWidth);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Observability");
        drawLine(context, left, CONTENT_TOP - 6, "Live runtime counters from review and state stores.");
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        int y = CONTENT_TOP + 18;
        drawLine(context, left, y, "Review Queue: " + runtime.reviewStore().entries().size() + " / " + runtime.reviewStore().maxEntries());
        y += 14;
        drawLine(
            context,
            left,
            y,
            "Behavior Store: " + runtime.behaviorStore().trackedSenderCount()
                + " senders | " + runtime.behaviorStore().trackedMessageCount() + " messages"
        );
        y += 14;
        drawLine(context, left, y, "Trend Store: " + runtime.trendStore().trackedMessageCount() + " buffered messages");
        y += 14;
        drawLine(
            context,
            left,
            y,
            "Funnel Store: " + runtime.funnelStore().trackedSenderCount()
                + " senders | " + runtime.funnelStore().trackedStepCount() + " steps"
        );
    }
}
