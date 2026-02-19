package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.discord.UploadRelayClient;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.AsyncDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UploadRelaySettingsScreen extends ScamScreenerGUI {
	private final UploadRelayClient uploadRelayClient;

	private Button serverUrlButton;
	private Button statusButton;
	private Button redeemButton;
	private Button resetButton;
	private EditBox inviteCodeInput;
	private boolean busy;

	public UploadRelaySettingsScreen(Screen parent, UploadRelayClient uploadRelayClient) {
		super(Component.literal("Upload Relay Settings"), parent);
		this.uploadRelayClient = uploadRelayClient == null ? new UploadRelayClient() : uploadRelayClient;
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(420, Math.max(220, this.width - 30));
		int x = centeredX(buttonWidth);
		int y = CONTENT_START_Y;

		serverUrlButton = addInfoButton(x, y, buttonWidth);
		y += ROW_HEIGHT;

		int redeemButtonWidth = 88;
		int inputWidth = buttonWidth - redeemButtonWidth - 8;
		inviteCodeInput = this.addRenderableWidget(new EditBox(this.font, x, y, inputWidth, 20, Component.literal("Invite Code")));
		inviteCodeInput.setMaxLength(256);
		redeemButton = this.addRenderableWidget(Button.builder(Component.literal("Redeem"), button -> redeemInviteCode())
			.bounds(x + inputWidth + 8, y, redeemButtonWidth, 20)
			.build());
		y += ROW_HEIGHT;

		resetButton = this.addRenderableWidget(Button.builder(Component.literal("Reset Credentials"), button -> resetCredentials())
			.bounds(x, y, buttonWidth, 20)
			.build());
		y += ROW_HEIGHT;

		statusButton = addInfoButton(x, y, buttonWidth);
		addBackButton(buttonWidth);
		refreshStatus();
	}

	private void redeemInviteCode() {
		if (busy || inviteCodeInput == null) {
			return;
		}
		String inviteCode = inviteCodeInput.getValue() == null ? "" : inviteCodeInput.getValue().trim();
		if (inviteCode.isBlank()) {
			MessageDispatcher.reply(Messages.uploadRelayRedeemFailed("Invite code is empty."));
			return;
		}

		setBusy(true);
		MessageDispatcher.reply(Messages.uploadRelayRedeemStarted());
		AsyncDispatcher.runIo(() -> {
			UploadRelayClient.RedeemResult result = uploadRelayClient.redeemInviteCode(inviteCode);
			Minecraft client = Minecraft.getInstance();
			if (client == null) {
				return;
			}
			AsyncDispatcher.onClient(client, () -> {
				setBusy(false);
				refreshStatus();
				if (result.success()) {
					if (inviteCodeInput != null) {
						inviteCodeInput.setValue("");
					}
					MessageDispatcher.reply(Messages.uploadRelayRedeemSucceeded(result.detail()));
					return;
				}
				MessageDispatcher.reply(Messages.uploadRelayRedeemFailed(result.detail()));
			});
		});
	}

	private void resetCredentials() {
		if (busy) {
			return;
		}
		uploadRelayClient.clearCredentials();
		refreshStatus();
		MessageDispatcher.reply(Messages.uploadRelayCredentialsCleared());
	}

	private void setBusy(boolean busy) {
		this.busy = busy;
		if (redeemButton != null) {
			redeemButton.active = !busy;
		}
		if (resetButton != null) {
			resetButton.active = !busy;
		}
		if (inviteCodeInput != null) {
			inviteCodeInput.setEditable(!busy);
		}
	}

	private void refreshStatus() {
		UploadRelayClient.RelayStatus status = uploadRelayClient.status();
		if (serverUrlButton != null) {
			serverUrlButton.setMessage(Component.literal("Server URL: " + status.serverUrl()).withStyle(ChatFormatting.GRAY));
		}
		if (statusButton != null) {
			String message = status.configured()
				? "Credentials: configured (" + status.clientIdPreview() + ")"
				: "Credentials: not configured (auto on first upload)";
			statusButton.setMessage(Component.literal(message).withStyle(status.configured() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
		}
	}
}
