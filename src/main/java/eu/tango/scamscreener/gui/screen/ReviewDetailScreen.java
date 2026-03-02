package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.gui.base.BaseListScreen;
import eu.tango.scamscreener.gui.widget.SelectableListWidget;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewActionHandler;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only detail view for one review entry.
 */
public final class ReviewDetailScreen extends BaseListScreen {
    private static final int DETAIL_ROW_HEIGHT = 16;
    private static final int WRAP_WIDTH = 92;
    private static final int ACTION_COLUMNS = 3;
    private static final int ACTION_AREA_HEIGHT = (DEFAULT_BUTTON_HEIGHT * 2) + 18;

    private final ReviewEntry entry;
    private final List<DetailLine> lines = new ArrayList<>();

    private SelectableListWidget<DetailLine> listWidget;
    private ButtonWidget markRiskButton;
    private ButtonWidget markSafeButton;
    private ButtonWidget ignoreButton;
    private ButtonWidget blacklistButton;
    private ButtonWidget whitelistButton;
    private ButtonWidget removeButton;

    /**
     * Creates a detail view for one captured review entry.
     *
     * @param parent the parent screen to return to
     * @param entry the review entry to inspect
     */
    public ReviewDetailScreen(Screen parent, ReviewEntry entry) {
        super(Text.literal("Review Details"), parent);
        this.entry = entry;
    }

    /**
     * Builds the read-only detail view.
     */
    @Override
    protected void init() {
        int contentWidth = Math.min(640, Math.max(360, this.width - 40));
        int contentX = centeredX(contentWidth);
        int listY = CONTENT_TOP + 24;
        int listHeight = Math.max(140, footerY() - listY - ACTION_AREA_HEIGHT - 10);

        listWidget = new SelectableListWidget<>(
            contentX,
            listY,
            contentWidth,
            listHeight,
            DETAIL_ROW_HEIGHT,
            this::renderDetailLine
        );

        int buttonWidth = splitWidth(contentWidth, ACTION_COLUMNS, DEFAULT_SPLIT_GAP);
        int buttonY = listY + listHeight + 10;

        markRiskButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Mark Risk"), button -> setVerdict(ReviewVerdict.RISK))
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        markSafeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Mark Safe"), button -> setVerdict(ReviewVerdict.SAFE))
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        ignoreButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Ignore"), button -> setVerdict(ReviewVerdict.IGNORED))
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        buttonY += DEFAULT_BUTTON_HEIGHT + 4;
        blacklistButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("To Blacklist"), button -> addToBlacklist())
                .dimensions(contentX, buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        whitelistButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("To Whitelist"), button -> addToWhitelist())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 1), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        removeButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeEntry())
                .dimensions(columnX(contentX, buttonWidth, DEFAULT_SPLIT_GAP, 2), buttonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        rebuildLines();
        updateActionState();
        addCloseButton(contentWidth);
    }

    /**
     * Draws the detail header and scrollable content.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(Math.min(640, Math.max(360, this.width - 40)));
        drawSectionTitle(context, left, CONTENT_TOP, "Captured Review Context");
        drawLine(context, left, CONTENT_TOP + 12, summaryLine());

        if (listWidget != null) {
            listWidget.render(context, this.textRenderer, mouseX, mouseY);
        }
    }

    @Override
    protected boolean handleListClick(double mouseX, double mouseY, int button) {
        return listWidget != null && listWidget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean handleListScroll(double mouseX, double mouseY, double verticalAmount) {
        return listWidget != null && listWidget.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    private void setVerdict(ReviewVerdict verdict) {
        if (!ReviewActionHandler.setVerdict(entry, verdict)) {
            return;
        }

        rebuildLines();
        updateActionState();
    }

    private void addToBlacklist() {
        if (!ReviewActionHandler.addToBlacklist(entry)) {
            return;
        }

        rebuildLines();
        updateActionState();
    }

    private void addToWhitelist() {
        if (!ReviewActionHandler.addToWhitelist(entry)) {
            return;
        }

        rebuildLines();
        updateActionState();
    }

    private void removeEntry() {
        if (!ReviewActionHandler.remove(entry)) {
            return;
        }

        close();
    }

    private void rebuildLines() {
        lines.clear();
        if (entry == null) {
            lines.add(new DetailLine("No review entry selected.", 0xD0D0D0));
        } else {
            addKeyValue("Entry ID", entry.getId(), 0xFFFFFF);
            addKeyValue("Sender", displaySender(), 0xFFFFFF);
            addKeyValue("Verdict", entry.getVerdict().name(), color(entry.getVerdict()));
            addKeyValue("Score", Integer.toString(entry.getScore()), 0xFFFFFF);
            addKeyValue("Decided By", emptyFallback(entry.getDecidedByStage(), "-"), 0xFFFFFF);
            addKeyValue("Captured", formatCapturedAt(entry.getCapturedAtMs()), 0xAAAAAA);

            addSpacer();
            addSection("Message", 0x55FF55);
            addWrappedLines(entry.getMessage(), 0xFFFFFF, "  ");

            addSpacer();
            addSection("Reasons", 0x55FF55);
            if (!entry.hasReasons()) {
                lines.add(new DetailLine("  No explicit reasons captured.", 0xAAAAAA));
            } else {
                for (String reason : entry.getReasons()) {
                    addWrappedLines("- " + emptyFallback(reason, "(blank)"), 0xFFD27F, "  ");
                }
            }

            addSpacer();
            addSection("Stage Trace", 0x55FF55);
            if (!entry.hasStageResults()) {
                lines.add(new DetailLine("  No stage trace captured.", 0xAAAAAA));
            } else {
                for (StageResult stageResult : entry.getStageResults()) {
                    if (stageResult == null) {
                        continue;
                    }

                    lines.add(new DetailLine(stageSummary(stageResult), stageColor(stageResult)));
                    if (stageResult.hasReason()) {
                        addWrappedLines("reason: " + stageResult.getReason(), 0xC8C8C8, "    ");
                    }
                }
            }
        }

        if (listWidget != null) {
            listWidget.setRows(lines);
            listWidget.clearSelection();
        }
    }

    private void updateActionState() {
        boolean hasEntry = entry != null;
        boolean hasTarget = ReviewActionHandler.hasPlayerTarget(entry);

        if (markRiskButton != null) {
            markRiskButton.active = hasEntry;
        }
        if (markSafeButton != null) {
            markSafeButton.active = hasEntry;
        }
        if (ignoreButton != null) {
            ignoreButton.active = hasEntry;
        }
        if (blacklistButton != null) {
            blacklistButton.active = hasTarget;
        }
        if (whitelistButton != null) {
            whitelistButton.active = hasTarget;
        }
        if (removeButton != null) {
            removeButton.active = hasEntry;
        }
    }

    private void renderDetailLine(
        DrawContext context,
        net.minecraft.client.font.TextRenderer textRenderer,
        DetailLine line,
        int x,
        int y,
        int width,
        int height,
        boolean hovered,
        boolean selected
    ) {
        if (line == null) {
            return;
        }

        context.drawTextWithShadow(textRenderer, Text.literal(line.text()), x, y, line.color());
    }

    private void addSection(String title, int color) {
        lines.add(new DetailLine(title, color));
    }

    private void addKeyValue(String label, String value, int color) {
        lines.add(new DetailLine(label + ": " + emptyFallback(value, "-"), color));
    }

    private void addWrappedLines(String text, int color, String indent) {
        String normalizedText = normalizeText(text);
        if (normalizedText.isEmpty()) {
            lines.add(new DetailLine(indent + "-", color));
            return;
        }

        for (String line : wrap(normalizedText, WRAP_WIDTH)) {
            lines.add(new DetailLine(indent + line, color));
        }
    }

    private void addSpacer() {
        lines.add(new DetailLine("", 0xFFFFFF));
    }

    private String summaryLine() {
        if (entry == null) {
            return "Missing review entry";
        }

        return displaySender() + " | " + entry.getScore() + " score | " + entry.getStageResults().size() + " stages";
    }

    private String displaySender() {
        if (entry == null || entry.getSenderName().isBlank()) {
            return "Unknown Sender";
        }

        return entry.getSenderName();
    }

    private static String stageSummary(StageResult stageResult) {
        String decision = stageResult.getDecision() == null ? "?" : stageResult.getDecision().name();
        return "  " + stageResult.getStageName() + " | " + decision + " | score " + stageResult.getScoreDelta();
    }

    private static int stageColor(StageResult stageResult) {
        if (stageResult == null || stageResult.getDecision() == null) {
            return 0xD0D0D0;
        }

        return switch (stageResult.getDecision()) {
            case PASS -> stageResult.getScoreDelta() > 0 ? 0xFFCC66 : 0xD0D0D0;
            case MUTE, WHITELIST, ALLOW -> 0x99FF99;
            case BLACKLIST, BLOCK -> 0xFFB366;
        };
    }

    private static int color(eu.tango.scamscreener.review.ReviewVerdict verdict) {
        if (verdict == null) {
            return 0xD0D0D0;
        }

        return switch (verdict) {
            case PENDING -> 0xD0D0D0;
            case RISK -> 0xFFB366;
            case SAFE -> 0x99FF99;
            case IGNORED -> 0xB8B8B8;
        };
    }

    private static String formatCapturedAt(long capturedAtMs) {
        if (capturedAtMs <= 0L) {
            return "-";
        }

        return Instant.ofEpochMilli(capturedAtMs).toString();
    }

    private static List<String> wrap(String text, int maxLength) {
        List<String> wrapped = new ArrayList<>();
        String remaining = normalizeText(text);
        int safeMaxLength = Math.max(16, maxLength);
        while (!remaining.isEmpty()) {
            if (remaining.length() <= safeMaxLength) {
                wrapped.add(remaining);
                break;
            }

            int splitIndex = remaining.lastIndexOf(' ', safeMaxLength);
            if (splitIndex <= 0) {
                splitIndex = safeMaxLength;
            }

            wrapped.add(remaining.substring(0, splitIndex).trim());
            remaining = remaining.substring(splitIndex).trim();
        }

        return wrapped;
    }

    private static String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String emptyFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private record DetailLine(String text, int color) {
        private DetailLine {
            text = text == null ? "" : text;
        }
    }
}
