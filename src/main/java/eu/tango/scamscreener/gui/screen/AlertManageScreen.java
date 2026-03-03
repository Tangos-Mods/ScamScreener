package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.ChatLineClassifier;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.lists.BlacklistSource;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.review.ReviewActionHandler;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Alert-specific review screen that mirrors the old v1 alert-manage flow.
 */
public final class AlertManageScreen extends BaseScreen {
    private static final int HEADER_Y_OFFSET = 12;
    private static final int ACTION_ROW_OFFSET = 24;
    private static final int LIST_TOP = 48;
    private static final int LIST_ROW_HEIGHT = 20;
    private static final int CHECKBOX_HEIGHT = 20;
    private static final int CHECKBOX_GAP = 4;
    private static final int ACTION_BUTTON_GAP = 12;
    private static final int LIST_BOTTOM_PADDING = 8;

    private final AlertContextRegistry.AlertContext context;
    private final List<ReviewRow> sourceRows = new ArrayList<>();
    private final List<SelectionState> states = new ArrayList<>();

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int maxVisibleRows;
    private int scrollOffsetRows;

    private SimpleCheckbox blacklistCheckbox;
    private SimpleCheckbox blockCheckbox;

    /**
     * Creates the v1-style alert review screen for one recent alert.
     *
     * @param parent the parent screen to return to
     * @param context the alert context to review
     */
    public AlertManageScreen(Screen parent, AlertContextRegistry.AlertContext context) {
        super(Text.literal("Manage Alert"), parent);
        this.context = context;
        sourceRows.addAll(buildRows(context));
        for (ReviewRow row : sourceRows) {
            states.add(SelectionState.fromReviewRow(row));
        }
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(620, Math.max(260, this.width - 30));
        int contentX = centeredX(contentWidth);
        int buttonY = this.height - FOOTER_MARGIN - ACTION_ROW_OFFSET;
        int checkboxY = buttonY - (CHECKBOX_HEIGHT * 2) - CHECKBOX_GAP;

        listX = contentX;
        listY = LIST_TOP;
        listWidth = contentWidth;
        listHeight = Math.max(80, checkboxY - listY - LIST_BOTTOM_PADDING);
        maxVisibleRows = Math.max(1, listHeight / LIST_ROW_HEIGHT);

        blacklistCheckbox = new SimpleCheckbox(
            contentX,
            checkboxY,
            contentWidth,
            CHECKBOX_HEIGHT,
            "Add player to ScamScreener blacklist",
            false,
            hasPlayerTarget()
        );
        blockCheckbox = new SimpleCheckbox(
            contentX,
            checkboxY + CHECKBOX_HEIGHT + CHECKBOX_GAP,
            contentWidth,
            CHECKBOX_HEIGHT,
            "Add player to Hypixel /block list",
            false,
            hasPlayerTarget()
        );

        int buttonWidth = splitWidth(contentWidth, 3, ACTION_BUTTON_GAP);
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Save"), button -> submit(false))
                .dimensions(columnX(contentX, buttonWidth, ACTION_BUTTON_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Save & Upload"), button -> submit(true))
                .dimensions(columnX(contentX, buttonWidth, ACTION_BUTTON_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int headerY = TITLE_Y + HEADER_Y_OFFSET;
        if (this.context != null) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(headerLine()),
                this.width / 2,
                headerY,
                opaqueColor(0xCCCCCC)
            );
        }
        renderSelectedSummary(context, headerY + this.textRenderer.fontHeight + 2);
        renderList(context, mouseX, mouseY);
        if (blacklistCheckbox != null) {
            blacklistCheckbox.render(context, this.textRenderer, mouseX, mouseY);
        }
        if (blockCheckbox != null) {
            blockCheckbox.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean doubleClick) {
        if (event != null && handleMouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInsideList(mouseX, mouseY) && sourceRows.size() > maxVisibleRows) {
            int delta = verticalAmount > 0 ? -1 : 1;
            int maxOffset = Math.max(0, sourceRows.size() - maxVisibleRows);
            scrollOffsetRows = clamp(scrollOffsetRows + delta, 0, maxOffset);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideList(mouseX, mouseY)) {
            int relativeY = (int) (mouseY - listY);
            int rowIndex = relativeY / LIST_ROW_HEIGHT;
            int absoluteIndex = scrollOffsetRows + rowIndex;
            if (absoluteIndex >= 0 && absoluteIndex < states.size()) {
                states.set(absoluteIndex, states.get(absoluteIndex).next());
                return true;
            }
        }

        if (blacklistCheckbox != null && blacklistCheckbox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (blockCheckbox != null && blockCheckbox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return false;
    }

    private void submit(boolean upload) {
        int scamCount = countSelections(SelectionState.SCAM);
        int legitCount = countSelections(SelectionState.LEGIT);
        if (scamCount == 0 && legitCount == 0) {
            MessageDispatcher.reply(ClientMessages.reviewSelectionRequired());
            return;
        }

        applyReviewVerdict(scamCount, legitCount);
        if (blacklistCheckbox != null && blacklistCheckbox.checked()) {
            addToBlacklist();
        }
        if (blockCheckbox != null && blockCheckbox.checked()) {
            sendBlockCommand();
        }

        // The v2 branch no longer has the old upload backend; keep the button behavior local for now.
        if (upload) {
            // Intentionally mirrored to the same local save path.
        }

        MessageDispatcher.reply(ClientMessages.reviewMessagesSaved(scamCount, legitCount));
        close();
    }

    private void applyReviewVerdict(int scamCount, int legitCount) {
        ReviewEntry entry = linkedReviewEntry().orElse(null);
        if (entry == null) {
            return;
        }

        ReviewVerdict verdict = scamCount > 0 ? ReviewVerdict.RISK : ReviewVerdict.SAFE;
        ReviewActionHandler.setVerdict(entry, verdict);
    }

    private void addToBlacklist() {
        if (!hasPlayerTarget()) {
            return;
        }

        ReviewEntry entry = linkedReviewEntry().orElse(null);
        if (entry != null) {
            ReviewActionHandler.addToBlacklist(entry);
            return;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.whitelist().remove(context.senderUuid(), context.senderName());
        runtime.blacklist().add(
            context.senderUuid(),
            context.senderName(),
            context.score(),
            blacklistReason(),
            BlacklistSource.PLAYER
        );
    }

    private void sendBlockCommand() {
        if (!hasPlayerTarget() || this.client == null || this.client.getNetworkHandler() == null) {
            return;
        }

        String senderName = context.senderName().trim();
        if (senderName.isBlank()) {
            return;
        }

        this.client.getNetworkHandler().sendChatCommand("block " + senderName);
    }

    private Optional<ReviewEntry> linkedReviewEntry() {
        if (context == null || !context.hasLinkedReviewEntry()) {
            return Optional.empty();
        }

        return ScamScreenerRuntime.getInstance().reviewStore().find(context.linkedReviewEntryId());
    }

    private boolean hasPlayerTarget() {
        return context != null && context.hasPlayerTarget();
    }

    private String headerLine() {
        if (context == null) {
            return "unknown | score 0";
        }

        return safePlayerName(context.displayPlayerName()) + " | score " + clampScore(context.score());
    }

    private String blacklistReason() {
        if (context == null || context.decidedByStage().isBlank()) {
            return "Alert action";
        }

        return "Alert action (" + context.decidedByStage() + ")";
    }

    private void renderList(DrawContext context, int mouseX, int mouseY) {
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0xA0101010);
        context.fill(listX, listY, listX + listWidth, listY + 1, 0xFF5A5A5A);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFF5A5A5A);
        context.fill(listX, listY, listX + 1, listY + listHeight, 0xFF5A5A5A);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFF5A5A5A);

        if (sourceRows.isEmpty()) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("No messages available"),
                this.width / 2,
                listY + (listHeight / 2) - 4,
                opaqueColor(0xAAAAAA)
            );
            return;
        }

        int maxOffset = Math.max(0, sourceRows.size() - maxVisibleRows);
        scrollOffsetRows = clamp(scrollOffsetRows, 0, maxOffset);
        int visibleRows = Math.min(maxVisibleRows, sourceRows.size() - scrollOffsetRows);
        int textX = listX + 8;

        for (int rowIndex = 0; rowIndex < visibleRows; rowIndex++) {
            int absoluteIndex = scrollOffsetRows + rowIndex;
            int rowY = listY + (rowIndex * LIST_ROW_HEIGHT);
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= rowY && mouseY < rowY + LIST_ROW_HEIGHT;

            int rowBackground = (rowIndex % 2 == 0) ? 0x402A2A2A : 0x40333333;
            if (hovered) {
                rowBackground = 0x60606060;
            }
            context.fill(listX + 1, rowY + 1, listX + listWidth - 1, rowY + LIST_ROW_HEIGHT - 1, rowBackground);

            SelectionState state = states.get(absoluteIndex);
            ReviewRow row = sourceRows.get(absoluteIndex);
            String prefix = state.marker() + " (" + clampScore(row.modScore()) + ") ";

            context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(prefix + compactMessage(row.message(), 160)),
                textX,
                rowY + 6,
                opaqueColor(state.color())
            );
        }

        if (sourceRows.size() > maxVisibleRows) {
            renderScrollBar(context);
        }
    }

    private void renderScrollBar(DrawContext context) {
        int trackLeft = listX + listWidth - 4;
        int trackTop = listY + 2;
        int trackBottom = listY + listHeight - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        context.fill(trackLeft, trackTop, trackLeft + 2, trackBottom, 0xFF3A3A3A);

        int totalRows = Math.max(1, sourceRows.size());
        int thumbHeight = Math.max(12, (int) (trackHeight * (maxVisibleRows / (double) totalRows)));
        int maxOffset = Math.max(1, totalRows - maxVisibleRows);
        int thumbRange = Math.max(1, trackHeight - thumbHeight);
        int thumbTop = trackTop + (int) Math.round((scrollOffsetRows / (double) maxOffset) * thumbRange);
        context.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
    }

    private void renderSelectedSummary(DrawContext context, int y) {
        int scamCount = countSelections(SelectionState.SCAM);
        int legitCount = countSelections(SelectionState.LEGIT);
        int ignoredCount = Math.max(0, sourceRows.size() - scamCount - legitCount);

        String prefix = "Selected: ";
        String scamPart = "scam " + scamCount;
        String legitPart = "legit " + legitCount;
        String ignoredPart = "ignored " + ignoredCount;
        String separator = " | ";

        int totalWidth = this.textRenderer.getWidth(prefix)
            + this.textRenderer.getWidth(scamPart)
            + this.textRenderer.getWidth(separator)
            + this.textRenderer.getWidth(legitPart)
            + this.textRenderer.getWidth(separator)
            + this.textRenderer.getWidth(ignoredPart);
        int x = (this.width - totalWidth) / 2;

        context.drawTextWithShadow(this.textRenderer, Text.literal(prefix), x, y, opaqueColor(0xCFCFCF));
        x += this.textRenderer.getWidth(prefix);
        context.drawTextWithShadow(this.textRenderer, Text.literal(scamPart), x, y, opaqueColor(SelectionState.SCAM.color()));
        x += this.textRenderer.getWidth(scamPart);
        context.drawTextWithShadow(this.textRenderer, Text.literal(separator), x, y, opaqueColor(0xCFCFCF));
        x += this.textRenderer.getWidth(separator);
        context.drawTextWithShadow(this.textRenderer, Text.literal(legitPart), x, y, opaqueColor(SelectionState.LEGIT.color()));
        x += this.textRenderer.getWidth(legitPart);
        context.drawTextWithShadow(this.textRenderer, Text.literal(separator), x, y, opaqueColor(0xCFCFCF));
        x += this.textRenderer.getWidth(separator);
        context.drawTextWithShadow(this.textRenderer, Text.literal(ignoredPart), x, y, opaqueColor(0xCFCFCF));
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        return mouseX >= listX
            && mouseX <= listX + listWidth
            && mouseY >= listY
            && mouseY <= listY + listHeight;
    }

    private int countSelections(SelectionState targetState) {
        int count = 0;
        for (SelectionState state : states) {
            if (state == targetState) {
                count++;
            }
        }

        return count;
    }

    private static List<ReviewRow> buildRows(AlertContextRegistry.AlertContext context) {
        if (context == null) {
            return List.of();
        }

        Set<String> uniqueMessages = new LinkedHashSet<>();
        for (String capturedMessage : context.capturedMessages()) {
            String normalizedMessage = normalizeReviewMessage(capturedMessage);
            if (!normalizedMessage.isBlank()) {
                uniqueMessages.add(normalizedMessage);
            }
        }

        if (uniqueMessages.isEmpty()) {
            String normalizedMessage = normalizeReviewMessage(context.rawMessage());
            if (!normalizedMessage.isBlank()) {
                uniqueMessages.add(normalizedMessage);
            }
        }

        List<ReviewRow> rows = new ArrayList<>();
        int currentLabel = currentLabel(context);
        int index = 0;
        for (String message : uniqueMessages) {
            rows.add(new ReviewRow(context.id() + "-row-" + index, message, currentLabel, context.score()));
            index++;
        }

        return rows;
    }

    private static int currentLabel(AlertContextRegistry.AlertContext context) {
        if (context == null) {
            return -1;
        }

        if (!context.hasLinkedReviewEntry()) {
            return -1;
        }

        Optional<ReviewEntry> entry = ScamScreenerRuntime.getInstance().reviewStore().find(context.linkedReviewEntryId());
        if (entry.isEmpty()) {
            return -1;
        }

        return switch (entry.get().getVerdict()) {
            case RISK -> 1;
            case SAFE -> 0;
            default -> -1;
        };
    }

    private static String normalizeReviewMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }

        return ChatLineClassifier.displayMessageOnly(rawMessage.replace('\n', ' ').replace('\r', ' ').trim());
    }

    private static String compactMessage(String rawMessage, int maxLength) {
        String normalizedMessage = normalizeReviewMessage(rawMessage);
        if (normalizedMessage.length() <= maxLength) {
            return normalizedMessage;
        }

        return normalizedMessage.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String safePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "unknown";
        }

        return playerName.trim();
    }

    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private enum SelectionState {
        IGNORE(-1, 0xFFFFFF, "I"),
        SCAM(1, 0xFFB3B3, "S"),
        LEGIT(0, 0xB3FFB3, "L");

        private final int label;
        private final int color;
        private final String marker;

        SelectionState(int label, int color, String marker) {
            this.label = label;
            this.color = color;
            this.marker = marker;
        }

        private SelectionState next() {
            return switch (this) {
                case IGNORE -> SCAM;
                case SCAM -> LEGIT;
                case LEGIT -> IGNORE;
            };
        }

        private int color() {
            return color;
        }

        private String marker() {
            return "[" + marker + "]";
        }

        private static SelectionState fromCurrentLabel(int currentLabel) {
            return switch (currentLabel) {
                case 1 -> SCAM;
                case 0 -> LEGIT;
                default -> IGNORE;
            };
        }

        private static SelectionState fromReviewRow(ReviewRow row) {
            if (row == null) {
                return IGNORE;
            }

            SelectionState byLabel = fromCurrentLabel(row.currentLabel());
            if (byLabel != IGNORE) {
                return byLabel;
            }

            return row.modScore() > 0 ? SCAM : IGNORE;
        }
    }

    private static final class SimpleCheckbox {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String label;
        private final boolean enabled;
        private boolean checked;

        private SimpleCheckbox(int x, int y, int width, int height, String label, boolean checked, boolean enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label == null ? "" : label;
            this.checked = checked;
            this.enabled = enabled;
        }

        private void render(
            DrawContext context,
            net.minecraft.client.font.TextRenderer textRenderer,
            int mouseX,
            int mouseY
        ) {
            boolean hovered = enabled
                && mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;

            int rowColor;
            if (!enabled) {
                rowColor = 0x20222222;
            } else {
                rowColor = hovered ? 0x40444444 : 0x302A2A2A;
            }

            context.fill(x, y, x + width, y + height, rowColor);
            context.fill(x, y, x + width, y + 1, 0xFF5A5A5A);
            context.fill(x, y + height - 1, x + width, y + height, 0xFF5A5A5A);
            context.fill(x, y, x + 1, y + height, 0xFF5A5A5A);
            context.fill(x + width - 1, y, x + width, y + height, 0xFF5A5A5A);

            int boxSize = 12;
            int boxX = x + 6;
            int boxY = y + ((height - boxSize) / 2);
            context.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF111111);
            context.fill(boxX, boxY, boxX + boxSize, boxY + 1, 0xFF9A9A9A);
            context.fill(boxX, boxY + boxSize - 1, boxX + boxSize, boxY + boxSize, 0xFF9A9A9A);
            context.fill(boxX, boxY, boxX + 1, boxY + boxSize, 0xFF9A9A9A);
            context.fill(boxX + boxSize - 1, boxY, boxX + boxSize, boxY + boxSize, 0xFF9A9A9A);

            if (checked) {
                context.drawTextWithShadow(textRenderer, Text.literal("x"), boxX + 3, boxY + 2, opaqueColor(0x90EE90));
            }

            int textColor;
            if (!enabled) {
                textColor = 0x888888;
            } else if (checked) {
                textColor = 0x90EE90;
            } else {
                textColor = 0xE0E0E0;
            }

            context.drawTextWithShadow(textRenderer, Text.literal(label), boxX + boxSize + 8, y + 6, opaqueColor(textColor));
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!enabled || button != 0) {
                return false;
            }
            if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
                return false;
            }

            checked = !checked;
            return true;
        }

        private boolean checked() {
            return checked;
        }
    }

    private record ReviewRow(String rowId, String message, int currentLabel, int modScore) {
    }
}
