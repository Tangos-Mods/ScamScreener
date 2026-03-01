package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.data.WhitelistRow;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.lists.WhitelistEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First concrete v2 GUI screen built on the shared GUI foundation.
 *
 * <p>This screen intentionally focuses on list rendering and shared actions so
 * it can validate the new GUI base before the full settings tree is added.
 */
public final class WhitelistScreen extends BaseListScreen {
    private static final int LIST_ROW_HEIGHT = 28;

    private final WhitelistAccess whitelist;

    private SelectableListWidget<WhitelistRow> listWidget;
    private ButtonWidget reloadButton;
    private ButtonWidget removeButton;
    private ButtonWidget clearButton;

    /**
     * Creates the whitelist screen using the shared runtime whitelist.
     *
     * @param parent the parent screen to return to
     */
    public WhitelistScreen(Screen parent) {
        this(parent, ScamScreenerRuntime.getInstance().whitelist());
    }

    /**
     * Creates the whitelist screen.
     *
     * @param parent the parent screen to return to
     * @param whitelist the whitelist access to present and edit
     */
    public WhitelistScreen(Screen parent, WhitelistAccess whitelist) {
        super(Text.literal("Whitelist"), parent);
        this.whitelist = whitelist;
    }

    /**
     * Builds the list layout and footer actions.
     */
    @Override
    protected void init() {
        int contentWidth = Math.min(460, Math.max(260, this.width - 40));
        int contentX = centeredX(contentWidth);
        int listY = CONTENT_TOP + 16;
        int listHeight = Math.max(100, footerY() - listY - 36);

        listWidget = new SelectableListWidget<>(
            contentX,
            listY,
            contentWidth,
            listHeight,
            LIST_ROW_HEIGHT,
            this::renderRow
        );

        int buttonWidth = splitWidth(contentWidth, 4, DEFAULT_SPLIT_GAP);
        int buttonY = listY + listHeight + 10;
        int reloadX = contentX;
        int removeX = columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1);
        int clearX = columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2);
        int closeX = columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 3);

        reloadButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Reload"), button -> reloadRows())
                .dimensions(reloadX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove Selected"), button -> removeSelected())
                .dimensions(removeX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        clearButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear All"), button -> clearWhitelist())
                .dimensions(clearX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(closeX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        reloadRows();
    }

    /**
     * Draws the screen summary and the reusable list widget.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(Math.min(460, Math.max(260, this.width - 40)));
        drawSectionTitle(context, left, CONTENT_TOP, "Trusted Players");
        drawLine(context, left, CONTENT_TOP + 12, "Entries: " + whitelist.allEntries().size());

        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        if (listWidget != null && listWidget.mouseClicked(mouseX, mouseY, button)) {
            updateActionState();
            return true;
        }

        return false;
    }

    @Override
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private void reloadRows() {
        List<WhitelistRow> rows = new ArrayList<>();
        for (WhitelistEntry entry : whitelist.allEntries()) {
            rows.add(WhitelistRow.fromEntry(entry));
        }
        rows.sort(
            Comparator.comparing(
                row -> row.displayName().toLowerCase(java.util.Locale.ROOT)
            )
        );

        if (listWidget != null) {
            listWidget.setRows(rows);
        }

        updateActionState();
    }

    private void updateActionState() {
        if (reloadButton != null) {
            reloadButton.active = true;
        }
        if (removeButton != null) {
            removeButton.active = listWidget != null && listWidget.selectedRow().isPresent();
        }
        if (clearButton != null) {
            clearButton.active = !whitelist.isEmpty();
        }
    }

    private void removeSelected() {
        if (listWidget == null) {
            return;
        }

        WhitelistRow selected = listWidget.selectedRow().orElse(null);
        if (selected == null) {
            return;
        }

        boolean removed = false;
        if (selected.playerUuid() != null) {
            removed = whitelist.remove(selected.playerUuid());
        }
        if (!removed && selected.playerName() != null && !selected.playerName().isBlank()) {
            whitelist.removeByName(selected.playerName());
        }

        reloadRows();
    }

    private void clearWhitelist() {
        whitelist.clear();
        reloadRows();
    }

    private void renderRow(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        WhitelistRow row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        context.drawTextWithShadow(textRenderer, Text.literal(row.displayName()), x, y, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(row.detailLine()), x, y + 11, 0xA0A0A0);
    }
}
