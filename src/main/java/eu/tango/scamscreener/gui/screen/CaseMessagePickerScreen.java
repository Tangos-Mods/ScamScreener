package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Picker for adding cached inbound chat lines into one review case.
 */
public final class CaseMessagePickerScreen extends BaseListScreen {
    private static final int LIST_ROW_HEIGHT = 28;

    private final AlertManageScreen reviewScreen;
    private final String initialPlayerFilter;
    private final List<RecentChatCache.CachedChatMessage> visibleEntries = new ArrayList<>();

    private SelectableListWidget<RecentChatCache.CachedChatMessage> listWidget;
    private TextFieldWidget playerFilterField;
    private ButtonWidget addSelectedButton;
    private ButtonWidget addAllVisibleButton;
    private ButtonWidget addLastTenButton;
    private long lastSeenCacheVersion = -1L;

    public CaseMessagePickerScreen(AlertManageScreen reviewScreen, String initialPlayerFilter) {
        super(Text.literal("Add Case Messages"), reviewScreen);
        this.reviewScreen = reviewScreen;
        this.initialPlayerFilter = initialPlayerFilter == null ? "" : initialPlayerFilter.trim();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(620, Math.max(360, this.width - 40));
        int contentX = centeredX(contentWidth);
        int controlsY = CONTENT_TOP + 24;

        playerFilterField = addDrawableChild(
            new TextFieldWidget(
                this.textRenderer,
                contentX,
                controlsY,
                contentWidth,
                DEFAULT_BUTTON_HEIGHT,
                Text.literal("Player Filter")
            )
        );
        playerFilterField.setMaxLength(64);
        playerFilterField.setSuggestion("Filter by player name...");
        playerFilterField.setText(initialPlayerFilter);
        playerFilterField.setChangedListener(value -> reloadRows());

        int listY = controlsY + ROW_HEIGHT;
        int buttonY = footerY() - DEFAULT_BUTTON_HEIGHT - 24;
        int listHeight = Math.max(80, buttonY - listY - 10);
        listWidget = new SelectableListWidget<>(
            contentX,
            listY,
            contentWidth,
            listHeight,
            LIST_ROW_HEIGHT,
            this::renderRow
        );

        int buttonWidth = splitWidth(contentWidth, 3, DEFAULT_SPLIT_GAP);
        addSelectedButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Selected"), button -> addSelected())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addAllVisibleButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Add All Visible"), button -> addAllVisible())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addLastTenButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Last 10"), button -> addLastTen())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(Math.min(220, contentWidth));
        reloadRows();
    }

    @Override
    public void tick() {
        long currentVersion = ScamScreenerRuntime.getInstance().recentChatCache().version();
        if (currentVersion != lastSeenCacheVersion) {
            reloadRows();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        int left = centeredX(Math.min(620, Math.max(360, this.width - 40)));
        drawLine(context, left, CONTENT_TOP, "Pick cached chat lines, filter by player, then add one, all visible, or the latest 10 visible.");
        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        if (button != 0 || listWidget == null) {
            return false;
        }
        if (!listWidget.mouseClicked(mouseX, mouseY, button)) {
            return false;
        }

        updateActionState();
        return true;
    }

    @Override
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private void addSelected() {
        if (listWidget == null) {
            return;
        }

        RecentChatCache.CachedChatMessage selected = listWidget.selectedRow().orElse(null);
        if (selected == null) {
            return;
        }

        reviewScreen.appendCachedMessages(List.of(selected));
        close();
    }

    private void addAllVisible() {
        if (visibleEntries.isEmpty()) {
            return;
        }

        reviewScreen.appendCachedMessages(visibleEntries);
        close();
    }

    private void addLastTen() {
        if (visibleEntries.isEmpty()) {
            return;
        }

        reviewScreen.appendCachedMessages(visibleEntries.subList(0, Math.min(10, visibleEntries.size())));
        close();
    }

    private void reloadRows() {
        RecentChatCache chatCache = ScamScreenerRuntime.getInstance().recentChatCache();
        RecentChatCache.CachedChatMessage selected = listWidget == null ? null : listWidget.selectedRow().orElse(null);
        visibleEntries.clear();
        String playerFilter = currentPlayerFilter();
        for (RecentChatCache.CachedChatMessage entry : chatCache.entries()) {
            if (entry != null
                && entry.sourceType() == ChatSourceType.PLAYER
                && entry.matchesPlayerFilter(playerFilter)) {
                visibleEntries.add(entry);
            }
        }
        lastSeenCacheVersion = chatCache.version();

        if (listWidget != null) {
            listWidget.setRows(visibleEntries);
            listWidget.setSelectedIndex(indexOf(selected));
        }
        updateActionState();
    }

    private void updateActionState() {
        boolean hasVisibleEntries = !visibleEntries.isEmpty();
        boolean hasSelection = listWidget != null && listWidget.selectedRow().isPresent();
        if (addSelectedButton != null) {
            addSelectedButton.active = hasSelection;
        }
        if (addAllVisibleButton != null) {
            addAllVisibleButton.active = hasVisibleEntries;
        }
        if (addLastTenButton != null) {
            addLastTenButton.active = hasVisibleEntries;
        }
    }

    private void renderRow(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        RecentChatCache.CachedChatMessage row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        String header = row.displaySender() + " | " + row.sourceLabel();
        context.drawTextWithShadow(textRenderer, Text.literal(header), x, y, opaqueColor(0xFFFFFF));
        context.drawTextWithShadow(textRenderer, Text.literal(compact(row.cleanText(), 84)), x, y + 11, opaqueColor(0xCCCCCC));
    }

    private int indexOf(RecentChatCache.CachedChatMessage selected) {
        if (selected == null) {
            return -1;
        }

        for (int index = 0; index < visibleEntries.size(); index++) {
            RecentChatCache.CachedChatMessage entry = visibleEntries.get(index);
            if (entry != null
                && entry.capturedAtMs() == selected.capturedAtMs()
                && entry.cleanText().equals(selected.cleanText())
                && entry.displaySender().equals(selected.displaySender())) {
                return index;
            }
        }

        return -1;
    }

    private String currentPlayerFilter() {
        return playerFilterField == null ? "" : playerFilterField.getText();
    }

    private static String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
