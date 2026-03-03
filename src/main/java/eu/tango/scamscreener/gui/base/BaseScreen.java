package eu.tango.scamscreener.gui.base;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Shared base screen for ScamScreener GUI pages.
 *
 * <p>This keeps the common frame, layout helpers and navigation behavior in one
 * place so concrete screens only define their specific content.
 */
public abstract class BaseScreen extends Screen {
    protected static final int TITLE_Y = 14;
    protected static final int CONTENT_TOP = 36;
    protected static final int FOOTER_MARGIN = 28;
    protected static final int ROW_HEIGHT = 24;
    protected static final int DEFAULT_BUTTON_HEIGHT = 20;
    protected static final int DEFAULT_SPLIT_GAP = 8;

    private final Screen parent;

    /**
     * Creates a shared ScamScreener screen.
     *
     * @param title the visible screen title
     * @param parent the parent screen to return to
     */
    protected BaseScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /**
     * Draws the common background and title.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 0x90000000);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, TITLE_Y, opaqueColor(0xFFFFFF));
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    /**
     * Returns to the parent screen.
     */
    @Override
    public void close() {
        if (this.client == null) {
            return;
        }

        this.client.setScreen(parent);
    }

    /**
     * Returns the default content width used by most settings screens.
     *
     * @return a centered, bounded content width
     */
    protected int defaultContentWidth() {
        return Math.min(360, Math.max(220, this.width - 40));
    }

    /**
     * Returns the classic v1 button width used by the compact settings pages.
     *
     * @return a centered, bounded button width
     */
    protected int defaultButtonWidth() {
        return Math.min(320, Math.max(180, this.width - 40));
    }

    /**
     * Returns the shared single-column layout used by the v1 settings screens.
     *
     * @return the default button width, x position and start y position
     */
    protected ColumnState defaultColumnState() {
        int buttonWidth = defaultButtonWidth();
        return new ColumnState(buttonWidth, centeredX(buttonWidth), CONTENT_TOP);
    }

    /**
     * Returns the x coordinate for a centered widget.
     *
     * @param widgetWidth the width of the widget
     * @return the centered x coordinate
     */
    protected int centeredX(int widgetWidth) {
        return (this.width - widgetWidth) / 2;
    }

    /**
     * Returns the y coordinate for the footer row.
     *
     * @return the footer y position
     */
    protected int footerY() {
        return this.height - FOOTER_MARGIN;
    }

    /**
     * Adds a footer button at a fixed x position.
     *
     * @param x the button x position
     * @param width the button width
     * @param label the button label
     * @param onPress the press handler
     * @return the created button widget
     */
    protected ButtonWidget addFooterButton(int x, int width, Text label, ButtonWidget.PressAction onPress) {
        return addDrawableChild(
            ButtonWidget.builder(label, onPress)
                .dimensions(x, footerY(), width, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
    }

    /**
     * Adds a centered footer button.
     *
     * @param width the button width
     * @param label the button label
     * @param onPress the press handler
     * @return the created button widget
     */
    protected ButtonWidget addCenteredFooterButton(int width, Text label, ButtonWidget.PressAction onPress) {
        return addFooterButton(centeredX(width), width, label, onPress);
    }

    /**
     * Adds a standard close button.
     *
     * @param width the button width
     * @return the created button widget
     */
    protected ButtonWidget addCloseButton(int width) {
        return addCenteredFooterButton(width, Text.literal("Close"), button -> close());
    }

    /**
     * Adds a standard back button.
     *
     * @param width the button width
     * @return the created button widget
     */
    protected ButtonWidget addBackButton(int width) {
        return addCenteredFooterButton(width, Text.literal("Back"), button -> close());
    }

    /**
     * Draws a section heading.
     *
     * @param context the draw context
     * @param x the x position
     * @param y the y position
     * @param title the visible section title
     */
    protected void drawSectionTitle(DrawContext context, int x, int y, String title) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(title), x, y, opaqueColor(0x55FF55));
    }

    /**
     * Draws a simple content line.
     *
     * @param context the draw context
     * @param x the x position
     * @param y the y position
     * @param text the visible text
     */
    protected void drawLine(DrawContext context, int x, int y, String text) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(text), x, y, opaqueColor(0xFFFFFF));
    }

    /**
     * Ensures custom text colors use a visible alpha channel.
     *
     * @param color an RGB or ARGB color
     * @return the same color with an opaque alpha channel when none was set
     */
    protected static int opaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }

    /**
     * Returns a short on/off label for toggle buttons.
     *
     * @param enabled the current toggle value
     * @return {@code ON} or {@code OFF}
     */
    protected String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    /**
     * Splits an area into equal-width columns.
     *
     * @param totalWidth the full available width
     * @param columns the number of columns
     * @param gap the spacing between columns
     * @return the width of each column
     */
    protected static int splitWidth(int totalWidth, int columns, int gap) {
        int safeColumns = Math.max(1, columns);
        int safeGap = Math.max(0, gap);
        int availableWidth = Math.max(0, totalWidth - ((safeColumns - 1) * safeGap));
        return availableWidth / safeColumns;
    }

    /**
     * Returns the x position of a column within a split area.
     *
     * @param startX the area start x
     * @param columnWidth the computed column width
     * @param gap the spacing between columns
     * @param columnIndex the target column index
     * @return the x position of the column
     */
    protected static int columnX(int startX, int columnWidth, int gap, int columnIndex) {
        int safeIndex = Math.max(0, columnIndex);
        int safeGap = Math.max(0, gap);
        return startX + ((columnWidth + safeGap) * safeIndex);
    }

    /**
     * Small layout state for one centered button column.
     *
     * @param buttonWidth the shared button width
     * @param x the shared x position
     * @param y the initial y position
     */
    protected record ColumnState(int buttonWidth, int x, int y) {
    }
}
