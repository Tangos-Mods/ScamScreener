package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * User-facing output settings screen.
 */
public final class MessageSettingsScreen extends BaseScreen {
    private ButtonWidget riskMessageButton;
    private ButtonWidget riskPingButton;
    private ButtonWidget blacklistMessageButton;
    private ButtonWidget blacklistPingButton;

    /**
     * Creates the message settings screen.
     *
     * @param parent the parent screen to return to
     */
    public MessageSettingsScreen(Screen parent) {
        super(Text.literal("Message Settings"), parent);
    }

    /**
     * Builds the message setting toggles.
     */
    @Override
    protected void init() {
        int contentWidth = defaultContentWidth();
        int x = centeredX(contentWidth);
        int y = CONTENT_TOP + 8;

        riskMessageButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleRiskMessage())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        riskPingButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleRiskPing())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        blacklistMessageButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleBlacklistMessage())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        blacklistPingButton = addDrawableChild(
            ButtonWidget.builder(Text.empty(), button -> toggleBlacklistPing())
                .dimensions(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addCloseButton(contentWidth);
        refreshButtons();
    }

    /**
     * Draws a small context header above the settings.
     *
     * @param context the current draw context
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param deltaTicks partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultContentWidth());
        drawSectionTitle(context, left, CONTENT_TOP - 18, "Warnings");
        drawLine(context, left, CONTENT_TOP - 6, "These toggles control local risk and blacklist alerts.");
    }

    private void toggleRiskMessage() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setShowRiskWarningMessage(!output.isShowRiskWarningMessage());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleRiskPing() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setPingOnRiskWarning(!output.isPingOnRiskWarning());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleBlacklistMessage() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setShowBlacklistWarningMessage(!output.isShowBlacklistWarningMessage());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void toggleBlacklistPing() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setPingOnBlacklistWarning(!output.isPingOnBlacklistWarning());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void refreshButtons() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();

        if (riskMessageButton != null) {
            riskMessageButton.setMessage(Text.literal("Show Risk Warning: " + onOff(output.isShowRiskWarningMessage())));
        }
        if (riskPingButton != null) {
            riskPingButton.setMessage(Text.literal("Ping On Risk Warning: " + onOff(output.isPingOnRiskWarning())));
        }
        if (blacklistMessageButton != null) {
            blacklistMessageButton.setMessage(Text.literal("Show Blacklist Warning: " + onOff(output.isShowBlacklistWarningMessage())));
        }
        if (blacklistPingButton != null) {
            blacklistPingButton.setMessage(Text.literal("Ping On Blacklist Warning: " + onOff(output.isPingOnBlacklistWarning())));
        }
    }
}
