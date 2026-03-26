package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * User-facing output settings screen.
 */
public final class MessageSettingsScreen extends BaseScreen {
    private Button scamWarningMessageButton;
    private Button scamWarningPingButton;
    private Button blacklistMessageButton;
    private Button blacklistPingButton;
    private Button autoLeaveMessageButton;

    /**
     * Creates the message settings screen.
     *
     * @param parent the parent screen to return to
     */
    public MessageSettingsScreen(Screen parent) {
        super(Component.literal("ScamScreener Messages"), parent);
    }

    /**
     * Builds the message setting toggles.
     */
    @Override
    protected void init() {
        ColumnState column = defaultColumnState();
        int contentWidth = column.buttonWidth();
        int x = column.x();
        int y = column.y() + 12;

        scamWarningMessageButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleRiskMessage())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        scamWarningPingButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleRiskPing())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        blacklistMessageButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleBlacklistMessage())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        blacklistPingButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleBlacklistPing())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
        y += ROW_HEIGHT;

        autoLeaveMessageButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> toggleAutoLeaveMessage())
                .bounds(x, y, contentWidth, DEFAULT_BUTTON_HEIGHT)
                .build()
        );

        addBackButton(contentWidth);
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(defaultButtonWidth());
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

    private void toggleAutoLeaveMessage() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        output.setShowAutoLeaveMessage(!output.isShowAutoLeaveMessage());
        ScamScreenerRuntime.getInstance().saveConfig();
        refreshButtons();
    }

    private void refreshButtons() {
        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();

        if (scamWarningMessageButton != null) {
            scamWarningMessageButton.setMessage(toggleText("Scam Warning Message: ", output.isShowRiskWarningMessage()));
        }
        if (scamWarningPingButton != null) {
            scamWarningPingButton.setMessage(toggleText("Scam Warning Ping: ", output.isPingOnRiskWarning()));
        }
        if (blacklistMessageButton != null) {
            blacklistMessageButton.setMessage(toggleText("Blacklist Warning Message: ", output.isShowBlacklistWarningMessage()));
        }
        if (blacklistPingButton != null) {
            blacklistPingButton.setMessage(toggleText("Blacklist Warning Ping: ", output.isPingOnBlacklistWarning()));
        }
        if (autoLeaveMessageButton != null) {
            autoLeaveMessageButton.setMessage(toggleText("Auto Leave Info Message: ", output.isShowAutoLeaveMessage()));
        }
    }
}
