package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.review.ReviewActionHandler;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Review list screen using the case-oriented review workflow.
 */
public final class ReviewScreen extends BaseScreen {
    private static final String TRAINING_HUB_URL = "https://scamscreener.creepans.net/";
    private static final int LIST_ROW_HEIGHT = 28;
    private static final int FILTER_BUTTON_WIDTH = 120;
    private static final int ACTION_COLUMNS = 4;
    private static final int ACTION_AREA_HEIGHT = (DEFAULT_BUTTON_HEIGHT * 3) + 22;

    private final List<ReviewEntry> rows = new ArrayList<>();

    private ReviewFilter activeFilter = ReviewFilter.ALL;
    private SelectableListWidget<ReviewEntry> listWidget;
    private TextFieldWidget searchField;
    private ButtonWidget filterButton;
    private ButtonWidget markRiskButton;
    private ButtonWidget markSafeButton;
    private ButtonWidget ignoreButton;
    private ButtonWidget resetVisibleButton;
    private ButtonWidget removeButton;
    private ButtonWidget clearVisibleButton;
    private ButtonWidget contributeTrainingButton;

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
            ButtonWidget.builder(Text.literal("Review Case"), button -> openCaseReview())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        markSafeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Info"), button -> openInfo())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        ignoreButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Dismiss"), button -> setSelectedVerdict(ReviewVerdict.IGNORED))
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        resetVisibleButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("New Case"), button -> openNewCase())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 3), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        buttonY += DEFAULT_BUTTON_HEIGHT + 4;
        int lowerButtonWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelectedEntry())
                .dimensions(contentX, buttonY, lowerButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        clearVisibleButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear Visible"), button -> clearVisible())
                .dimensions(columnX(contentX, lowerButtonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, lowerButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        int footerButtonWidth = splitWidth(contentWidth, 3, DEFAULT_SPLIT_GAP);
        addFooterButton(contentX, footerButtonWidth, Text.literal("Review Settings"), button -> this.client.setScreen(new ReviewSettingsScreen(this)));
        contributeTrainingButton = addFooterButton(
            columnX(contentX, footerButtonWidth, DEFAULT_SPLIT_GAP, 1),
            footerButtonWidth,
            Text.literal("Contribute Training Data"),
            button -> contributeTrainingData()
        );
        addFooterButton(
            columnX(contentX, footerButtonWidth, DEFAULT_SPLIT_GAP, 2),
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
        drawLine(context, left, CONTENT_TOP + 36, "Select a case or start a New Case, then use Review Case to annotate context and signals.");
        drawLine(context, left, CONTENT_TOP + 48, "Training Hub contribution will be available soon.");

        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean doubleClick) {
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

    private void clearVisible() {
        List<String> entryIds = new ArrayList<>();
        for (ReviewEntry row : rows) {
            entryIds.add(row.getId());
        }
        for (String entryId : entryIds) {
            ScamScreenerRuntime.getInstance().reviewStore().remove(entryId);
        }
        reloadRows(null);
    }

    private void removeSelectedEntry() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null) {
            return;
        }

        ReviewActionHandler.remove(entry);
        reloadRows(null);
    }

    private void openCaseReview() {
        ReviewEntry entry = selectedEntry().orElse(null);
        if (entry == null || this.client == null) {
            return;
        }

        AlertContextRegistry.AlertContext context = AlertContextRegistry.createReviewContext(entry).orElse(null);
        if (context == null) {
            return;
        }

        this.client.setScreen(new AlertManageScreen(this, context));
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

    private void openNewCase() {
        if (this.client == null) {
            return;
        }

        this.client.setScreen(new AlertManageScreen(this, null));
    }

    private void exportTrainingCases() {
        MessageDispatcher.reply(ClientMessages.trainingCasesExportStarted());
        ScamScreenerRuntime.getInstance().trainingCaseExportService()
            .exportReviewedCasesAsync(ScamScreenerRuntime.getInstance().reviewStore().entries())
            .whenComplete((exportResult, throwable) -> {
                if (throwable != null) {
                    MessageDispatcher.reply(ClientMessages.trainingCasesExportFailed(rootCauseMessage(throwable)));
                    return;
                }

                MessageDispatcher.reply(ClientMessages.trainingCasesExported(exportResult));
            });
    }

    private void contributeTrainingData() {
        exportTrainingCases();
        openTrainingHub();
    }

    private void openTrainingHub() {
        if (this.client == null) {
            MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed("Client unavailable."));
            return;
        }

        this.client.setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
                try {
                    Util.getOperatingSystem().open(TRAINING_HUB_URL);
                } catch (Exception exception) {
                    MessageDispatcher.reply(ClientMessages.trainingHubOpenFailed(exception.getMessage()));
                }
            }
            this.client.setScreen(this);
        }, TRAINING_HUB_URL, true));
    }

    private void updateActionState() {
        boolean hasVisibleRows = !rows.isEmpty();
        boolean hasSelection = selectedEntry().isPresent();

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
            resetVisibleButton.active = true;
        }
        if (removeButton != null) {
            removeButton.active = hasSelection;
        }
        if (clearVisibleButton != null) {
            clearVisibleButton.active = hasVisibleRows;
        }
        if (contributeTrainingButton != null) {
            contributeTrainingButton.active = false;
        }
    }

    private int count(ReviewVerdict target) {
        int count = 0;
        for (ReviewEntry row : rows) {
            if (row.getVerdict() == target) {
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
            rows.add(entry);
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
            if (rowId.equals(rows.get(index).getId())) {
                return index;
            }
        }

        return -1;
    }

    private void renderRow(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        ReviewEntry row,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        String header = "[" + marker(row.getVerdict()) + "] (" + row.getScore() + ") " + entryDisplayName(row);

        context.drawTextWithShadow(textRenderer, Text.literal(header), x, y, opaqueColor(color(row.getVerdict())));
        context.drawTextWithShadow(textRenderer, Text.literal(entryCompactMessage(row)), x, y + 11, opaqueColor(0xFFFFFF));
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

    private Optional<ReviewEntry> selectedEntry() {
        if (listWidget == null) {
            return Optional.empty();
        }

        return listWidget.selectedRow();
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

        ReviewEntry row = listWidget.selectedRow().orElse(null);
        if (row == null || row.getId().isBlank()) {
            return null;
        }

        return row.getId();
    }

    private String searchSummary() {
        String currentSearch = currentSearch().trim();
        if (currentSearch.isEmpty()) {
            return "-";
        }

        return currentSearch;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause instanceof CompletionException && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause == null ? null : rootCause.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }

    private static String entryDisplayName(ReviewEntry entry) {
        if (entry == null) {
            return "Case";
        }

        String numericId = trailingNumericId(entry.getId());
        if (isManual(entry)) {
            return numericId.isBlank() ? "Manual Case" : "Manual Case #" + numericId;
        }

        return numericId.isBlank() ? "Case" : "Case #" + numericId;
    }

    private static String entryCompactMessage(ReviewEntry entry) {
        String summary = caseSummary(entry);
        if (summary.length() <= 72) {
            return summary;
        }

        return summary.substring(0, 69) + "...";
    }

    private static String caseSummary(ReviewEntry entry) {
        if (entry == null) {
            return "";
        }

        int messageCount = 0;
        int signalCount = 0;
        if (entry.hasCaseMessages()) {
            for (ReviewCaseMessage caseMessage : entry.getCaseMessages()) {
                if (caseMessage == null || caseMessage.getCleanText().isBlank()) {
                    continue;
                }
                messageCount++;
                if (caseMessage.isSignalMessage()) {
                    signalCount++;
                }
            }
        }
        if (messageCount == 0 && entry.getMessage() != null && !entry.getMessage().isBlank()) {
            messageCount = 1;
        }

        String stageLabel = entry.getDecidedByStage() == null || entry.getDecidedByStage().isBlank()
            ? "Manual"
            : entry.getDecidedByStage().trim();
        return messageCount + " messages | " + signalCount + " signals | " + stageLabel;
    }

    private static boolean isManual(ReviewEntry entry) {
        return entry != null && (entry.getDecidedByStage() == null || entry.getDecidedByStage().isBlank());
    }

    private static String trailingNumericId(String rowId) {
        if (rowId == null || rowId.isBlank()) {
            return "";
        }

        int separatorIndex = rowId.lastIndexOf('-');
        return separatorIndex < 0 ? rowId.trim() : rowId.substring(separatorIndex + 1).trim();
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
