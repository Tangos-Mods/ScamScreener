package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.lists.WhitelistEntry;
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
 * Whitelist management screen.
 */
public final class WhitelistScreen extends BaseScreen {
    private static final int ENTRIES_PER_PAGE = 8;

    private final WhitelistAccess whitelist;
    private final List<Button> entryButtons = new ArrayList<>();
    private final List<WhitelistEntry> pageEntries = new ArrayList<>();

    private Button previousPageButton;
    private Button nextPageButton;
    private Button removeButton;
    private EditBox addInput;
    private int page;
    private int totalPages = 1;
    private UUID selectedUuid;
    private String selectedName = "";

    /**
     * Creates the whitelist screen using the shared runtime whitelist.
     *
     * @param parent the parent screen
     */
    public WhitelistScreen(Screen parent) {
        this(parent, ScamScreenerRuntime.getInstance().whitelist());
    }

    /**
     * Creates the whitelist screen.
     *
     * @param parent the parent screen
     * @param whitelist the whitelist access to manage
     */
    public WhitelistScreen(Screen parent, WhitelistAccess whitelist) {
        super(Component.literal("ScamScreener Whitelist"), parent);
        this.whitelist = whitelist;
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

        int addButtonWidth = 72;
        int addInputWidth = Math.max(80, buttonWidth - addButtonWidth - DEFAULT_SPLIT_GAP);
        addInput = addRenderableWidget(
            new EditBox(
                this.font,
                x,
                y,
                addInputWidth,
                DEFAULT_BUTTON_HEIGHT,
                Component.literal("Player Name")
            )
        );
        addInput.setMaxLength(36);
        addRenderableWidget(
            Button.builder(Component.literal("Add"), button -> addEntryFromInput())
                .bounds(x + addInputWidth + DEFAULT_SPLIT_GAP, y, addButtonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        removeButton = addRenderableWidget(
            Button.builder(Component.literal("Remove Selected"), button -> removeSelected())
                .bounds(x, y, buttonWidth, DEFAULT_BUTTON_HEIGHT)
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

        String selectedText = selectedUuid == null && selectedName.isBlank()
            ? "Selected: none"
            : "Selected: " + displayName(selectedUuid, selectedName);
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

        WhitelistEntry entry = pageEntries.get(indexOnPage);
        selectedUuid = entry == null ? null : entry.playerUuid();
        selectedName = entry == null ? "" : safeName(entry.playerName());
        refreshList();
    }

    private void refreshList() {
        List<WhitelistEntry> allEntries = new ArrayList<>(whitelist.allEntries());
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

            WhitelistEntry entry = allEntries.get(absoluteIndex);
            pageEntries.add(entry);
            button.active = true;
            button.setMessage(Component.literal(formatEntry(entry, matchesSelection(entry))));
        }

        if ((selectedUuid == null && selectedName.isBlank()) && !allEntries.isEmpty()) {
            WhitelistEntry firstEntry = allEntries.get(0);
            selectedUuid = firstEntry.playerUuid();
            selectedName = safeName(firstEntry.playerName());
            refreshList();
            return;
        }

        previousPageButton.active = page > 0;
        nextPageButton.active = page < totalPages - 1;
        removeButton.active = selectedUuid != null || !selectedName.isBlank();
    }

    private void addEntryFromInput() {
        if (addInput == null) {
            return;
        }

        String rawValue = addInput.getValue();
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }

        String trimmedValue = rawValue.trim();
        UUID playerUuid = tryParseUuid(trimmedValue);
        String playerName = playerUuid == null ? trimmedValue : "";
        if (!whitelist.add(playerUuid, playerName)) {
            return;
        }

        selectedUuid = playerUuid;
        selectedName = playerName;
        addInput.setValue("");
        refreshList();
    }

    private void removeSelected() {
        if (selectedUuid != null && whitelist.remove(selectedUuid)) {
            selectedUuid = null;
            selectedName = "";
            refreshList();
            return;
        }

        if (!selectedName.isBlank()) {
            whitelist.removeByName(selectedName);
            selectedUuid = null;
            selectedName = "";
            refreshList();
        }
    }

    private boolean hasSelection(List<WhitelistEntry> entries) {
        for (WhitelistEntry entry : entries) {
            if (matchesSelection(entry)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesSelection(WhitelistEntry entry) {
        if (entry == null) {
            return false;
        }

        if (selectedUuid != null && selectedUuid.equals(entry.playerUuid())) {
            return true;
        }

        String entryName = safeName(entry.playerName());
        return !selectedName.isBlank() && selectedName.equalsIgnoreCase(entryName);
    }

    private static String formatEntry(WhitelistEntry entry, boolean selected) {
        if (entry == null) {
            return "-";
        }

        return (selected ? "> " : "  ")
            + displayName(entry.playerUuid(), entry.playerName())
            + " | "
            + (entry.playerUuid() == null ? "-" : entry.playerUuid());
    }

    private static UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
}
