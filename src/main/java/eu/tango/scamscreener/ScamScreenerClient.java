package eu.tango.scamscreener;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.ai.LocalAiTrainer;
import eu.tango.scamscreener.ai.ModelUpdateCommandHandler;
import eu.tango.scamscreener.ai.TrainingCommandHandler;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.blacklist.BlacklistAlertService;
import eu.tango.scamscreener.client.ClientTickController;
import eu.tango.scamscreener.commands.ScamScreenerCommands;
import eu.tango.scamscreener.config.DebugConfig;
import eu.tango.scamscreener.config.ScamRulesConfig;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.chat.parser.OutgoingChatCommandParser;
import eu.tango.scamscreener.chat.trigger.TriggerContext;
import eu.tango.scamscreener.discord.DiscordWebhookUploader;
import eu.tango.scamscreener.gui.MainSettingsScreen;
import eu.tango.scamscreener.pipeline.model.DetectionOutcome;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.core.MessageEventParser;
import eu.tango.scamscreener.location.LocationService;
import eu.tango.scamscreener.lookup.MojangProfileService;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.lookup.TargetResolutionService;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.DebugRegistry;
import eu.tango.scamscreener.ui.ChatDecorator;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.NotificationService;
import eu.tango.scamscreener.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import eu.tango.scamscreener.security.EmailSafety;
import eu.tango.scamscreener.security.DiscordSafety;
import eu.tango.scamscreener.security.OutgoingMessageGuard;
import eu.tango.scamscreener.security.BypassCommandHandler;
import eu.tango.scamscreener.security.CoopAddSafety;
import net.minecraft.client.gui.screens.Screen;

public class ScamScreenerClient implements ClientModInitializer {
	private static final BlacklistManager BLACKLIST = new BlacklistManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(ScamScreenerClient.class);
	private static ScamScreenerClient INSTANCE;
	private final PlayerLookup playerLookup = new PlayerLookup();
	private final MojangProfileService mojangProfileService = new MojangProfileService();
	private final TrainingDataService trainingDataService = new TrainingDataService();
	private final LocalAiTrainer localAiTrainer = new LocalAiTrainer();
	private final DiscordWebhookUploader discordWebhookUploader = new DiscordWebhookUploader();
	private final ModelUpdateService modelUpdateService = new ModelUpdateService();
	private final MutePatternManager mutePatternManager = new MutePatternManager();
	private final DetectionPipeline detectionPipeline = new DetectionPipeline(mutePatternManager, new LocalAiScorer());
	private final LocationService locationService = new LocationService();
	private final EmailSafety emailSafety = new EmailSafety();
	private final DiscordSafety discordSafety = new DiscordSafety();
	private final CoopAddSafety coopAddSafety = new CoopAddSafety(BLACKLIST, playerLookup);
	private final TrainingCommandHandler trainingCommandHandler = new TrainingCommandHandler(
		trainingDataService,
		localAiTrainer,
		discordWebhookUploader
	);
	private final OutgoingMessageGuard outgoingMessageGuard = new OutgoingMessageGuard(emailSafety, discordSafety, coopAddSafety);
	private final ModelUpdateCommandHandler modelUpdateCommandHandler = new ModelUpdateCommandHandler(modelUpdateService);
	private final BypassCommandHandler bypassCommandHandler = new BypassCommandHandler(emailSafety, discordSafety, coopAddSafety);
	private final TargetResolutionService targetResolutionService = new TargetResolutionService(playerLookup, mojangProfileService, BLACKLIST);
	private boolean autoLeaveOnBlacklist;
	private DebugConfig debugConfig;
	private DebugReporter debugReporter;
	private BlacklistAlertService blacklistAlertService;
	private ClientTickController tickController;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		BLACKLIST.load();
		ScamRules.reloadConfig();
		autoLeaveOnBlacklist = ScamRulesConfig.loadOrCreate().autoLeaveOnBlacklist;
		mutePatternManager.load();
		loadDebugConfig();
		debugReporter = new DebugReporter(debugConfig);
		blacklistAlertService = new BlacklistAlertService(BLACKLIST, playerLookup, debugReporter, () -> autoLeaveOnBlacklist);
		Runnable openSettingsAction = () -> {
			Minecraft client = Minecraft.getInstance();
			if (client == null) {
				return;
			}
			Screen settingsScreen = createSettingsScreen(client.screen);
			if (settingsScreen != null) {
				client.setScreen(settingsScreen);
			}
		};
		tickController = new ClientTickController(
			mutePatternManager,
			detectionPipeline,
			openSettingsAction,
			locationService
		);
		registerCommands();
		registerHypixelMessageChecks();
		ClientTickEvents.END_CLIENT_TICK.register(client ->
			tickController.onClientTick(client, () -> modelUpdateService.checkForUpdateAsync(MessageDispatcher::reply)));
	}

	private void registerCommands() {
		Runnable openSettingsHandler = tickController::requestOpenSettings;

		ScamScreenerCommands commands = new ScamScreenerCommands(
			BLACKLIST,
			targetResolutionService::resolveTargetOrReply,
			mutePatternManager,
			trainingCommandHandler::captureChatAsTrainingData,
			trainingCommandHandler::captureMessageById,
			trainingCommandHandler::captureBulkLegit,
			trainingCommandHandler::migrateTrainingData,
			modelUpdateCommandHandler::handleModelUpdateCommand,
			modelUpdateCommandHandler::handleModelUpdateCheck,
			bypassCommandHandler::handleEmailBypass,
			this::setAllDebug,
			this::setDebugKey,
			this::debugStateSnapshot,
			() -> autoLeaveOnBlacklist,
			this::setAutoLeaveEnabled,
			trainingCommandHandler::trainLocalAiModel,
			trainingCommandHandler::resetLocalAiModel,
			trainingDataService::lastCapturedLine,
			ignored -> {},
			openSettingsHandler,
			MessageDispatcher::reply
		);
		commands.register();
	}

	public static Screen createSettingsScreen(Screen parent) {
		ScamScreenerClient instance = INSTANCE;
		if (instance == null) {
			return parent;
		}
		return instance.createMainSettingsScreen(parent);
	}

	private MainSettingsScreen createMainSettingsScreen(Screen parent) {
		return new MainSettingsScreen(
			parent,
			BLACKLIST,
			mutePatternManager,
			() -> autoLeaveOnBlacklist,
			this::setAutoLeaveEnabled,
			this::setAllDebug,
			this::setDebugKey,
			this::debugStateSnapshot,
			() -> modelUpdateCommandHandler.handleModelUpdateCheck(false),
			() -> modelUpdateCommandHandler.handleModelUpdateCheck(true),
			modelUpdateCommandHandler::latestPendingSnapshot,
			modelUpdateCommandHandler::handleModelUpdateCommand,
			() -> trainingCommandHandler.trainLocalAiModel()
		);
	}

	private void registerHypixelMessageChecks() {
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			String plain = message == null ? "" : message.getString();
			return !mutePatternManager.shouldBlock(plain);
		});
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> handleChatAllow(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleHypixelMessage(message));
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handleHypixelMessage(message));
		ClientSendMessageEvents.ALLOW_CHAT.register(this::handleOutgoingChat);
		ClientSendMessageEvents.ALLOW_COMMAND.register(this::handleOutgoingCommand);
	}

	private void handleHypixelMessage(Component message) {
		if (message == null) {
			return;
		}
		String plain = message.getString().trim();
		trainingDataService.recordChatLine(plain);

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			return;
		}

		MessageEvent event = MessageEventParser.parse(plain, System.currentTimeMillis());
		if (event != null) {
			detectionPipeline.process(event, MessageDispatcher::reply, NotificationService::playWarningTone)
				.ifPresent(this::autoAddFlaggedMessageToTrainingData);
		}
		if (BLACKLIST.isEmpty()) {
			return;
		}

		for (TriggerContext context : TriggerContext.values()) {
			blacklistAlertService.checkTriggerAndWarn(plain, context);
		}
	}

	private boolean handleOutgoingChat(String message) {
		if (!outgoingMessageGuard.allowChat(message)) {
			return false;
		}
		trainingDataService.recordOutgoingChatLine(localPlayerName(), message, "public");
		return true;
	}

	private boolean handleOutgoingCommand(String command) {
		if (!outgoingMessageGuard.allowCommand(command)) {
			return false;
		}
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse(command);
		if (parsed != null) {
			trainingDataService.recordOutgoingChatLine(localPlayerName(), parsed.message(), parsed.channel());
		}
		return true;
	}

	private static String localPlayerName() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			var profile = client.player.getGameProfile();
			if (profile != null && profile.name() != null && !profile.name().isBlank()) {
				return profile.name().trim();
			}
		}
		return "unknown";
	}

	private boolean handleChatAllow(Component message) {
		String plain = message == null ? "" : message.getString();
		if (mutePatternManager.shouldBlock(plain)) {
			debugReporter.debugMute("blocked chat: " + plain);
			return false;
		}
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(plain);
		if (parsed == null) {
			return true;
		}

		boolean blacklisted = BLACKLIST.isBlacklisted(parsed.playerName(), playerLookup::findUuidByName);
		debugReporter.debugChatColor("line speaker=" + TextUtil.anonymizedSpeakerKey(parsed.playerName()) + " blacklisted=" + blacklisted);
		Component decorated = ChatDecorator.decoratePlayerLine(message, parsed, blacklisted);
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(() -> {
				if (client.player != null) {
					client.player.displayClientMessage(decorated, false);
				}
			});
		}

		handleHypixelMessage(message);
		return false;
	}

	private void setAllDebug(boolean enabled) {
		modelUpdateService.setDebugEnabled(enabled);
		debugConfig.setAll(enabled);
		updateDebugConfig();
	}

	private void setAutoLeaveEnabled(boolean enabled) {
		ScamRulesConfig rulesConfig = ScamRulesConfig.loadOrCreate();
		rulesConfig.autoLeaveOnBlacklist = enabled;
		ScamRulesConfig.save(rulesConfig);
		ScamRules.reloadConfig();
		autoLeaveOnBlacklist = enabled;
	}

	private void setDebugKey(String key, boolean enabled) {
		if (key == null) {
			return;
		}
		String normalized = DebugRegistry.normalize(key);
		if (normalized.isBlank()) {
			return;
		}
		if ("updater".equals(normalized)) {
			modelUpdateService.setDebugEnabled(enabled);
		}
		debugConfig.setEnabled(normalized, enabled);
		updateDebugConfig();
	}

	private java.util.Map<String, Boolean> debugStateSnapshot() {
		java.util.Map<String, Boolean> states = debugConfig.snapshot();
		states.put("updater", modelUpdateService.isDebugEnabled());
		return states;
	}

	private void loadDebugConfig() {
		debugConfig = DebugConfig.loadOrCreate();
		if (debugConfig == null) {
			debugConfig = new DebugConfig();
		}
		modelUpdateService.setDebugEnabled(debugConfig.isEnabled("updater"));
	}

	private void updateDebugConfig() {
		DebugConfig.save(debugConfig);
	}


	private void autoAddFlaggedMessageToTrainingData(DetectionOutcome outcome) {
		if (outcome == null || outcome.result() == null || !outcome.result().shouldCapture()) {
			return;
		}
		MessageEvent event = outcome.event();
		if (event == null || event.rawMessage() == null || event.rawMessage().isBlank()) {
			return;
		}

		try {
			trainingDataService.appendDetectedEvent(event, outcome.result(), 1);
		} catch (IOException e) {
			LOGGER.debug("Failed to auto-save flagged message as training sample", e);
		}
	}


}
