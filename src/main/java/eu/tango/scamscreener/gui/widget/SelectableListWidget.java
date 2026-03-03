package eu.tango.scamscreener.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Small reusable scrollable list helper for ScamScreener screens.
 *
 * <p>This is intentionally lightweight and keeps the common list chrome in one
 * place, while each screen provides its own row renderer.
 *
 * @param <T> the row data type
 */
public final class SelectableListWidget<T> {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rowHeight;
    private final RowRenderer<T> rowRenderer;

    private List<T> rows = List.of();
    private int selectedIndex = -1;
    private int scrollOffsetRows;

    /**
     * Creates a reusable list widget.
     *
     * @param x the left x position
     * @param y the top y position
     * @param width the total widget width
     * @param height the total widget height
     * @param rowHeight the height of each row
     * @param rowRenderer callback used to draw each row
     */
    public SelectableListWidget(int x, int y, int width, int height, int rowHeight, RowRenderer<T> rowRenderer) {
        this.x = x;
        this.y = y;
        this.width = Math.max(40, width);
        this.height = Math.max(40, height);
        this.rowHeight = Math.max(16, rowHeight);
        this.rowRenderer = rowRenderer;
    }

    /**
     * Replaces the rows currently shown in the list.
     *
     * @param rows the new rows to show
     */
    public void setRows(List<T> rows) {
        if (rows == null || rows.isEmpty()) {
            this.rows = List.of();
            this.selectedIndex = -1;
            this.scrollOffsetRows = 0;
            return;
        }

        this.rows = List.copyOf(new ArrayList<>(rows));
        if (selectedIndex >= this.rows.size()) {
            selectedIndex = this.rows.size() - 1;
        }

        scrollOffsetRows = clamp(scrollOffsetRows, 0, maxScrollOffset());
    }

    /**
     * Returns the currently selected row, when present.
     *
     * @return the selected row
     */
    public Optional<T> selectedRow() {
        if (selectedIndex < 0 || selectedIndex >= rows.size()) {
            return Optional.empty();
        }

        return Optional.ofNullable(rows.get(selectedIndex));
    }

    /**
     * Returns the currently selected row index.
     *
     * @return the selected row index, or {@code -1} when nothing is selected
     */
    public int selectedIndex() {
        return selectedIndex;
    }

    /**
     * Selects a specific row index.
     *
     * @param selectedIndex the target row index, or {@code -1} to clear it
     */
    public void setSelectedIndex(int selectedIndex) {
        if (rows.isEmpty()) {
            this.selectedIndex = -1;
            this.scrollOffsetRows = 0;
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= rows.size()) {
            this.selectedIndex = -1;
            return;
        }

        this.selectedIndex = selectedIndex;
        if (this.selectedIndex < scrollOffsetRows) {
            scrollOffsetRows = this.selectedIndex;
        } else if (this.selectedIndex >= scrollOffsetRows + visibleRows()) {
            scrollOffsetRows = this.selectedIndex - visibleRows() + 1;
        }
        scrollOffsetRows = clamp(scrollOffsetRows, 0, maxScrollOffset());
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        selectedIndex = -1;
    }

    /**
     * Indicates whether the list is empty.
     *
     * @return {@code true} when there are no rows
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Renders the list background, rows and scrollbar.
     *
     * @param context the current draw context
     * @param textRenderer the active text renderer
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     */
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, 0xA0101010);
        context.fill(x, y, x + width, y + 1, 0xFF5A5A5A);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF5A5A5A);
        context.fill(x, y, x + 1, y + height, 0xFF5A5A5A);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF5A5A5A);

        if (rows.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No entries", x + (width / 2), y + (height / 2) - 4, opaqueColor(0xAAAAAA));
            return;
        }

        int visibleRows = visibleRows();
        int rowAreaWidth = rows.size() > visibleRows ? width - 6 : width - 2;
        int visibleCount = Math.min(visibleRows, rows.size() - scrollOffsetRows);

        for (int index = 0; index < visibleCount; index++) {
            int absoluteIndex = scrollOffsetRows + index;
            int rowY = y + (index * rowHeight);
            boolean hovered = isRowHovered(mouseX, mouseY, rowY);
            boolean selected = absoluteIndex == selectedIndex;

            int background = (index % 2 == 0) ? 0x402A2A2A : 0x40333333;
            if (selected) {
                background = 0x705A6A2A;
            } else if (hovered) {
                background = 0x60606060;
            }

            context.fill(x + 1, rowY + 1, x + rowAreaWidth, rowY + rowHeight - 1, background);
            rowRenderer.render(
                context,
                textRenderer,
                rows.get(absoluteIndex),
                x + 6,
                rowY + 4,
                rowAreaWidth - 10,
                rowHeight - 8,
                hovered,
                selected
            );
        }

        if (rows.size() > visibleRows) {
            renderScrollBar(context, visibleRows);
        }
    }

    /**
     * Handles row selection via left click.
     *
     * @param mouseX the click x position
     * @param mouseY the click y position
     * @param button the mouse button
     * @return {@code true} when the list consumed the click
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isInside(mouseX, mouseY) || rows.isEmpty()) {
            return false;
        }

        int relativeY = (int) (mouseY - y);
        int rowIndex = relativeY / rowHeight;
        int absoluteIndex = scrollOffsetRows + rowIndex;
        if (absoluteIndex < 0 || absoluteIndex >= rows.size()) {
            return false;
        }

        selectedIndex = absoluteIndex;
        return true;
    }

    /**
     * Handles vertical scrolling for the list.
     *
     * @param mouseX the scroll x position
     * @param mouseY the scroll y position
     * @param verticalAmount the vertical scroll amount
     * @return {@code true} when the list consumed the scroll
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!isInside(mouseX, mouseY) || rows.size() <= visibleRows()) {
            return false;
        }

        int delta = verticalAmount > 0 ? -1 : 1;
        scrollOffsetRows = clamp(scrollOffsetRows + delta, 0, maxScrollOffset());
        return true;
    }

    private void renderScrollBar(DrawContext context, int visibleRows) {
        int trackLeft = x + width - 4;
        int trackTop = y + 2;
        int trackBottom = y + height - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        context.fill(trackLeft, trackTop, trackLeft + 2, trackBottom, 0xFF3A3A3A);

        int thumbHeight = Math.max(12, (int) (trackHeight * (visibleRows / (double) rows.size())));
        int thumbRange = Math.max(1, trackHeight - thumbHeight);
        int maxOffset = Math.max(1, maxScrollOffset());
        int thumbTop = trackTop + (int) Math.round((scrollOffsetRows / (double) maxOffset) * thumbRange);
        context.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
    }

    private int visibleRows() {
        return Math.max(1, height / rowHeight);
    }

    private int maxScrollOffset() {
        return Math.max(0, rows.size() - visibleRows());
    }

    private boolean isInside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isRowHovered(int mouseX, int mouseY, int rowY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY < rowY + rowHeight;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int opaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }

    /**
     * Renders a single list row.
     *
     * @param <T> the row data type
     */
    @FunctionalInterface
    public interface RowRenderer<T> {
        /**
         * Draws one row.
         *
         * @param context the current draw context
         * @param textRenderer the active text renderer
         * @param row the row data
         * @param x the content x position
         * @param y the content y position
         * @param width the available row width
         * @param height the available row height
         * @param hovered whether the row is hovered
         * @param selected whether the row is selected
         */
        void render(
            DrawContext context,
            TextRenderer textRenderer,
            T row,
            int x,
            int y,
            int width,
            int height,
            boolean hovered,
            boolean selected
        );
    }
}
