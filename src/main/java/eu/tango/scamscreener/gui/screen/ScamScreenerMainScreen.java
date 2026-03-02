package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.ChatPipelineListener;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Root v2 GUI screen that routes into the main management pages.
 */
public final class ScamScreenerMainScreen extends BaseScreen {
    /**
     * Creates the root ScamScreener screen.
     *
     * @param parent the parent screen to return to
     */
    public ScamScreenerMainScreen(Screen parent) {
        super(Text.literal("ScamScreener"), parent);
    }

    /**
     * Builds the navigation buttons.
     */
    @Override
    protected void init() {
        int contentWidth = Math.min(420, Math.max(240, this.width - 40));
        int x = centeredX(contentWidth);
        int y = CONTENT_TOP + 126;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Whitelist"), button -> this.client.setScreen(new WhitelistScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Blacklist"), button -> this.client.setScreen(new BlacklistScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Review Queue"), button -> this.client.setScreen(new ReviewScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Review Settings"), button -> this.client.setScreen(new ReviewSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Runtime Settings"), button -> this.client.setScreen(new RuntimeSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rules Settings"), button -> this.client.setScreen(new RulesSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Message Settings"), button -> this.client.setScreen(new MessageSettingsScreen(this)))
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Reload Data"), button -> ScamScreenerRuntime.getInstance().reload())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addCloseButton(contentWidth);
    }

    /**
     * Draws the current runtime summary.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        RuntimeConfig config = runtime.config();
        int left = centeredX(Math.min(420, Math.max(240, this.width - 40)));
        int y = CONTENT_TOP - 18;
        ChatEvent lastEvent = ChatPipelineListener.getLastChatEvent().orElse(null);
        PipelineDecision lastDecision = ChatPipelineListener.getLastPipelineDecision().orElse(null);

        drawSectionTitle(context, left, y, "Runtime");
        y += 12;
        drawLine(context, left, y, "Whitelist Entries: " + runtime.whitelist().allEntries().size());
        y += 12;
        drawLine(context, left, y, "Blacklist Entries: " + runtime.blacklist().allEntries().size());
        y += 12;
        drawLine(context, left, y, "Review Entries: " + runtime.reviewStore().entries().size() + " / " + runtime.reviewStore().maxEntries());
        y += 12;
        drawLine(context, left, y, "Review Threshold: " + config.pipeline().reviewThreshold());
        y += 12;
        drawLine(context, left, y, "Review Capture: " + onOff(config.review().isCaptureEnabled()));
        y += 12;
        drawLine(context, left, y, "Model Stage: " + onOff(config.stages().isModelEnabled()));
        y += 12;
        drawLine(context, left, y, "Debug Logging: " + onOff(config.output().isDebugLogging()));
        y += 12;
        drawLine(context, left, y, "Similarity Phrases: " + runtime.rules().similarityStage().phrases().size());
        y += 12;
        drawLine(
            context,
            left,
            y,
            "State Cache: B " + runtime.behaviorStore().trackedSenderCount() + "/" + runtime.behaviorStore().trackedMessageCount()
                + " | T " + runtime.trendStore().trackedMessageCount()
                + " | F " + runtime.funnelStore().trackedSenderCount() + "/" + runtime.funnelStore().trackedStepCount()
        );
        y += 12;
        drawSectionTitle(context, left, y, "Live Status");
        y += 12;
        drawLine(context, left, y, "Last Outcome: " + (lastDecision == null ? "-" : lastDecision.getOutcome().name()));
        y += 12;
        drawLine(context, left, y, "Last Stage: " + (lastDecision == null ? "-" : emptyFallback(lastDecision.getDecidedByStage())));
        y += 12;
        drawLine(context, left, y, "Last Source: " + (lastEvent == null ? "-" : lastEvent.getSourceType().name()));
        y += 12;
        drawLine(context, left, y, "Last Sender: " + (lastEvent == null ? "-" : displaySender(lastEvent)));
        y += 12;
        drawLine(context, left, y, "Last Message: " + compactMessage(lastEvent));
    }

    private static String displaySender(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getSenderName().isBlank()) {
            return "Unknown";
        }

        return chatEvent.getSenderName();
    }

    private static String compactMessage(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getRawMessage().isBlank()) {
            return "-";
        }

        String message = chatEvent.getRawMessage().replace('\n', ' ').replace('\r', ' ').trim();
        if (message.length() <= 48) {
            return message;
        }

        return message.substring(0, 45) + "...";
    }

    private static String emptyFallback(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value;
    }
}
