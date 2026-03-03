package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Warning gate before opening the low-level advanced rule tuning screen.
 */
public final class AdvancedRulesWarningScreen extends BaseScreen {
    private final Screen navigationParent;

    public AdvancedRulesWarningScreen(Screen parent) {
        super(Text.literal("Advanced Rule Tuning"), parent);
        navigationParent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = defaultButtonWidth();
        int x = centeredX(contentWidth);
        int y = CONTENT_TOP + 52;

        addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(
                Text.literal("I KNOW WHAT I AM DOING"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new AdvancedRulesSettingsScreen(navigationParent));
                    }
                })
            .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
            .build());

        addCenteredFooterButton(contentWidth, Text.literal("Back"), button -> close());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
        int y = CONTENT_TOP - 6;
        drawSectionTitle(context, left, y, "Danger Zone");
        y += 14;
        drawLine(context, left, y, "This screen changes low-level pipeline weights, windows and thresholds.");
        y += 12;
        drawLine(context, left, y, "Bad values can weaken detection, flood alerts or break the mod.");
        y += 12;
        drawLine(context, left, y, "Only continue if you understand the current pipeline and the risk.");
    }
}
