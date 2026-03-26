package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.lists.BlacklistEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Blacklist management screen.
 */
public final class BlacklistScreen extends BaseScreen {
    private static final int ENTRIES_PER_PAGE = 7;
    private static final int SCORE_STEP = 10;

    private final BlacklistAccess blacklist;
    private final List<Button> entryButtons = new ArrayList<>();
    private final List<BlacklistEntry> pageEntries = new ArrayList<>();

    private Button previousPageButton;
    private Button nextPageButton;
    private Button scoreDownButton;
    private Button scoreUpButton;
    private Button setReasonButton;
    private Button removeButton;
    private EditBox reasonInput;
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
        super(Component.literal("ScamScreener Blacklist"), parent);
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
            Button rowButton = addRenderableWidget(
                Button.builder(Component.literal("-"), button -> selectEntry(indexOnPage))
                    .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                    .build()
            );
            entryButtons.add(rowButton);
            y += ROW_HEIGHT;
        }

        int halfWidth = splitWidth(buttonWidth, 2, DEFAULT_SPLIT_GAP);
        previousPageButton = addRenderableWidget(
            Button.builder(Component.literal("< Previous"), button -> {
                page = Math.max(0, page - 1);
                refreshList();
            }).bounds(x, y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        nextPageButton = addRenderableWidget(
            Button.builder(Component.literal("Next >"), button -> {
                page = Math.min(Math.max(0, totalPages - 1), page + 1);
                refreshList();
            }).bounds(columnX(x, halfWidth, DEFAULT_SPLIT_GAP, 1), y, halfWidth, DEFAULT_BUTTON_HEIGHT).build()
        );
        y += ROW_HEIGHT;

        int thirdWidth = splitWidth(buttonWidth, 3, DEFAULT_SPLIT_GAP);
        scoreDownButton = addRenderableWidget(
            Button.builder(Component.literal("Score -" + SCORE_STEP), button -> adjustScore(-SCORE_STEP))
                .bounds(x, y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        scoreUpButton = addRenderableWidget(
            Button.builder(Component.literal("Score +" + SCORE_STEP), button -> adjustScore(SCORE_STEP))
                .bounds(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 1), y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addRenderableWidget(
            Button.builder(Component.literal("Remove"), button -> removeSelected())
                .bounds(columnX(x, thirdWidth, DEFAULT_SPLIT_GAP, 2), y, thirdWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        int saveButtonWidth = 52;
        int inputWidth = Math.max(80, buttonWidth - saveButtonWidth - DEFAULT_SPLIT_GAP);
        reasonInput = addRenderableWidget(
            new EditBox(
                this.font,
                x,
                y,
                inputWidth,
                DEFAULT_BUTTON_HEIGHT,
                Component.literal("Reason")
            )
        );
        reasonInput.setMaxLength(64);
        setReasonButton = addRenderableWidget(
            Button.builder(Component.literal("Save"), button -> applyReasonInput())
                .bounds(x + inputWidth + DEFAULT_SPLIT_GAP, y, saveButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(buttonWidth);
        refreshList();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        context.centeredText(
            this.font,
            Component.literal("Page " + (totalPages == 0 ? 0 : page + 1) + "/" + Math.max(1, totalPages)),
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
        context.centeredText(
            this.font,
            Component.literal(selectedText),
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
            Button button = entryButtons.get(index);
            if (absoluteIndex >= allEntries.size()) {
                button.active = false;
                button.setMessage(Component.literal("-"));
                continue;
            }

            BlacklistEntry entry = allEntries.get(absoluteIndex);
            pageEntries.add(entry);
            button.active = true;
            button.setMessage(Component.literal(formatEntry(entry, matchesSelection(entry))));
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
            if (!value.equals(reasonInput.getValue())) {
                reasonInput.setValue(value);
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

        blacklist.add(selected.playerUuid(), selected.playerName(), selected.score(), reasonInput.getValue());
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
