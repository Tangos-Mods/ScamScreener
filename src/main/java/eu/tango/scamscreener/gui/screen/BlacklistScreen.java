package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.lists.BlacklistEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Blacklist management screen.
 */
public final class BlacklistScreen extends BaseScreen {
    private static final int ENTRIES_PER_PAGE = 7;
    private static final int SCORE_STEP = 10;

    private final BlacklistAccess blacklist;
    private final List<ButtonWidget> entryButtons = new ArrayList<>();
    private final List<BlacklistEntry> pageEntries = new ArrayList<>();

    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;
    private ButtonWidget scoreDownButton;
    private ButtonWidget scoreUpButton;
    private ButtonWidget setReasonButton;
    private ButtonWidget removeButton;
    private TextFieldWidget reasonInput;
    private int page;
    private UUID selectedUuid;
    private String selectedName = "";
    private int totalPages = 1;

    /**
     * Creates the blacklist screen using the shared runtime blacklist.
     *
     * @param parent the parent screen
     */
    public BlacklistScreen(Screen parent) {
        this(parent, ScamScreenerRuntime.getInstance().blacklist());
    }

    /**
     * Creates the blacklist screen.
     *
     * @param parent the parent screen
     * @param blacklist the blacklist access to manage
     */
    public BlacklistScreen(Screen parent, BlacklistAccess blacklist) {
        super(Text.literal("ScamScreener Blacklist"), parent);
        this.blacklist = blacklist;
    }

    @Override
    protected void init() {
        entryButtons.clear();
        pageEntries.clear();

        int buttonWidth = Math.min(420, Math.max(220, this.width - 30));
        int x = centeredX(buttonWidth);
        int y = 34;

        for (int index = 0; index < ENTRIES_PER_PAGE; index++) {
            final int indexOnPage = index;
            ButtonWidget rowButton = addDrawableChild(
                ButtonWidget.builder(Text.literal("-"), button -> selectEntry(indexOnPage))
                    .dimensions(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                    .build()
            );
            entryButtons.add(rowButton);
            y += ROW_HEIGHT;
        }

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        previousPageButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("< Previous"), button -> {
                page = Math.max(0, page - 1);
                refreshList();
            }).dimensions(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        nextPageButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Next >"), button -> {
                page = Math.min(Math.max(0, totalPages - 1), page + 1);
                refreshList();
            }).dimensions(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        y += ROW_HEIGHT;

        int thirdWidth = splitWidth(buttonWidth, 3, DEFAULT_SPLIT_GAP);
        scoreDownButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Score -" + SCORE_STEP), button -> adjustScore(-SCORE_STEP))
                .dimensions(x, y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        scoreUpButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Score +" + SCORE_STEP), button -> adjustScore(SCORE_STEP))
                .dimensions(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 1), y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelected())
                .dimensions(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 2), y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int saveButtonWidth = 52;
        int inputWidth = Math.max(80, buttonWidth - saveButtonWidth - DEFAULT_SPLIT_GAP);
        reasonInput = addDrawableChild(
            new TextFieldWidget(
                this.textRenderer,
                x,
                y,
                inputWidth,
                DEFAULT_BUTTON_HEIGHT,
                Text.literal("Reason")
            )
        );
        reasonInput.setMaxLength(64);
        setReasonButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Save"), button -> applyReasonInput())
                .dimensions(x + inputWidth + DEFAULT_SPLIT_GAP, y, saveButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(buttonWidth);
        refreshList();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Page " + (totalPages == 0 ? 0 : page + 1) + "/" + Math.max(1, totalPages)),
            this.width / 2,
            TITLE_Y + 12,
            opaqueColor(0xAAAAAA)
        );

        BlacklistEntry selected = selectedEntry();
        String selectedText = selected == null
            ? "Selected: none"
            : "Selected: " + displayName(selected.playerUuid(), selected.playerName())
                + " | score " + Math.max(0, selected.score())
                + " | " + safeReason(selected.reason());
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(selectedText),
            this.width / 2,
            this.height - 42,
            opaqueColor(0xCCCCCC)
        );
    }

    private void selectEntry(int indexOnPage) {
        if (indexOnPage < 0 || indexOnPage >= pageEntries.size()) {
            return;
        }

        BlacklistEntry entry = pageEntries.get(indexOnPage);
        selectedUuid = entry == null ? null : entry.playerUuid();
        selectedName = entry == null ? "" : safeName(entry.playerName());
        refreshList();
    }

    private void refreshList() {
        List<BlacklistEntry> allEntries = new ArrayList<>(blacklist.allEntries());
        allEntries.sort((left, right) -> displayName(left.playerUuid(), left.playerName())
            .toLowerCase(Locale.ROOT)
            .compareTo(displayName(right.playerUuid(), right.playerName()).toLowerCase(Locale.ROOT)));

        totalPages = Math.max(1, (int) Math.ceil(allEntries.size() / (double) ENTRIES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        if (!hasSelection(allEntries)) {
            selectedUuid = null;
            selectedName = "";
        }

        pageEntries.clear();
        int start = page * ENTRIES_PER_PAGE;
        for (int index = 0; index < ENTRIES_PER_PAGE; index++) {
            int absoluteIndex = start + index;
            ButtonWidget button = entryButtons.get(index);
            if (absoluteIndex >= allEntries.size()) {
                button.active = false;
                button.setMessage(Text.literal("-"));
                continue;
            }

            BlacklistEntry entry = allEntries.get(absoluteIndex);
            pageEntries.add(entry);
            button.active = true;
            button.setMessage(Text.literal(formatEntry(entry, matchesSelection(entry))));
        }

        if ((selectedUuid == null && selectedName.isBlank()) && !allEntries.isEmpty()) {
            BlacklistEntry firstEntry = allEntries.get(0);
            selectedUuid = firstEntry.playerUuid();
            selectedName = safeName(firstEntry.playerName());
            refreshList();
            return;
        }

        previousPageButton.active = page > 0;
        nextPageButton.active = page < totalPages - 1;
        updateActionButtons();
    }

    private void updateActionButtons() {
        BlacklistEntry selected = selectedEntry();
        boolean hasSelection = selected != null;
        scoreDownButton.active = hasSelection;
        scoreUpButton.active = hasSelection;
        removeButton.active = hasSelection;
        setReasonButton.active = hasSelection;
        if (reasonInput != null) {
            reasonInput.setEditable(hasSelection);
            reasonInput.active = hasSelection;
            String value = hasSelection ? safeReason(selected.reason()) : "";
            if (!value.equals(reasonInput.getText())) {
                reasonInput.setText(value);
            }
        }
    }

    private BlacklistEntry selectedEntry() {
        if (selectedUuid != null) {
            BlacklistEntry byUuid = blacklist.get(selectedUuid).orElse(null);
            if (byUuid != null) {
                return byUuid;
            }
        }
        if (!selectedName.isBlank()) {
            return blacklist.findByName(selectedName).orElse(null);
        }

        return null;
    }

    private void adjustScore(int delta) {
        BlacklistEntry selected = selectedEntry();
        if (selected == null) {
            return;
        }

        int updatedScore = Math.max(0, Math.min(100, selected.score() + delta));
        blacklist.add(selected.playerUuid(), selected.playerName(), updatedScore, selected.reason());
        refreshList();
    }

    private void applyReasonInput() {
        BlacklistEntry selected = selectedEntry();
        if (selected == null || reasonInput == null) {
            return;
        }

        blacklist.add(selected.playerUuid(), selected.playerName(), selected.score(), reasonInput.getText());
        refreshList();
    }

    private void removeSelected() {
        if (selectedUuid != null && blacklist.remove(selectedUuid)) {
            selectedUuid = null;
            selectedName = "";
            refreshList();
            return;
        }

        if (!selectedName.isBlank()) {
            blacklist.removeByName(selectedName);
            selectedUuid = null;
            selectedName = "";
            refreshList();
        }
    }

    private boolean hasSelection(List<BlacklistEntry> entries) {
        for (BlacklistEntry entry : entries) {
            if (matchesSelection(entry)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesSelection(BlacklistEntry entry) {
        if (entry == null) {
            return false;
        }

        if (selectedUuid != null && selectedUuid.equals(entry.playerUuid())) {
            return true;
        }

        String entryName = safeName(entry.playerName());
        return !selectedName.isBlank() && selectedName.equalsIgnoreCase(entryName);
    }

    private static String formatEntry(BlacklistEntry entry, boolean selected) {
        if (entry == null) {
            return "-";
        }

        String reason = safeReason(entry.reason());
        if (reason.length() > 22) {
            reason = reason.substring(0, 19) + "...";
        }

        return (selected ? "> " : "  ")
            + displayName(entry.playerUuid(), entry.playerName())
            + " | "
            + Math.max(0, entry.score())
            + " | "
            + reason;
    }

    private static String displayName(UUID playerUuid, String playerName) {
        String normalizedName = safeName(playerName);
        if (!normalizedName.isBlank()) {
            return normalizedName;
        }
        if (playerUuid != null) {
            return playerUuid.toString();
        }

        return "<unknown>";
    }

    private static String safeName(String playerName) {
        return playerName == null ? "" : playerName.trim();
    }

    private static String safeReason(String reason) {
        return reason == null ? "" : reason;
    }
}
