package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.lists.BlacklistSource;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewSignalTag;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseMappings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AlertManageScreen extends BaseScreen {
    private static final int LIST_TOP = 48;
    private static final int LIST_ROW_HEIGHT = 20;
    private static final int CHECKBOX_HEIGHT = 20;
    private static final int CHECKBOX_GAP = 4;
    private static final int EDITOR_HEIGHT = 172;
    private static final int ADVANCED_VISIBLE_ROWS = 3;

    private final AlertContextRegistry.AlertContext context;
    private final List<ReviewCaseMessage> caseMessages = new ArrayList<>();
    private final List<TrainingCaseMappings.MappingOption> advancedRuleOptions = new ArrayList<>();
    private final List<SignalCheckbox> signalCheckboxes = new ArrayList<>();

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int maxVisibleRows;
    private int listScrollOffset;
    private int selectedMessageIndex;

    private int advancedX;
    private int advancedY;
    private int advancedWidth;
    private int advancedHeight;
    private int advancedScrollOffset;
    private boolean showAdvanced;

    private int blacklistX;
    private int blacklistY;
    private int blockY;
    private int checkboxWidth;
    private boolean blacklistChecked;
    private boolean blockChecked;

    private ButtonWidget excludeButton;
    private ButtonWidget contextButton;
    private ButtonWidget signalButton;
    private ButtonWidget advancedButton;
    private ButtonWidget addMessageButton;

    public AlertManageScreen(Screen parent, AlertContextRegistry.AlertContext context) {
        super(Text.literal("Manage Alert"), parent);
        this.context = context;
        caseMessages.addAll(copyCaseMessages(context));
        advancedRuleOptions.addAll(TrainingCaseMappings.optionsForStageResults(context == null ? List.of() : context.stageResults()));
        selectedMessageIndex = findInitialSelection(caseMessages);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(640, Math.max(320, this.width - 30));
        int contentX = centeredX(contentWidth);
        int actionY = this.height - FOOTER_MARGIN - 24;
        int bottomCheckboxHeight = hasPlayerTarget() ? (CHECKBOX_HEIGHT * 2) + CHECKBOX_GAP : 0;
        int checkboxY = actionY - bottomCheckboxHeight;
        int editorTop = checkboxY - 8 - EDITOR_HEIGHT;

        listX = contentX;
        listY = LIST_TOP;
        listWidth = contentWidth;
        listHeight = Math.max(80, editorTop - listY - 8);
        maxVisibleRows = Math.max(1, listHeight / LIST_ROW_HEIGHT);

        int quarter = splitWidth(contentWidth, 4, DEFAULT_SPLIT_GAP);
        excludeButton = addDrawableChild(ButtonWidget.builder(Text.literal("Exclude"), button -> setRole(ReviewCaseRole.EXCLUDED))
            .dimensions(contentX, editorTop, quarter, DEFAULT_BUTTON_HEIGHT).build());
        contextButton = addDrawableChild(ButtonWidget.builder(Text.literal("Context"), button -> setRole(ReviewCaseRole.CONTEXT))
            .dimensions(columnX(contentX, quarter, DEFAULT_SPLIT_GAP, 1), editorTop, quarter, DEFAULT_BUTTON_HEIGHT).build());
        signalButton = addDrawableChild(ButtonWidget.builder(Text.literal("Signal"), button -> setRole(ReviewCaseRole.SIGNAL))
            .dimensions(columnX(contentX, quarter, DEFAULT_SPLIT_GAP, 2), editorTop, quarter, DEFAULT_BUTTON_HEIGHT).build());
        advancedButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleAdvanced())
            .dimensions(columnX(contentX, quarter, DEFAULT_SPLIT_GAP, 3), editorTop, quarter, DEFAULT_BUTTON_HEIGHT).build());

        int inputY = editorTop + DEFAULT_BUTTON_HEIGHT + 4;
        addMessageButton = addDrawableChild(ButtonWidget.builder(Text.literal("Add Case Message"), button -> openMessagePicker())
            .dimensions(contentX, inputY, contentWidth, DEFAULT_BUTTON_HEIGHT).build());

        int checkboxSectionY = inputY + DEFAULT_BUTTON_HEIGHT + 12;
        buildSignalCheckboxes(contentX, checkboxSectionY, contentWidth);
        advancedX = contentX;
        advancedY = checkboxSectionY + ((CHECKBOX_HEIGHT + CHECKBOX_GAP) * 2);
        advancedWidth = contentWidth;
        advancedHeight = Math.max(20, checkboxY - 8 - advancedY);

        checkboxWidth = contentWidth;
        blacklistX = contentX;
        blacklistY = checkboxY;
        blockY = checkboxY + CHECKBOX_HEIGHT + CHECKBOX_GAP;

        int actionWidth = splitWidth(contentWidth, 4, DEFAULT_SPLIT_GAP);
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
            .dimensions(contentX, actionY, actionWidth, DEFAULT_BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Dismiss"), button -> submit(ReviewVerdict.IGNORED))
            .dimensions(columnX(contentX, actionWidth, DEFAULT_SPLIT_GAP, 1), actionY, actionWidth, DEFAULT_BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save Safe"), button -> submit(ReviewVerdict.SAFE))
            .dimensions(columnX(contentX, actionWidth, DEFAULT_SPLIT_GAP, 2), actionY, actionWidth, DEFAULT_BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save Risk"), button -> submit(ReviewVerdict.RISK))
            .dimensions(columnX(contentX, actionWidth, DEFAULT_SPLIT_GAP, 3), actionY, actionWidth, DEFAULT_BUTTON_HEIGHT).build());
        refreshButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(headerLine()), this.width / 2, TITLE_Y + 12, opaqueColor(0xCCCCCC));
        drawLine(context, listX, TITLE_Y + 24, summaryLine());
        renderList(context, mouseX, mouseY);
        renderEditor(context, mouseX, mouseY);
        if (hasPlayerTarget()) {
            renderBottomCheckbox(context, mouseX, mouseY, blacklistY, "Add player to ScamScreener blacklist", blacklistChecked, true);
            renderBottomCheckbox(context, mouseX, mouseY, blockY, "Add player to Hypixel /block list", blockChecked, true);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean doubleClick) {
        if (event != null && event.button() == 0 && handleMouseClick(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, listX, listY, listWidth, listHeight) && caseMessages.size() > maxVisibleRows) {
            listScrollOffset = clamp(listScrollOffset + (verticalAmount > 0 ? -1 : 1), 0, Math.max(0, caseMessages.size() - maxVisibleRows));
            return true;
        }
        if (showAdvanced && inside(mouseX, mouseY, advancedX, advancedY, advancedWidth, advancedHeight) && advancedRuleOptions.size() > ADVANCED_VISIBLE_ROWS) {
            advancedScrollOffset = clamp(advancedScrollOffset + (verticalAmount > 0 ? -1 : 1), 0, Math.max(0, advancedRuleOptions.size() - ADVANCED_VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean handleMouseClick(double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, listX, listY, listWidth, listHeight) && !caseMessages.isEmpty()) {
            int row = ((int) mouseY - listY) / LIST_ROW_HEIGHT;
            int absolute = listScrollOffset + row;
            if (absolute >= 0 && absolute < caseMessages.size()) {
                selectedMessageIndex = absolute;
                refreshButtons();
                return true;
            }
        }
        ReviewCaseMessage selected = selectedMessage();
        if (selected != null && selected.isSignalMessage()) {
            for (SignalCheckbox checkbox : signalCheckboxes) {
                if (checkbox.hit(mouseX, mouseY)) {
                    selected.toggleSignalTag(checkbox.tagId);
                    return true;
                }
            }
            if (showAdvanced && inside(mouseX, mouseY, advancedX, advancedY, advancedWidth, advancedHeight)) {
                int row = ((int) mouseY - advancedY) / CHECKBOX_HEIGHT;
                int absolute = advancedScrollOffset + row;
                if (absolute >= 0 && absolute < advancedRuleOptions.size()) {
                    selected.toggleAdvancedRuleSelection(advancedRuleOptions.get(absolute).id());
                    return true;
                }
            }
        }
        if (hasPlayerTarget() && inside(mouseX, mouseY, blacklistX, blacklistY, checkboxWidth, CHECKBOX_HEIGHT)) {
            blacklistChecked = !blacklistChecked;
            return true;
        }
        if (hasPlayerTarget() && inside(mouseX, mouseY, blacklistX, blockY, checkboxWidth, CHECKBOX_HEIGHT)) {
            blockChecked = !blockChecked;
            return true;
        }
        return false;
    }

    private void buildSignalCheckboxes(int x, int y, int width) {
        signalCheckboxes.clear();
        int columnWidth = splitWidth(width, 4, DEFAULT_SPLIT_GAP);
        int column = 0;
        int row = 0;
        for (ReviewSignalTag tag : ReviewSignalTag.values()) {
            signalCheckboxes.add(new SignalCheckbox(
                columnX(x, columnWidth, DEFAULT_SPLIT_GAP, column),
                y + (row * (CHECKBOX_HEIGHT + CHECKBOX_GAP)),
                columnWidth,
                tag.id(),
                tag.label()
            ));
            column++;
            if (column >= 4) {
                column = 0;
                row++;
            }
        }
    }

    private void refreshButtons() {
        boolean hasSelection = selectedMessage() != null;
        if (excludeButton != null) {
            excludeButton.active = hasSelection;
        }
        if (contextButton != null) {
            contextButton.active = hasSelection;
        }
        if (signalButton != null) {
            signalButton.active = hasSelection;
        }
        if (advancedButton != null) {
            advancedButton.active = hasSelection;
            advancedButton.setMessage(toggleText("Advanced: ", showAdvanced));
        }
        if (addMessageButton != null) {
            addMessageButton.active = this.client != null;
        }
    }

    private void toggleAdvanced() {
        showAdvanced = !showAdvanced;
        if (!showAdvanced) {
            advancedScrollOffset = 0;
        }
        refreshButtons();
    }

    private void setRole(ReviewCaseRole role) {
        ReviewCaseMessage selected = selectedMessage();
        if (selected == null) {
            return;
        }
        selected.setCaseRole(role);
        if (role != ReviewCaseRole.SIGNAL) {
            selected.clearSignalAnnotations();
        }
    }

    private void openMessagePicker() {
        if (this.client == null) {
            return;
        }

        this.client.setScreen(new CaseMessagePickerScreen(this, defaultPickerFilter()));
    }

    private void submit(ReviewVerdict verdict) {
        int included = includedCount();
        int signals = signalCount();
        if (verdict != ReviewVerdict.IGNORED && included == 0) {
            MessageDispatcher.reply(ClientMessages.caseReviewNeedsCaseSelection());
            return;
        }
        if (verdict == ReviewVerdict.RISK && signals == 0) {
            MessageDispatcher.reply(ClientMessages.caseReviewNeedsSignalSelection());
            return;
        }
        ReviewEntry entry = ensureReviewEntry();
        if (entry == null) {
            MessageDispatcher.reply(ClientMessages.alertContextMissing());
            return;
        }
        entry.replaceCaseMessages(copyCaseMessages(caseMessages));
        ScamScreenerRuntime.getInstance().reviewStore().setVerdict(entry.getId(), verdict);
        if (blacklistChecked) {
            applyBlacklist();
        }
        if (blockChecked) {
            sendBlockCommand();
        }
        MessageDispatcher.reply(ClientMessages.caseReviewSaved(included, signals, verdict));
        close();
    }

    private void renderList(DrawContext context, int mouseX, int mouseY) {
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0xA0101010);
        drawBoxBorder(context, listX, listY, listWidth, listHeight);
        if (caseMessages.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("No case messages available"), this.width / 2, listY + (listHeight / 2) - 4, opaqueColor(0xAAAAAA));
            return;
        }

        int maxOffset = Math.max(0, caseMessages.size() - maxVisibleRows);
        listScrollOffset = clamp(listScrollOffset, 0, maxOffset);
        int visibleRows = Math.min(maxVisibleRows, caseMessages.size() - listScrollOffset);
        for (int index = 0; index < visibleRows; index++) {
            int absolute = listScrollOffset + index;
            int rowY = listY + (index * LIST_ROW_HEIGHT);
            boolean hovered = inside(mouseX, mouseY, listX, rowY, listWidth, LIST_ROW_HEIGHT);
            boolean selected = absolute == selectedMessageIndex;
            int fill = selected ? 0x70707030 : (hovered ? 0x60505050 : (index % 2 == 0 ? 0x402A2A2A : 0x40333333));
            context.fill(listX + 1, rowY + 1, listX + listWidth - 1, rowY + LIST_ROW_HEIGHT - 1, fill);

            ReviewCaseMessage message = caseMessages.get(absolute);
            String line = message.getCaseRole().marker() + (message.isTriggerMessage() ? " [TRIGGER] " : " ") + compact(message.getCleanText(), 180);
            context.drawTextWithShadow(this.textRenderer, Text.literal(line), listX + 8, rowY + 6, opaqueColor(message.getCaseRole().color()));
        }
        if (caseMessages.size() > maxVisibleRows) {
            renderScrollBar(context, listX + listWidth - 4, listY + 2, listHeight - 4, listScrollOffset, caseMessages.size(), maxVisibleRows);
        }
    }

    private void renderEditor(DrawContext context, int mouseX, int mouseY) {
        ReviewCaseMessage selected = selectedMessage();
        int helperY = signalCheckboxes.isEmpty() ? advancedY - 12 : signalCheckboxes.get(0).y - 12;
        String helperText;
        if (caseMessages.isEmpty()) {
            helperText = "Add case messages, then mark which ones are context and which ones are signals.";
        } else if (selected == null) {
            helperText = "Select a message, then define whether it belongs to the case and which signals it carries.";
        } else {
            helperText = "Selected: " + compact(selected.getCleanText(), 96);
        }
        drawLine(context, listX, helperY, helperText);

        for (SignalCheckbox checkbox : signalCheckboxes) {
            renderCheckbox(context, mouseX, mouseY, checkbox.x, checkbox.y, checkbox.width, checkbox.label,
                selected != null && selected.hasSignalTag(checkbox.tagId), selected != null && selected.isSignalMessage());
        }

        context.fill(advancedX, advancedY, advancedX + advancedWidth, advancedY + advancedHeight, 0x20202020);
        drawBoxBorder(context, advancedX, advancedY, advancedWidth, advancedHeight);
        if (!showAdvanced) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Advanced rule mapping is hidden."), advancedX + 6, advancedY + 6, opaqueColor(0xA0A0A0));
            return;
        }
        if (selected == null || !selected.isSignalMessage()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Mark one message as Signal to map advanced rules."), advancedX + 6, advancedY + 6, opaqueColor(0xA0A0A0));
            return;
        }
        if (advancedRuleOptions.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("No pipeline rule hints were captured for this alert."), advancedX + 6, advancedY + 6, opaqueColor(0xA0A0A0));
            return;
        }

        int maxOffset = Math.max(0, advancedRuleOptions.size() - ADVANCED_VISIBLE_ROWS);
        advancedScrollOffset = clamp(advancedScrollOffset, 0, maxOffset);
        int visibleRows = Math.min(ADVANCED_VISIBLE_ROWS, advancedRuleOptions.size() - advancedScrollOffset);
        for (int index = 0; index < visibleRows; index++) {
            int absolute = advancedScrollOffset + index;
            int rowY = advancedY + (index * CHECKBOX_HEIGHT);
            TrainingCaseMappings.MappingOption option = advancedRuleOptions.get(absolute);
            renderCheckbox(
                context,
                mouseX,
                mouseY,
                advancedX + 2,
                rowY,
                advancedWidth - 4,
                compact(option.label(), 88),
                selected.hasAdvancedRuleSelection(option.id()),
                true
            );
        }
        if (advancedRuleOptions.size() > ADVANCED_VISIBLE_ROWS) {
            renderScrollBar(context, advancedX + advancedWidth - 4, advancedY + 2, advancedHeight - 4, advancedScrollOffset, advancedRuleOptions.size(), ADVANCED_VISIBLE_ROWS);
        }
    }

    private void renderBottomCheckbox(DrawContext context, int mouseX, int mouseY, int y, String label, boolean checked, boolean enabled) {
        renderCheckbox(context, mouseX, mouseY, blacklistX, y, checkboxWidth, label, checked, enabled);
    }

    private void renderCheckbox(DrawContext context, int mouseX, int mouseY, int x, int y, int width, String label, boolean checked, boolean enabled) {
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, width, CHECKBOX_HEIGHT);
        int fill = !enabled ? 0x20222222 : (hovered ? 0x40444444 : 0x302A2A2A);
        context.fill(x, y, x + width, y + CHECKBOX_HEIGHT, fill);
        drawBoxBorder(context, x, y, width, CHECKBOX_HEIGHT);

        int boxX = x + 6;
        int boxY = y + 4;
        context.fill(boxX, boxY, boxX + 12, boxY + 12, 0xFF111111);
        context.fill(boxX, boxY, boxX + 12, boxY + 1, 0xFF9A9A9A);
        context.fill(boxX, boxY + 11, boxX + 12, boxY + 12, 0xFF9A9A9A);
        context.fill(boxX, boxY, boxX + 1, boxY + 12, 0xFF9A9A9A);
        context.fill(boxX + 11, boxY, boxX + 12, boxY + 12, 0xFF9A9A9A);
        if (checked) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("x"), boxX + 3, boxY + 2, opaqueColor(0x90EE90));
        }

        int color = !enabled ? 0x888888 : (checked ? 0x90EE90 : 0xE0E0E0);
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), boxX + 20, y + 6, opaqueColor(color));
    }

    private void drawBoxBorder(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + 1, 0xFF5A5A5A);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF5A5A5A);
        context.fill(x, y, x + 1, y + height, 0xFF5A5A5A);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF5A5A5A);
    }

    private static void renderScrollBar(DrawContext context, int x, int y, int height, int offset, int total, int visible) {
        context.fill(x, y, x + 2, y + height, 0xFF3A3A3A);
        int thumbHeight = Math.max(12, (int) (height * (visible / (double) Math.max(1, total))));
        int thumbRange = Math.max(1, height - thumbHeight);
        int maxOffset = Math.max(1, total - visible);
        int thumbTop = y + (int) Math.round((offset / (double) maxOffset) * thumbRange);
        context.fill(x, thumbTop, x + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
    }

    private ReviewEntry ensureReviewEntry() {
        Optional<ReviewEntry> linked = linkedReviewEntry();
        if (linked.isPresent()) {
            return linked.get();
        }
        if (context == null) {
            String summaryMessage = firstSignalOrTriggerText();
            return ScamScreenerRuntime.getInstance().reviewStore().createManual(
                null,
                "",
                summaryMessage,
                0,
                "",
                System.currentTimeMillis(),
                List.of(),
                List.of(),
                copyCaseMessages(caseMessages)
            );
        }
        return ScamScreenerRuntime.getInstance().reviewStore().createManual(
            context.senderUuid(),
            context.senderName(),
            context.rawMessage(),
            context.score(),
            context.decidedByStage(),
            context.capturedAtMs(),
            context.reasons(),
            context.stageResults(),
            copyCaseMessages(caseMessages)
        );
    }

    private Optional<ReviewEntry> linkedReviewEntry() {
        return context == null || !context.hasLinkedReviewEntry()
            ? Optional.empty()
            : ScamScreenerRuntime.getInstance().reviewStore().find(context.linkedReviewEntryId());
    }

    private void applyBlacklist() {
        if (!hasPlayerTarget() || context == null) {
            return;
        }
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.whitelist().remove(context.senderUuid(), context.senderName());
        runtime.blacklist().add(context.senderUuid(), context.senderName(), context.score(), blacklistReason(), BlacklistSource.PLAYER);
    }

    private void sendBlockCommand() {
        if (!hasPlayerTarget() || context == null || this.client == null || this.client.getNetworkHandler() == null) {
            return;
        }
        String senderName = context.senderName().trim();
        if (!senderName.isBlank()) {
            this.client.getNetworkHandler().sendChatCommand("block " + senderName);
        }
    }

    private ReviewCaseMessage selectedMessage() {
        return selectedMessageIndex < 0 || selectedMessageIndex >= caseMessages.size() ? null : caseMessages.get(selectedMessageIndex);
    }

    private int includedCount() {
        int count = 0;
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && message.isIncludedInCase()) {
                count++;
            }
        }
        return count;
    }

    private int signalCount() {
        int count = 0;
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && message.isSignalMessage()) {
                count++;
            }
        }
        return count;
    }

    private String headerLine() {
        if (context == null) {
            return "Manual Case | score 0";
        }
        return safePlayer(context.displayPlayerName()) + " | score " + Math.max(0, Math.min(100, context.score()));
    }

    private String summaryLine() {
        String selected = selectedMessageIndex >= 0 && selectedMessageIndex < caseMessages.size() ? "Selected #" + selectedMessageIndex : "No selection";
        return selected + " | Included " + includedCount() + " | Signals " + signalCount() + " | Tagged " + totalTagCount();
    }

    private int totalTagCount() {
        int count = 0;
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null) {
                count += message.signalCount();
            }
        }
        return count;
    }

    private boolean hasPlayerTarget() {
        return context != null && context.hasPlayerTarget();
    }

    void appendCachedMessages(List<RecentChatCache.CachedChatMessage> cachedMessages) {
        if (cachedMessages == null || cachedMessages.isEmpty()) {
            return;
        }

        List<RecentChatCache.CachedChatMessage> additions = new ArrayList<>();
        for (RecentChatCache.CachedChatMessage cachedMessage : cachedMessages) {
            if (cachedMessage != null && !cachedMessage.cleanText().isBlank()) {
                additions.add(cachedMessage);
            }
        }
        if (additions.isEmpty()) {
            return;
        }

        additions.sort(Comparator.comparingLong(RecentChatCache.CachedChatMessage::capturedAtMs));
        boolean alreadyHasTrigger = hasTriggerMessage();
        int appended = 0;
        for (int index = 0; index < additions.size(); index++) {
            RecentChatCache.CachedChatMessage cachedMessage = additions.get(index);
            boolean isTriggerMessage = !alreadyHasTrigger && index == additions.size() - 1;
            caseMessages.add(new ReviewCaseMessage(
                caseMessages.size(),
                cachedMessage.speakerRoleId(),
                cachedMessage.messageSourceTypeId(),
                cachedMessage.cleanText(),
                isTriggerMessage,
                isTriggerMessage ? ReviewCaseRole.SIGNAL : ReviewCaseRole.CONTEXT,
                List.of(),
                List.of()
            ));
            appended++;
        }

        if (appended > 0) {
            selectedMessageIndex = caseMessages.size() - 1;
            int maxOffset = Math.max(0, caseMessages.size() - maxVisibleRows);
            listScrollOffset = maxOffset;
            refreshButtons();
        }
    }

    private String defaultPickerFilter() {
        if (context == null || context.senderName() == null || context.senderName().isBlank()) {
            return "";
        }

        return context.senderName().trim();
    }

    private boolean hasTriggerMessage() {
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && message.isTriggerMessage()) {
                return true;
            }
        }

        return false;
    }

    private String firstSignalOrTriggerText() {
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && message.isSignalMessage() && !message.getCleanText().isBlank()) {
                return message.getCleanText();
            }
        }
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && message.isTriggerMessage() && !message.getCleanText().isBlank()) {
                return message.getCleanText();
            }
        }
        for (ReviewCaseMessage message : caseMessages) {
            if (message != null && !message.getCleanText().isBlank()) {
                return message.getCleanText();
            }
        }

        return "";
    }

    private String blacklistReason() {
        return context == null || context.decidedByStage().isBlank() ? "Case review action" : "Case review action (" + context.decidedByStage() + ")";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String safePlayer(String playerName) {
        return playerName == null || playerName.isBlank() ? "unknown" : playerName.trim();
    }

    private static List<ReviewCaseMessage> copyCaseMessages(AlertContextRegistry.AlertContext context) {
        if (context == null) {
            return List.of();
        }
        if (context.hasLinkedReviewEntry()) {
            Optional<ReviewEntry> linkedEntry = ScamScreenerRuntime.getInstance().reviewStore().find(context.linkedReviewEntryId());
            if (linkedEntry.isPresent() && linkedEntry.get().hasCaseMessages()) {
                return copyCaseMessages(linkedEntry.get().getCaseMessages());
            }
        }
        return copyCaseMessages(context.caseMessages().isEmpty()
            ? ReviewCaseMessage.fromCapturedMessages(context.capturedMessages(), context.rawMessage(), null)
            : context.caseMessages());
    }

    private static List<ReviewCaseMessage> copyCaseMessages(List<ReviewCaseMessage> source) {
        List<ReviewCaseMessage> copies = new ArrayList<>();
        if (source != null) {
            for (ReviewCaseMessage message : source) {
                if (message != null && !message.getCleanText().isBlank()) {
                    copies.add(new ReviewCaseMessage(
                        message.getMessageIndex(),
                        message.getSpeakerRole(),
                        message.getMessageSourceType(),
                        message.getCleanText(),
                        message.isTriggerMessage(),
                        message.getCaseRole(),
                        message.getSignalTagIds(),
                        message.getAdvancedRuleSelections()
                    ));
                }
            }
        }
        return List.copyOf(copies);
    }

    private static int findInitialSelection(List<ReviewCaseMessage> messages) {
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index) != null && messages.get(index).isTriggerMessage()) {
                return index;
            }
        }
        return messages.isEmpty() ? -1 : 0;
    }

    private static final class SignalCheckbox {
        private final int x;
        private final int y;
        private final int width;
        private final String tagId;
        private final String label;

        private SignalCheckbox(int x, int y, int width, String tagId, String label) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.tagId = tagId;
            this.label = label;
        }

        private boolean hit(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, width, CHECKBOX_HEIGHT);
        }
    }
}
