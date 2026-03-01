package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.data.ReviewRow;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Review list screen modeled after the dense v1 review presentation.
 *
 * <p>The current version is a UI groundwork screen. It already supports row
 * cycling and summary counts, while the real review store can be connected
 * later without changing the interaction model.
 */
public final class ReviewScreen extends BaseListScreen {
    private static final int LIST_ROW_HEIGHT = 28;

    private final List<ReviewRow> rows = new ArrayList<>();
    private SelectableListWidget<ReviewRow> listWidget;
    private ButtonWidget resetButton;
    private ButtonWidget clearButton;

    /**
     * Creates a review screen backed by the shared runtime review queue.
     *
     * @param parent the parent screen to return to
     */
    public ReviewScreen(Screen parent) {
        super(Text.literal("Review Queue"), parent);
    }

    /**
     * Builds the list layout and footer actions.
     */
    @Override
    protected void init() {
        int contentWidth = Math.min(560, Math.max(320, this.width - 40));
        int contentX = centeredX(contentWidth);
        int listY = CONTENT_TOP + 24;
        int listHeight = Math.max(120, footerY() - listY - 36);

        listWidget = new SelectableListWidget<>(
            contentX,
            listY,
            contentWidth,
            listHeight,
            LIST_ROW_HEIGHT,
            this::renderRow
        );

        int buttonWidth = splitWidth(contentWidth, 3, DEFAULT_SPLIT_GAP);
        int buttonY = listY + listHeight + 10;

        resetButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset Labels"), button -> resetChoices())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        clearButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear Queue"), button -> clearQueue())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        reloadRows();
        updateActionState();
    }

    /**
     * Draws the review summary and the list widget.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(Math.min(560, Math.max(320, this.width - 40)));
        drawSectionTitle(context, left, CONTENT_TOP, "Review Summary");
        drawLine(
            context,
            left,
            CONTENT_TOP + 12,
            "Pending " + count(ReviewVerdict.PENDING)
                + " | Risk " + count(ReviewVerdict.RISK)
                + " | Safe " + count(ReviewVerdict.SAFE)
                + " | Ignored " + count(ReviewVerdict.IGNORED)
        );

        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        if (button != 0 || listWidget == null || !listWidget.mouseClicked(mouseX, mouseY, button)) {
            return false;
        }

        ReviewRow selectedRow = listWidget.selectedRow().orElse(null);
        if (selectedRow != null) {
            ScamScreenerRuntime.getInstance().reviewStore().setVerdict(selectedRow.rowId(), nextVerdict(selectedRow.verdict()));
            reloadRows();
        }

        return true;
    }

    @Override
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private void resetChoices() {
        for (ReviewRow row : rows) {
            ScamScreenerRuntime.getInstance().reviewStore().setVerdict(row.rowId(), ReviewVerdict.PENDING);
        }
        reloadRows();
    }

    private void clearQueue() {
        ScamScreenerRuntime.getInstance().reviewStore().clear();
        reloadRows();
    }

    private void updateActionState() {
        if (resetButton != null) {
            resetButton.active = !rows.isEmpty();
        }
        if (clearButton != null) {
            clearButton.active = !rows.isEmpty();
        }
    }

    private int count(ReviewVerdict target) {
        int count = 0;
        for (ReviewRow row : rows) {
            if (row.verdict() == target) {
                count++;
            }
        }
        return count;
    }

    private void reloadRows() {
        rows.clear();
        for (ReviewEntry entry : ScamScreenerRuntime.getInstance().reviewStore().entries()) {
            rows.add(ReviewRow.fromEntry(entry));
        }

        if (listWidget != null) {
            listWidget.setRows(rows);
        }

        updateActionState();
    }

    private void renderRow(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        ReviewRow row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        String header = "[" + marker(row.verdict()) + "] (" + row.score() + ") " + row.displayName();

        context.drawTextWithShadow(textRenderer, Text.literal(header), x, y, color(row.verdict()));
        context.drawTextWithShadow(textRenderer, Text.literal(row.compactMessage()), x, y + 11, 0xFFFFFF);
    }

    private ReviewVerdict nextVerdict(ReviewVerdict current) {
        if (current == null) {
            return ReviewVerdict.RISK;
        }

        return switch (current) {
            case PENDING -> ReviewVerdict.RISK;
            case RISK -> ReviewVerdict.SAFE;
            case SAFE -> ReviewVerdict.IGNORED;
            case IGNORED -> ReviewVerdict.PENDING;
        };
    }

    private String marker(ReviewVerdict verdict) {
        return switch (verdict == null ? ReviewVerdict.PENDING : verdict) {
            case PENDING -> "P";
            case RISK -> "R";
            case SAFE -> "S";
            case IGNORED -> "I";
        };
    }

    private int color(ReviewVerdict verdict) {
        return switch (verdict == null ? ReviewVerdict.PENDING : verdict) {
            case PENDING -> 0xD0D0D0;
            case RISK -> 0xFFB366;
            case SAFE -> 0x99FF99;
            case IGNORED -> 0xB8B8B8;
        };
    }
}
