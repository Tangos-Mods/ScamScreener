package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Review queue behavior and maintenance screen using the classic v1 single-column layout.
 */
public final class ReviewSettingsScreen extends BaseScreen {
    private static final int MIN_REVIEW_CAPACITY = 25;
    private static final int MAX_REVIEW_CAPACITY = 500;
    private static final int REVIEW_CAPACITY_STEP = 25;

    private ButtonWidget captureEnabledButton;
    private ButtonWidget maxEntriesButton;
    private ButtonWidget clearQueueButton;
    private ButtonWidget resetDetectionStateButton;

    /**
     * Creates the review settings screen.
     *
     * @param parent the parent screen to return to
     */
    public ReviewSettingsScreen(Screen parent) {
        super(Text.literal("ScamScreener Review"), parent);
    }

    /**
     * Builds the review settings and maintenance actions.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int contentWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + 24;

        captureEnabledButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleCaptureEnabled())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        maxEntriesButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleMaxEntries())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        clearQueueButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear Review Queue"), button -> clearReviewQueue())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        resetDetectionStateButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset Detection State"), button -> resetDetectionState())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Open Review Queue"), button -> this.client.setScreen(new ReviewScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Runtime Settings"), button -> this.client.setScreen(new RuntimeSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
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
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        int left = centeredX(defaultButtonWidth());
        int y = CONTENT_TOP - 18;

        drawSectionTitle(context, left, y, "Review Queue");
        y += 12;
        drawLine(context, left, y, "Capture can be disabled without disabling REVIEW decisions.");
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
            captureEnabledButton.setMessage(toggleText("Capture Review Entries: ", review.isCaptureEnabled()));
        }
        if (maxEntriesButton != null) {
            maxEntriesButton.setMessage(Text.literal("Review Queue Capacity: " + review.maxEntries()));
        }
        if (clearQueueButton != null) {
            clearQueueButton.active = !runtime.reviewStore().entries().isEmpty();
        }
        if (resetDetectionStateButton != null) {
            resetDetectionStateButton.active = true;
        }
    }
}
