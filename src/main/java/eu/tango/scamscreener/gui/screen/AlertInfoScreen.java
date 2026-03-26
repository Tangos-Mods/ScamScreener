package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.chat.ChatLineClassifier;
import eu.tango.scamscreener.gui.base.BaseScreen;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Scrollable v1-style alert detail screen backed by the new v2 alert context.
 */
public final class AlertInfoScreen extends BaseScreen {
    private static final int BOX_PADDING = 6;
    private static final int BUTTON_GAP = 4;

    private final AlertContextRegistry.AlertContext context;
    private final List<InfoLine> wrappedLines = new ArrayList<>();

    private int textAreaX;
    private int textAreaY;
    private int textAreaWidth;
    private int textAreaHeight;
    private int scrollOffset;
    private int maxScroll;

    /**
     * Creates the classic detail screen for one alert context.
     *
     * @param parent the parent screen to return to
     * @param context the alert context to inspect
     */
    public AlertInfoScreen(Screen parent, AlertContextRegistry.AlertContext context) {
        super(Component.literal("Alert Rule Details"), parent);
        this.context = context;
    }

    @Override
    protected void init() {
        int buttonWidth = defaultButtonWidth();
        int closeButtonY = this.height - FOOTER_MARGIN - DEFAULT_BUTTON_HEIGHT;
        int reviewButtonY = closeButtonY - DEFAULT_BUTTON_HEIGHT - BUTTON_GAP;
        textAreaWidth = buttonWidth;
        textAreaX = centeredX(buttonWidth);
        textAreaY = CONTENT_TOP;
        textAreaHeight = Math.max(80, reviewButtonY - textAreaY - 10);

        addRenderableWidget(
            Button.builder(Component.literal("Review Case"), button -> openReviewCase())
                .bounds(textAreaX, reviewButtonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        ).active = context != null;
        addRenderableWidget(
            Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(textAreaX, closeButtonY, buttonWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        rebuildWrappedLines();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        renderTextBox(context);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInsideTextArea(mouseX, mouseY) && maxScroll > 0) {
            scrollBy(verticalAmount > 0 ? -3 : 3);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event == null) {
            return false;
        }

        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scrollBy(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scrollBy(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollBy(-visibleLineCount());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollBy(visibleLineCount());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setScrollOffset(0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setScrollOffset(maxScroll);
            return true;
        }

        return super.keyPressed(event);
    }

    private void renderTextBox(GuiGraphicsExtractor context) {
        context.fill(textAreaX, textAreaY, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xA0101010);
        context.fill(textAreaX, textAreaY, textAreaX + textAreaWidth, textAreaY + 1, 0xFF5A5A5A);
        context.fill(textAreaX, textAreaY + textAreaHeight - 1, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xFF5A5A5A);
        context.fill(textAreaX, textAreaY, textAreaX + 1, textAreaY + textAreaHeight, 0xFF5A5A5A);
        context.fill(textAreaX + textAreaWidth - 1, textAreaY, textAreaX + textAreaWidth, textAreaY + textAreaHeight, 0xFF5A5A5A);

        int lineHeight = this.font.lineHeight + 1;
        int startX = textAreaX + BOX_PADDING;
        int startY = textAreaY + BOX_PADDING;
        int visible = visibleLineCount();
        for (int lineIndex = 0; lineIndex < visible; lineIndex++) {
            int absoluteIndex = scrollOffset + lineIndex;
            if (absoluteIndex >= wrappedLines.size()) {
                break;
            }

            InfoLine line = wrappedLines.get(absoluteIndex);
            context.text(
                this.font,
                Component.literal(line.text()),
                startX,
                startY + (lineIndex * lineHeight),
                opaqueColor(line.color())
            );
        }

        if (maxScroll > 0) {
            int trackLeft = textAreaX + textAreaWidth - 4;
            int trackTop = textAreaY + 2;
            int trackBottom = textAreaY + textAreaHeight - 2;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            context.fill(trackLeft, trackTop, trackLeft + 2, trackBottom, 0xFF3A3A3A);

            int thumbHeight = Math.max(10, (int) (trackHeight * (visibleLineCount() / (double) wrappedLines.size())));
            int thumbRange = Math.max(1, trackHeight - thumbHeight);
            int thumbTop = trackTop + (int) Math.round((scrollOffset / (double) maxScroll) * thumbRange);
            context.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFFC8C8C8);
        }
    }

    private void openReviewCase() {
        if (this.minecraft == null || context == null) {
            return;
        }

        this.minecraft.setScreen(new AlertManageScreen(this, context));
    }

    private void rebuildWrappedLines() {
        wrappedLines.clear();
        int wrapWidth = Math.max(80, textAreaWidth - (BOX_PADDING * 2) - 6);

        if (context == null) {
            addWrappedLine("Alert details are unavailable.", 0xAAAAAA, wrapWidth);
            updateScrollBounds();
            return;
        }

        addWrappedLine("Player: " + safePlayerName(context.displayPlayerName()), 0x55FFFF, wrapWidth);
        addWrappedLine("Risk: " + riskLabel(context) + " | Score: " + Math.max(0, context.score()), riskColor(context), wrapWidth);
        addBlankLine();

        addWrappedLine("Triggered Rules", 0xFFFF55, wrapWidth);
        Map<DetailGroup, List<String>> groupedDetails = groupedDetails();
        boolean addedGroupedDetails = false;
        for (DetailGroup group : DetailGroup.values()) {
            List<String> details = groupedDetails.get(group);
            if (details == null || details.isEmpty()) {
                continue;
            }

            addedGroupedDetails = true;
            addWrappedLine(group.label(), group.color(), wrapWidth);
            for (String detail : details) {
                addWrappedLine("  - " + detail, 0xFFD27F, wrapWidth);
            }
        }
        if (!addedGroupedDetails) {
            addWrappedLine("- none", 0x888888, wrapWidth);
        }
        addBlankLine();

        addWrappedLine("Captured Messages", 0xFFFF55, wrapWidth);
        List<String> capturedMessages = capturedMessages();
        if (capturedMessages.isEmpty()) {
            addWrappedLine("- none", 0x888888, wrapWidth);
        } else {
            for (String message : capturedMessages) {
                addWrappedLine("- " + message, 0x55FFFF, wrapWidth);
            }
        }

        updateScrollBounds();
    }

    private List<String> capturedMessages() {
        if (context == null) {
            return List.of();
        }

        Set<String> uniqueMessages = new LinkedHashSet<>();
        for (ReviewCaseMessage caseMessage : context.caseMessages()) {
            if (caseMessage == null) {
                continue;
            }

            String normalizedMessage = normalizeDisplayedMessage(caseMessage.getCleanText());
            if (!normalizedMessage.isBlank()) {
                uniqueMessages.add(normalizedMessage);
            }
        }
        for (String capturedMessage : context.capturedMessages()) {
            String normalizedMessage = normalizeDisplayedMessage(capturedMessage);
            if (!normalizedMessage.isBlank()) {
                uniqueMessages.add(normalizedMessage);
            }
        }

        String normalizedMessage = normalizeDisplayedMessage(context.rawMessage());
        if (!normalizedMessage.isBlank()) {
            uniqueMessages.add(normalizedMessage);
        }

        return List.copyOf(uniqueMessages);
    }

    private Map<DetailGroup, List<String>> groupedDetails() {
        Map<DetailGroup, List<String>> grouped = new EnumMap<>(DetailGroup.class);
        if (context == null) {
            return grouped;
        }

        for (AlertContextRegistry.RuleDetail detail : context.ruleDetails()) {
            if (detail == null) {
                continue;
            }

            String text = normalizeText(detail.detail());
            if (text.isBlank()) {
                continue;
            }

            DetailGroup group = DetailGroup.fromSource(detail.source());
            grouped.computeIfAbsent(group, ignored -> new ArrayList<>()).add(text);
        }

        return grouped;
    }

    private void addWrappedLine(String text, int color, int maxWidth) {
        String normalizedText = normalizeText(text);
        if (normalizedText.isEmpty()) {
            wrappedLines.add(new InfoLine("", color));
            return;
        }

        String remaining = normalizedText;
        while (!remaining.isEmpty()) {
            int splitIndex = findWrapIndex(remaining, maxWidth);
            String line = remaining.substring(0, splitIndex).trim();
            if (line.isEmpty()) {
                line = remaining.substring(0, 1);
                splitIndex = 1;
            }

            wrappedLines.add(new InfoLine(line, color));
            remaining = remaining.substring(splitIndex).trim();
        }
    }

    private void addBlankLine() {
        wrappedLines.add(new InfoLine("", 0xFFFFFF));
    }

    private int findWrapIndex(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (this.font.width(text) <= maxWidth) {
            return text.length();
        }

        int candidate = text.length();
        while (candidate > 1 && this.font.width(text.substring(0, candidate)) > maxWidth) {
            candidate--;
        }

        int splitAtSpace = text.lastIndexOf(' ', candidate);
        if (splitAtSpace > 0) {
            return splitAtSpace;
        }

        return Math.max(1, candidate);
    }

    private int visibleLineCount() {
        int lineHeight = this.font.lineHeight + 1;
        return Math.max(1, (textAreaHeight - (BOX_PADDING * 2)) / lineHeight);
    }

    private void updateScrollBounds() {
        maxScroll = Math.max(0, wrappedLines.size() - visibleLineCount());
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private void scrollBy(int delta) {
        setScrollOffset(scrollOffset + delta);
    }

    private void setScrollOffset(int value) {
        scrollOffset = clamp(value, 0, maxScroll);
    }

    private boolean isInsideTextArea(double mouseX, double mouseY) {
        return mouseX >= textAreaX
            && mouseX <= textAreaX + textAreaWidth
            && mouseY >= textAreaY
            && mouseY <= textAreaY + textAreaHeight;
    }

    private static String safePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "unknown";
        }

        return playerName.trim();
    }

    private static String riskLabel(AlertContextRegistry.AlertContext context) {
        if (context == null) {
            return "LOW";
        }

        PipelineDecision.Outcome outcome = context.outcome();
        int score = Math.max(0, context.score());
        if (outcome == PipelineDecision.Outcome.BLOCK || outcome == PipelineDecision.Outcome.BLACKLISTED) {
            return "CRITICAL";
        }
        if (score >= 75) {
            return "CRITICAL";
        }
        if (score >= 50) {
            return "HIGH";
        }
        if (score >= 25) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static int riskColor(AlertContextRegistry.AlertContext context) {
        return switch (riskLabel(context)) {
            case "MEDIUM" -> 0xFFCC66;
            case "HIGH" -> 0xFF5555;
            case "CRITICAL" -> 0xAA0000;
            default -> 0xFF5555;
        };
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String normalizeDisplayedMessage(String value) {
        return ChatLineClassifier.displayMessageOnly(normalizeText(value));
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

    private enum DetailGroup {
        RULE("Rule Stage", 0xFFFF55),
        PLAYER("Player Stage", 0x99FF99),
        BEHAVIOR("Behavior Stage", 0x55FFFF),
        SIMILARITY("Similarity Stage", 0xFF55FF),
        TREND("Trend Stage", 0x5599FF),
        FUNNEL("Funnel Stage", 0x00AAAA),
        MUTE("Mute Stage", 0xAAAAAA),
        OTHER("Other Stage", 0xAAAAAA);

        private final String label;
        private final int color;

        DetailGroup(String label, int color) {
            this.label = label;
            this.color = color;
        }

        private String label() {
            return label;
        }

        private int color() {
            return color;
        }

        private static DetailGroup fromSource(String source) {
            String normalizedSource = normalizeText(source);
            return switch (normalizedSource) {
                case "RuleStage" -> RULE;
                case "PlayerListStage" -> PLAYER;
                case "BehaviorStage" -> BEHAVIOR;
                case "LevenshteinStage" -> SIMILARITY;
                case "TrendStage" -> TREND;
                case "FunnelStage" -> FUNNEL;
                case "MuteStage" -> MUTE;
                default -> OTHER;
            };
        }
    }

    private record InfoLine(String text, int color) {
        private InfoLine {
            text = text == null ? "" : text;
        }
    }
}
