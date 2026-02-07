package eu.tango.scamscreener;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.ai.LocalAiTrainer;
import eu.tango.scamscreener.ai.TrainingCommandHandler;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.blacklist.BlacklistAlertService;
import eu.tango.scamscreener.client.ClientTickController;
import eu.tango.scamscreener.client.IncomingMessageProcessor;
import eu.tango.scamscreener.commands.ScamScreenerCommands;
import eu.tango.scamscreener.config.DebugConfig;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.ScreeningResult;
import eu.tango.scamscreener.lookup.MojangProfileService;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.security.SafetyBypassStore;
import eu.tango.scamscreener.ui.Keybinds;
import eu.tango.scamscreener.ui.DebugRegistry;
import eu.tango.scamscreener.ui.ChatDecorator;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.FlaggingController;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.NotificationService;
import eu.tango.scamscreener.ui.messages.CommandMessages;
import eu.tango.scamscreener.util.TextUtil;
import eu.tango.scamscreener.util.UuidUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import eu.tango.scamscreener.security.OutgoingMessageGuard;

public class ScamScreenerClient implements ClientModInitializer {
	private static final BlacklistManager BLACKLIST = new BlacklistManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(ScamScreenerClient.class);
	private static final int LEGIT_LABEL = 0;
	private static final int SCAM_LABEL = 1;
	private final PlayerLookup playerLookup = new PlayerLookup();
	private final MojangProfileService mojangProfileService = new MojangProfileService();
	private final TrainingDataService trainingDataService = new TrainingDataService();
	private final LocalAiTrainer localAiTrainer = new LocalAiTrainer();
	private final ModelUpdateService modelUpdateService = new ModelUpdateService();
	private final MutePatternManager mutePatternManager = new MutePatternManager();
	private final DetectionPipeline detectionPipeline = new DetectionPipeline(mutePatternManager, new LocalAiScorer());
	private final Set<UUID> currentlyDetected = new HashSet<>();
	private final Set<String> warnedContexts = new HashSet<>();
	private final SafetyBypassStore emailSafety = SafetyBypassStore.emailStore();
	private final SafetyBypassStore discordSafety = SafetyBypassStore.discordLinkStore();
	private final TrainingCommandHandler trainingCommandHandler = new TrainingCommandHandler(trainingDataService, localAiTrainer);
	private final OutgoingMessageGuard outgoingMessageGuard = new OutgoingMessageGuard(emailSafety, discordSafety);
	private DebugConfig debugConfig;
	private DebugReporter debugReporter;
	private BlacklistAlertService blacklistAlertService;
	private FlaggingController flaggingController;
	private ClientTickController tickController;
	private IncomingMessageProcessor incomingMessageProcessor;

	@Override
	public void onInitializeClient() {
		BLACKLIST.load();
		ScamRules.reloadConfig();
		mutePatternManager.load();
		loadDebugConfig();
		debugReporter = new DebugReporter(debugConfig);
		blacklistAlertService = new BlacklistAlertService(BLACKLIST, playerLookup, warnedContexts, debugReporter);
		incomingMessageProcessor = new IncomingMessageProcessor(
			trainingDataService,
			detectionPipeline,
			BLACKLIST,
			blacklistAlertService,
			MessageDispatcher::reply,
			NotificationService::playWarningTone,
			this::autoAddFlaggedMessageToTrainingData
		);
		flaggingController = new FlaggingController(trainingCommandHandler);
		tickController = new ClientTickController(
			flaggingController,
			mutePatternManager,
			detectionPipeline,
			BLACKLIST,
			blacklistAlertService,
			playerLookup,
			currentlyDetected,
			warnedContexts,
			LEGIT_LABEL,
			SCAM_LABEL
		);
		registerCommands();
		Keybinds.register();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> Keybinds.register());
		registerHypixelMessageChecks();
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void registerCommands() {
		ScamScreenerCommands.CoreHandlers core = new ScamScreenerCommands.CoreHandlers(
			BLACKLIST,
			this::resolveTargetOrReply,
			mutePatternManager,
			currentlyDetected::remove
		);
		ScamScreenerCommands.AiHandlers ai = new ScamScreenerCommands.AiHandlers(
			trainingCommandHandler::captureChatAsTrainingData,
			trainingCommandHandler::captureMessageById,
			trainingCommandHandler::captureBulkLegit,
			trainingCommandHandler::migrateTrainingData,
			this::handleModelUpdateCommand,
			this::handleModelUpdateCheck,
			trainingCommandHandler::trainLocalAiModel,
			trainingCommandHandler::resetLocalAiModel,
			trainingDataService::lastCapturedLine
		);
		ScamScreenerCommands.DebugHandlers debug = new ScamScreenerCommands.DebugHandlers(
			this::setAllDebug,
			this::setDebugKey,
			this::debugStateSnapshot,
			this::isScreenEnabled,
			this::setScreenEnabled
		);
		ScamScreenerCommands commands = new ScamScreenerCommands(
			core,
			ai,
			this::handleEmailBypass,
			debug,
			MessageDispatcher::reply
		);
		commands.register();
	}

	private ResolvedTarget resolveTargetOrReply(String input) {
		UUID parsedUuid = UuidUtil.parse(input);
		if (parsedUuid != null) {
			return new ResolvedTarget(parsedUuid, playerLookup.findNameByUuid(parsedUuid));
		}

		UUID byName = playerLookup.findUuidByName(input);
		if (byName != null) {
			return new ResolvedTarget(byName, playerLookup.findNameByUuid(byName));
		}

		BlacklistManager.ScamEntry knownEntry = BLACKLIST.findByName(input);
		if (knownEntry != null) {
			return new ResolvedTarget(knownEntry.uuid(), knownEntry.name());
		}

		ResolvedTarget mojangResolved = mojangProfileService.lookupCached(input);
		if (mojangResolved != null) {
			return mojangResolved;
		}

		mojangProfileService.lookupAsync(input).thenAccept(resolved -> {
			if (resolved == null) {
				MessageDispatcher.reply(CommandMessages.unresolvedTarget(input));
				return;
			}
			MessageDispatcher.reply(CommandMessages.mojangLookupCompleted(input, resolved.name()));
		});
		MessageDispatcher.reply(CommandMessages.mojangLookupStarted(input));
		return null;
	}

	private int handleModelUpdateCommand(String action, String id) {
		return switch (action) {
			case "download" -> modelUpdateService.download(id, MessageDispatcher::reply);
			case "accept" -> modelUpdateService.accept(id, MessageDispatcher::reply);
			case "merge" -> modelUpdateService.merge(id, MessageDispatcher::reply);
			case "ignore" -> modelUpdateService.ignore(id, MessageDispatcher::reply);
			default -> 0;
		};
	}

	private int handleModelUpdateCheck(boolean force) {
		modelUpdateService.checkForUpdateAndDownloadAsync(MessageDispatcher::reply, force);
		return 1;
	}

	private int handleEmailBypass(String id) {
		if (id == null || id.isBlank()) {
			MessageDispatcher.reply(CommandMessages.emailBypassExpired());
			return 0;
		}
		SafetyBypassStore.Pending pending = emailSafety.takePending(id);
		if (pending == null) {
			pending = discordSafety.takePending(id);
		}
		if (pending == null || pending.message() == null || pending.message().isBlank()) {
			MessageDispatcher.reply(CommandMessages.emailBypassExpired());
			return 0;
		}
		String message = TextUtil.normalizeCommand(pending.message(), pending.command());
		if (pending.kind() == SafetyBypassStore.Kind.DISCORD_LINK) {
			discordSafety.allowOnce(message, pending.command());
		} else {
			emailSafety.allowOnce(message, pending.command());
		}
		if (pending.command()) {
			MessageDispatcher.sendCommand(message);
		} else {
			MessageDispatcher.sendChatMessage(message);
		}
		return 1;
	}

	private void registerHypixelMessageChecks() {
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !mutePatternManager.shouldBlock(message.getString()));
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> handleChatAllow(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleHypixelMessage(message));
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handleHypixelMessage(message));
		ClientSendMessageEvents.ALLOW_CHAT.register(outgoingMessageGuard::allowChat);
		ClientSendMessageEvents.ALLOW_COMMAND.register(outgoingMessageGuard::allowCommand);
	}

	private void onClientTick(Minecraft client) {
		tickController.onClientTick(client, () -> modelUpdateService.checkForUpdateAsync(MessageDispatcher::reply));
	}

	private void handleHypixelMessage(Component message) {
		incomingMessageProcessor.process(message);
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

		boolean blacklisted = isBlacklisted(parsed.playerName());
		debugReporter.debugChatColor("line player=" + parsed.playerName() + " blacklisted=" + blacklisted);
		ScreeningResult screening = incomingMessageProcessor.process(message);
		Component decorated = ChatDecorator.decoratePlayerLine(message, parsed, blacklisted, isScreenEnabled() ? screening : null);
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(() -> {
				if (client.player != null) {
					client.player.displayClientMessage(decorated, false);
				}
			});
		}
		return false;
	}


	private boolean isBlacklisted(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return false;
		}
		UUID uuid = playerLookup.findUuidByName(playerName);
		if (uuid != null) {
			return BLACKLIST.contains(uuid);
		}
		return BLACKLIST.findByName(playerName) != null;
	}

	private void setAllDebug(boolean enabled) {
		modelUpdateService.setDebugEnabled(enabled);
		debugConfig.setAll(enabled);
		updateDebugConfig();
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

	private boolean isScreenEnabled() {
		return debugConfig != null && debugConfig.isEnabled("screen");
	}

	private void setScreenEnabled(boolean enabled) {
		if (debugConfig == null) {
			return;
		}
		debugConfig.setEnabled("screen", enabled);
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


	private void autoAddFlaggedMessageToTrainingData(ScreeningResult screening) {
		if (screening == null || screening.result() == null || !screening.result().shouldCapture()) {
			return;
		}
		MessageEvent event = screening.event();
		if (event == null || event.rawMessage() == null || event.rawMessage().isBlank()) {
			return;
		}

		try {
			trainingDataService.appendRows(List.of(event.rawMessage()), 1);
		} catch (IOException e) {
			LOGGER.debug("Failed to auto-save flagged message as training sample", e);
		}
	}


}
