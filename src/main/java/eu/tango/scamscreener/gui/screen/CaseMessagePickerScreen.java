package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Picker for adding cached inbound chat lines into one review case.
 */
public final class CaseMessagePickerScreen extends BaseScreen {
    private static final int LIST_ROW_HEIGHT = 28;

    private final AlertManageScreen reviewScreen;
    private final String initialPlayerFilter;
    private final List<RecentChatCache.CachedChatMessage> visibleEntries = new ArrayList<>();

    private SelectableListWidget<RecentChatCache.CachedChatMessage> listWidget;
    private EditBox playerFilterField;
    private Button addSelectedButton;
    private Button addAllVisibleButton;
    private Button addLastTenButton;
    private long lastSeenCacheVersion = -1L;

    public CaseMessagePickerScreen(AlertManageScreen reviewScreen, String initialPlayerFilter) {
        super(Component.literal("Add Case Messages"), reviewScreen);
        this.reviewScreen = reviewScreen;
        this.initialPlayerFilter = initialPlayerFilter == null ? "" : initialPlayerFilter.trim();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(620, Math.max(360, this.width - 40));
        int contentX = centeredX(contentWidth);
        int controlsY = CONTENT_TOP + 24;

        playerFilterField = addRenderableWidget(
            new EditBox(
                this.font,
                contentX,
                controlsY,
                contentWidth,
                DEFAULT_BUTTON_HEIGHT,
                Component.literal("Player Filter")
            )
        );
        playerFilterField.setMaxLength(64);
        playerFilterField.setSuggestion("Filter by player name...");
        playerFilterField.setValue(initialPlayerFilter);
        playerFilterField.setResponder(value -> reloadRows());

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
        addSelectedButton = addRenderableWidget(
            Button.builder(Component.literal("Add Selected"), button -> addSelected())
                .bounds(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addAllVisibleButton = addRenderableWidget(
            Button.builder(Component.literal("Add All Visible"), button -> addAllVisible())
                .bounds(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addLastTenButton = addRenderableWidget(
            Button.builder(Component.literal("Add Last 10"), button -> addLastTen())
                .bounds(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        int left = centeredX(Math.min(620, Math.max(360, this.width - 40)));
        drawLine(context, left, CONTENT_TOP, "Pick cached chat lines, filter by player, then add one, all visible, or the latest 10 visible.");
        if (listWidget != null) {
            listWidget.render(context, this.font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event != null && event.button() == 0 && listWidget != null && listWidget.mouseClicked(event.x(), event.y(), event.button())) {
            updateActionState();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
        onClose();
    }

    private void addAllVisible() {
        if (visibleEntries.isEmpty()) {
            return;
        }

        reviewScreen.appendCachedMessages(visibleEntries);
        onClose();
    }

    private void addLastTen() {
        if (visibleEntries.isEmpty()) {
            return;
        }

        reviewScreen.appendCachedMessages(visibleEntries.subList(0, Math.min(10, visibleEntries.size())));
        onClose();
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
        GuiGraphicsExtractor context,
        net.minecraft.client.gui.Font textRenderer,
        RecentChatCache.CachedChatMessage row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        String header = row.displaySender() + " | " + row.sourceLabel();
        context.text(textRenderer, Component.literal(header), x, y, opaqueColor(0xFFFFFF));
        context.text(textRenderer, Component.literal(compact(row.cleanText(), 84)), x, y + 11, opaqueColor(0xCCCCCC));
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
        return playerFilterField == null ? "" : playerFilterField.getValue();
    }

    private static String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
