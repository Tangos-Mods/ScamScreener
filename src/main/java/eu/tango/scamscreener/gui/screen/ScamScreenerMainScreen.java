package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
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
        int contentWidth = Math.min(360, Math.max(220, this.width - 40));
        int x = centeredX(contentWidth);
        int y = CONTENT_TOP + 8;

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
            ButtonWidget.builder(Text.literal("Runtime Settings"), button -> this.client.setScreen(new RuntimeSettingsScreen(this)))
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
        int left = centeredX(Math.min(360, Math.max(220, this.width - 40)));
        int y = CONTENT_TOP - 18;

        drawSectionTitle(context, left, y, "Runtime");
        y += 12;
        drawLine(context, left, y, "Whitelist Entries: " + runtime.whitelist().allEntries().size());
        y += 12;
        drawLine(context, left, y, "Blacklist Entries: " + runtime.blacklist().allEntries().size());
        y += 12;
        drawLine(context, left, y, "Review Entries: " + runtime.reviewStore().entries().size());
        y += 12;
        drawLine(context, left, y, "Review Threshold: " + config.pipeline().reviewThreshold());
    }
}
