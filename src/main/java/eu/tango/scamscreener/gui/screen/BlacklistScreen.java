package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.data.BlacklistRow;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.lists.BlacklistEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Shared blacklist management screen.
 */
public final class BlacklistScreen extends BaseListScreen {
    private static final int LIST_ROW_HEIGHT = 36;

    private final BlacklistAccess blacklist;

    private SelectableListWidget<BlacklistRow> listWidget;
    private ButtonWidget reloadButton;
    private ButtonWidget removeButton;
    private ButtonWidget clearButton;

    /**
     * Creates the blacklist screen using the shared runtime blacklist.
     *
     * @param parent the parent screen to return to
     */
    public BlacklistScreen(Screen parent) {
        this(parent, ScamScreenerRuntime.getInstance().blacklist());
    }

    /**
     * Creates the blacklist screen.
     *
     * @param parent the parent screen to return to
     * @param blacklist the blacklist access to present and edit
     */
    public BlacklistScreen(Screen parent, BlacklistAccess blacklist) {
        super(Text.literal("Blacklist"), parent);
        this.blacklist = blacklist;
    }

    /**
     * Builds the list layout and footer actions.
     */
    @Override
    protected void init() {
        int contentWidth = Math.min(520, Math.max(300, this.width - 40));
        int contentX = centeredX(contentWidth);
        int listY = CONTENT_TOP + 16;
        int listHeight = Math.max(120, footerY() - listY - 36);

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

        reloadButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Reload"), button -> reloadRows())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove Selected"), button -> removeSelected())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        clearButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear All"), button -> clearBlacklist())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 3), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
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

        int left = centeredX(Math.min(520, Math.max(300, this.width - 40)));
        drawSectionTitle(context, left, CONTENT_TOP, "Blocked Players");
        drawLine(context, left, CONTENT_TOP + 12, "Entries: " + blacklist.allEntries().size());

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
        List<BlacklistRow> rows = new ArrayList<>();
        for (BlacklistEntry entry : blacklist.allEntries()) {
            rows.add(BlacklistRow.fromEntry(entry));
        }
        rows.sort(Comparator.comparing(row -> row.displayName().toLowerCase(Locale.ROOT)));

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
            clearButton.active = !blacklist.isEmpty();
        }
    }

    private void removeSelected() {
        if (listWidget == null) {
            return;
        }

        BlacklistRow selected = listWidget.selectedRow().orElse(null);
        if (selected == null) {
            return;
        }

        boolean removed = false;
        if (selected.playerUuid() != null) {
            removed = blacklist.remove(selected.playerUuid());
        }
        if (!removed && selected.playerName() != null && !selected.playerName().isBlank()) {
            blacklist.removeByName(selected.playerName());
        }

        reloadRows();
    }

    private void clearBlacklist() {
        blacklist.clear();
        reloadRows();
    }

    private void renderRow(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        BlacklistRow row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        context.drawTextWithShadow(textRenderer, Text.literal(row.displayName()), x, y, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(row.detailLine()), x, y + 11, 0xFFB366);
        context.drawTextWithShadow(textRenderer, Text.literal(trimToWidth(row.extraLine(), 64)), x, y + 22, 0xA0A0A0);
    }

    private String trimToWidth(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }

        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
