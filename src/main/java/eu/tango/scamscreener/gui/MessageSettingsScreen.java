package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MessageSettingsScreen extends ScamScreenerGUI {
	private Button scamWarningMessageButton;
	private Button scamWarningPingButton;
	private Button blacklistWarningMessageButton;
	private Button blacklistWarningPingButton;
	private Button autoLeaveMessageButton;

	MessageSettingsScreen(Screen parent) {
		super(Component.literal("ScamScreener Messages"), parent);
	}

	@Override
	protected void init() {
		ColumnState column = defaultColumnState();
		int buttonWidth = column.buttonWidth();
		int x = column.x();
		int y = column.y();

		scamWarningMessageButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setShowScamWarningMessage(!ScamRules.showScamWarningMessage());
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		scamWarningPingButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setPingOnScamWarning(!ScamRules.pingOnScamWarning());
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		blacklistWarningMessageButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setShowBlacklistWarningMessage(!ScamRules.showBlacklistWarningMessage());
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		blacklistWarningPingButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setPingOnBlacklistWarning(!ScamRules.pingOnBlacklistWarning());
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());
		y += ROW_HEIGHT;

		autoLeaveMessageButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setShowAutoLeaveMessage(!ScamRules.showAutoLeaveMessage());
			refreshButtons();
		}).bounds(x, y, buttonWidth, 20).build());

		addBackButton(buttonWidth);
		refreshButtons();
	}

	private void refreshButtons() {
		if (scamWarningMessageButton != null) {
			scamWarningMessageButton.setMessage(onOffLine("Scam Warning Message: ", ScamRules.showScamWarningMessage()));
		}
		if (scamWarningPingButton != null) {
			scamWarningPingButton.setMessage(onOffLine("Scam Warning Ping: ", ScamRules.pingOnScamWarning()));
		}
		if (blacklistWarningMessageButton != null) {
			blacklistWarningMessageButton.setMessage(onOffLine("Blacklist Warning Message: ", ScamRules.showBlacklistWarningMessage()));
		}
		if (blacklistWarningPingButton != null) {
			blacklistWarningPingButton.setMessage(onOffLine("Blacklist Warning Ping: ", ScamRules.pingOnBlacklistWarning()));
		}
		if (autoLeaveMessageButton != null) {
			autoLeaveMessageButton.setMessage(onOffLine("Auto Leave Info Message: ", ScamRules.showAutoLeaveMessage()));
		}
	}
}
