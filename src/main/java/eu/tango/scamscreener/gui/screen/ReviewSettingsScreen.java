package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Review queue behavior and maintenance screen.
 */
public final class ReviewSettingsScreen extends BaseScreen {
    private static final int MIN_REVIEW_CAPACITY = 25;
    private static final int MAX_REVIEW_CAPACITY = 500;
    private static final int REVIEW_CAPACITY_STEP = 25;
    private static final int SUMMARY_TO_CONTROLS_GAP = 56;

    private Button captureEnabledButton;
    private Button maxEntriesButton;
    private Button clearQueueButton;
    private Button resetDetectionStateButton;

    /**
     * Creates the review settings screen.
     *
     * @param parent the parent screen to return to
     */
    public ReviewSettingsScreen(Screen parent) {
        super(Component.literal("ScamScreener Review"), parent);
    }

    /**
     * Builds the review settings and maintenance actions.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int contentWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + SUMMARY_TO_CONTROLS_GAP;

        captureEnabledButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleCaptureEnabled())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        maxEntriesButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> cycleMaxEntries())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        clearQueueButton = addRenderableWidget(
            Button.builder(Component.literal("Clear Review Queue"), button -> clearReviewQueue())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        resetDetectionStateButton = addRenderableWidget(
            Button.builder(Component.literal("Reset Detection State"), button -> resetDetectionState())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Open Review Queue"), button -> this.minecraft.setScreen(new ReviewScreen(this)))
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addRenderableWidget(
            Button.builder(Component.literal("Runtime Settings"), button -> this.minecraft.setScreen(new RuntimeSettingsScreen(this)))
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(contentWidth);
        refreshButtons();
    }

    /**
     * Draws the current review runtime summary.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        int left = centeredX(defaultButtonWidth());
        int y = CONTENT_TOP - 18;

        drawSectionTitle(context, left, y, "Review Queue");
        y += 12;
        drawLine(context, left, y, "Auto-capture stores REVIEW outcomes as queue cases.");
        y += 12;
        drawLine(context, left, y, "It does not auto-open the case-review screen.");
        y += 12;
        drawLine(context, left, y, "Exported JSONL files can be uploaded in the Training Hub.");
        y += 12;
        drawLine(context, left, y, "Current Entries: " + runtime.reviewStore().entries().size() + " / " + runtime.reviewStore().maxEntries());
    }

    private void toggleCaptureEnabled() {
        RuntimeConfig.ReviewSettings review = ScamScreenerRuntime.getInstance().config().review();
        review.setCaptureEnabled(!review.isCaptureEnabled());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void cycleMaxEntries() {
        RuntimeConfig.ReviewSettings review = ScamScreenerRuntime.getInstance().config().review();
        int nextValue = review.maxEntries() + REVIEW_CAPACITY_STEP;
        if (nextValue > MAX_REVIEW_CAPACITY) {
            nextValue = MIN_REVIEW_CAPACITY;
        }

        review.setMaxEntries(nextValue);
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void clearReviewQueue() {
        ScamScreenerRuntime.getInstance().reviewStore().clear();
        refreshButtons();
    }

    private void resetDetectionState() {
        ScamScreenerRuntime.getInstance().resetDetectionState();
        refreshButtons();
    }

    private void refreshButtons() {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        RuntimeConfig.ReviewSettings review = runtime.config().review();

        if (captureEnabledButton != null) {
            captureEnabledButton.setMessage(toggleText("Auto-Capture Cases: ", review.isCaptureEnabled()));
        }
        if (maxEntriesButton != null) {
            maxEntriesButton.setMessage(Component.literal("Review Queue Capacity: " + review.maxEntries()));
        }
        if (clearQueueButton != null) {
            clearQueueButton.active = !runtime.reviewStore().entries().isEmpty();
        }
        if (resetDetectionStateButton != null) {
            resetDetectionStateButton.active = true;
        }
    }
}
