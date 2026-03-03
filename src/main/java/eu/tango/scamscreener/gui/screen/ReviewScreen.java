package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.data.ReviewRow;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.review.ReviewActionHandler;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Review list screen using the dense v1 review workflow.
 */
public final class ReviewScreen extends BaseListScreen {
    private static final int LIST_ROW_HEIGHT = 28;
    private static final int FILTER_BUTTON_WIDTH = 120;
    private static final int ACTION_COLUMNS = 4;
    private static final int ACTION_AREA_HEIGHT = (DEFAULT_BUTTON_HEIGHT * 3) + 22;

    private final List<ReviewRow> rows = new ArrayList<>();

    private ReviewFilter activeFilter = ReviewFilter.ALL;
    private SelectableListWidget<ReviewRow> listWidget;
    private TextFieldWidget searchField;
    private ButtonWidget filterButton;
    private ButtonWidget markRiskButton;
    private ButtonWidget markSafeButton;
    private ButtonWidget ignoreButton;
    private ButtonWidget resetVisibleButton;
    private ButtonWidget blacklistButton;
    private ButtonWidget whitelistButton;
    private ButtonWidget removeButton;
    private ButtonWidget clearVisibleButton;
    private ButtonWidget detailsButton;

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
        int contentWidth = Math.min(620, Math.max(360, this.width - 40));
        int contentX = centeredX(contentWidth);
        int controlsY = CONTENT_TOP + 56;
        int searchWidth = Math.max(140, contentWidth - FILTER_BUTTON_WIDTH - DEFAULT_SPLIT_GAP);

        filterButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> cycleFilter())
                .dimensions(contentX, controlsY, FILTER_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        searchField = addDrawableChild(
            new TextFieldWidget(
                this.textRenderer,
                contentX + FILTER_BUTTON_WIDTH + DEFAULT_SPLIT_GAP,
                controlsY,
                searchWidth,
                DEFAULT_BUTTON_HEIGHT,
                Text.literal("Review Search")
            )
        );
        searchField.setMaxLength(64);
        searchField.setChangedListener(value -> reloadRows());

        int listY = controlsY + ROW_HEIGHT;
        int listHeight = Math.max(80, footerY() - listY - ACTION_AREA_HEIGHT - 10);
        listWidget = new SelectableListWidget<>(
            contentX,
            listY,
            contentWidth,
            listHeight,
            LIST_ROW_HEIGHT,
            this::renderRow
        );

        int buttonWidth = splitWidth(contentWidth, ACTION_COLUMNS, DEFAULT_SPLIT_GAP);
        int buttonY = listY + listHeight + 10;

        markRiskButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Mark Risk"), button -> setSelectedVerdict(ReviewVerdict.RISK))
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        markSafeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Mark Safe"), button -> setSelectedVerdict(ReviewVerdict.SAFE))
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        ignoreButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Dismiss"), button -> setSelectedVerdict(ReviewVerdict.IGNORED))
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        resetVisibleButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset Visible"), button -> resetVisibleChoices())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 3), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        buttonY += DEFAULT_BUTTON_HEIGHT + 4;
        blacklistButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("To Blacklist"), button -> addSelectedToBlacklist())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        whitelistButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("To Whitelist"), button -> addSelectedToWhitelist())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelectedEntry())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        clearVisibleButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear Visible"), button -> clearVisible())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 3), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        buttonY += DEFAULT_BUTTON_HEIGHT + 4;
        detailsButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Info"), button -> openInfo())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        int footerButtonWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);
        addFooterButton(contentX, footerButtonWidth, Text.literal("Review Settings"), button -> this.client.setScreen(new ReviewSettingsScreen(this)));
        addFooterButton(
            columnX(contentX, footerButtonWidth, DEFAULT_SPLIT_GAP, 1),
            footerButtonWidth,
            Text.literal("Back"),
            button -> close()
        );
        refreshFilterButton();
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

        int left = centeredX(Math.min(620, Math.max(360, this.width - 40)));
        drawSectionTitle(context, left, CONTENT_TOP, "Review Summary");
        drawLine(
            context,
            left,
            CONTENT_TOP + 12,
            "Open " + count(ReviewVerdict.PENDING)
                + " | Risk " + count(ReviewVerdict.RISK)
                + " | Safe " + count(ReviewVerdict.SAFE)
                + " | Dismissed " + count(ReviewVerdict.IGNORED)
        );
        drawLine(
            context,
            left,
            CONTENT_TOP + 24,
            "Showing " + rows.size() + " of " + ScamScreenerRuntime.getInstance().reviewStore().entries().size()
                + " | Filter " + activeFilter.label()
                + " | Search " + searchSummary()
        );
        drawLine(context, left, CONTENT_TOP + 36, "Click a selected row again to cycle Open/Risk/Safe/Dismissed.");

        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        if (button != 0 || listWidget == null) {
            return false;
        }

        int previousSelection = listWidget.selectedIndex();
        if (!listWidget.mouseClicked(mouseX, mouseY, button)) {
            return false;
        }

        if (previousSelection >= 0 && previousSelection == listWidget.selectedIndex()) {
            cycleSelectedVerdict();
            return true;
        }

        updateActionState();
        return true;
    }

    @Override
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private void cycleFilter() {
        activeFilter = activeFilter.next();
        refreshFilterButton();
        reloadRows();
    }

    private void setSelectedVerdict(ReviewVerdict verdict) {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null) {
            return;
        }

        ReviewActionHandler.setVerdict(entry, verdict);
        reloadRows(entry.getId());
    }

    private void cycleSelectedVerdict() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null) {
            return;
        }

        ReviewActionHandler.setVerdict(entry, nextVerdict(entry.getVerdict()));
        reloadRows(entry.getId());
    }

    private void resetVisibleChoices() {
        String selectedRowId = selectedRowId();
        for (ReviewRow row : rows) {
            ScamScreenerRuntime.getInstance().reviewStore().setVerdict(row.rowId(), ReviewVerdict.PENDING);
        }
        reloadRows(selectedRowId);
    }

    private void clearVisible() {
        List<String> entryIds = new ArrayList<>();
        for (ReviewRow row : rows) {
            entryIds.add(row.rowId());
        }
        for (String entryId : entryIds) {
            ScamScreenerRuntime.getInstance().reviewStore().remove(entryId);
        }
        reloadRows(null);
    }

    private void addSelectedToBlacklist() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (!ReviewActionHandler.hasPlayerTarget(entry)) {
            return;
        }

        ReviewActionHandler.addToBlacklist(entry);
        reloadRows(entry.getId());
    }

    private void addSelectedToWhitelist() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (!ReviewActionHandler.hasPlayerTarget(entry)) {
            return;
        }

        ReviewActionHandler.addToWhitelist(entry);
        reloadRows(entry.getId());
    }

    private void removeSelectedEntry() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null) {
            return;
        }

        ReviewActionHandler.remove(entry);
        reloadRows(null);
    }

    private void openInfo() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null || this.client == null) {
            return;
        }

        AlertContextRegistry.AlertContext context = AlertContextRegistry.createReviewContext(entry).orElse(null);
        if (context == null) {
            return;
        }

        this.client.setScreen(new AlertInfoScreen(this, context));
    }

    private void updateActionState() {
        boolean hasVisibleRows = !rows.isEmpty();
        boolean hasSelection = selectedEntry().isPresent();
        boolean hasSelectionTarget = ReviewActionHandler.hasPlayerTarget(selectedEntry().orElse(null));

        if (markRiskButton != null) {
            markRiskButton.active = hasSelection;
        }
        if (markSafeButton != null) {
            markSafeButton.active = hasSelection;
        }
        if (ignoreButton != null) {
            ignoreButton.active = hasSelection;
        }
        if (resetVisibleButton != null) {
            resetVisibleButton.active = hasVisibleRows;
        }
        if (blacklistButton != null) {
            blacklistButton.active = hasSelectionTarget;
        }
        if (whitelistButton != null) {
            whitelistButton.active = hasSelectionTarget;
        }
        if (removeButton != null) {
            removeButton.active = hasSelection;
        }
        if (clearVisibleButton != null) {
            clearVisibleButton.active = hasVisibleRows;
        }
        if (detailsButton != null) {
            detailsButton.active = hasSelection;
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
        reloadRows(selectedRowId());
    }

    private void reloadRows(String selectedRowId) {
        rows.clear();
        for (ReviewEntry entry : ScamScreenerRuntime.getInstance().reviewStore().entries(activeFilter.verdict(), currentSearch())) {
            rows.add(ReviewRow.fromEntry(entry));
        }

        if (listWidget != null) {
            listWidget.setRows(rows);
            listWidget.setSelectedIndex(indexOfRow(selectedRowId));
        }

        updateActionState();
    }

    private int indexOfRow(String rowId) {
        if (rowId == null || rowId.isBlank()) {
            return -1;
        }

        for (int index = 0; index < rows.size(); index++) {
            if (rowId.equals(rows.get(index).rowId())) {
                return index;
            }
        }

        return -1;
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

        context.drawTextWithShadow(textRenderer, Text.literal(header), x, y, opaqueColor(color(row.verdict())));
        context.drawTextWithShadow(textRenderer, Text.literal(row.compactMessage()), x, y + 11, opaqueColor(0xFFFFFF));
    }

    private String marker(ReviewVerdict verdict) {
        return switch (verdict == null ? ReviewVerdict.PENDING : verdict) {
            case PENDING -> "O";
            case RISK -> "R";
            case SAFE -> "S";
            case IGNORED -> "D";
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

    private void refreshFilterButton() {
        if (filterButton != null) {
            filterButton.setMessage(Text.literal("Filter: " + activeFilter.label()));
        }
    }

    private ReviewVerdict nextVerdict(ReviewVerdict verdict) {
        return switch (verdict == null ? ReviewVerdict.PENDING : verdict) {
            case PENDING -> ReviewVerdict.RISK;
            case RISK -> ReviewVerdict.SAFE;
            case SAFE -> ReviewVerdict.IGNORED;
            case IGNORED -> ReviewVerdict.PENDING;
        };
    }

    private Optional<ReviewEntry> selectedEntry() {
        if (listWidget == null) {
            return Optional.empty();
        }

        ReviewRow row = listWidget.selectedRow().orElse(null);
        if (row == null) {
            return Optional.empty();
        }

        return ScamScreenerRuntime.getInstance().reviewStore().find(row.rowId());
    }

    private String currentSearch() {
        if (searchField == null) {
            return "";
        }

        return searchField.getText();
    }

    private String selectedRowId() {
        if (listWidget == null) {
            return null;
        }

        ReviewRow row = listWidget.selectedRow().orElse(null);
        if (row == null || row.rowId().isBlank()) {
            return null;
        }

        return row.rowId();
    }

    private String searchSummary() {
        String currentSearch = currentSearch().trim();
        if (currentSearch.isEmpty()) {
            return "-";
        }

        return currentSearch;
    }

    private enum ReviewFilter {
        ALL("All", null),
        PENDING("Open", ReviewVerdict.PENDING),
        RISK("Risk", ReviewVerdict.RISK),
        SAFE("Safe", ReviewVerdict.SAFE),
        IGNORED("Dismissed", ReviewVerdict.IGNORED);

        private final String label;
        private final ReviewVerdict verdict;

        ReviewFilter(String label, ReviewVerdict verdict) {
            this.label = label;
            this.verdict = verdict;
        }

        public String label() {
            return label;
        }

        public ReviewVerdict verdict() {
            return verdict;
        }

        public ReviewFilter next() {
            ReviewFilter[] values = values();
            int nextIndex = (ordinal() + 1) % values.length;
            return values[nextIndex];
        }
    }
}
