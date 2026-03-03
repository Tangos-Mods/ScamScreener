package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Small v1-style metrics summary using the currently available runtime counters.
 */
public final class MetricsSettingsScreen extends BaseScreen {
    private ButtonWidget reviewButton;
    private ButtonWidget behaviorButton;
    private ButtonWidget trendButton;
    private ButtonWidget funnelButton;

    /**
     * Creates the metrics screen.
     *
     * @param parent the parent screen
     */
    public MetricsSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener Metrics"), parent);
    }

    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int buttonWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y();

        reviewButton = addDrawableChild(infoButton(x, y, buttonWidth));
        y += ROW_HEIGHT;
        behaviorButton = addDrawableChild(infoButton(x, y, buttonWidth));
        y += ROW_HEIGHT;
        trendButton = addDrawableChild(infoButton(x, y, buttonWidth));
        y += ROW_HEIGHT;
        funnelButton = addDrawableChild(infoButton(x, y, buttonWidth));
        y += ROW_HEIGHT;

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Refresh"), button -> refreshButtons())
                .dimensions(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset State"), button -> {
                ScamScreenerRuntime.getInstance().resetDetectionState();
                refreshButtons();
            }).dimensions(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        );

        addBackButton(buttonWidth);
        refreshButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Metrics");
        drawLine(context, left, CONTENT_TOP - 6, "Current v2 runtime counters in the old v1 screen slot.");
    }

    private ButtonWidget infoButton(int x, int y, int width) {
        return ButtonWidget.builder(Text.empty(), button -> refreshButtons())
            .dimensions(x, y, width, DEFAULT_BUTTON_HEIGHT)
            .build();
    }

    private void refreshButtons() {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();

        if (reviewButton != null) {
            reviewButton.setMessage(Text.literal(
                "Review Queue: " + runtime.reviewStore().entries().size() + " / " + runtime.reviewStore().maxEntries()
            ));
        }
        if (behaviorButton != null) {
            behaviorButton.setMessage(Text.literal(
                "Behavior Store: " + runtime.behaviorStore().trackedSenderCount()
                    + " senders | " + runtime.behaviorStore().trackedMessageCount() + " messages"
            ));
        }
        if (trendButton != null) {
            trendButton.setMessage(Text.literal(
                "Trend Store: " + runtime.trendStore().trackedMessageCount() + " buffered messages"
            ));
        }
        if (funnelButton != null) {
            funnelButton.setMessage(Text.literal(
                "Funnel Store: " + runtime.funnelStore().trackedSenderCount()
                    + " senders | " + runtime.funnelStore().trackedStepCount() + " steps"
            ));
        }
    }
}
