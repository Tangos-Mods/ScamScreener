package eu.tango.scamscreener;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.ai.ModelUpdateService;
import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.ai.TrainingUploadReminderService;
import eu.tango.scamscreener.ai.LocalAiTrainer;
import eu.tango.scamscreener.ai.ModelUpdateCommandHandler;
import eu.tango.scamscreener.ai.TrainingCommandHandler;
import eu.tango.scamscreener.ai.FunnelMetricsService;
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
import eu.tango.scamscreener.gui.MainSettingsScreen;
import eu.tango.scamscreener.gui.AlertInfoScreen;
import eu.tango.scamscreener.gui.AlertManageScreen;
import eu.tango.scamscreener.gui.WhitelistSettingsScreen;
import eu.tango.scamscreener.pipeline.core.DetectionScoring;
import eu.tango.scamscreener.pipeline.model.DetectionOutcome;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.core.MessageEventParser;
import eu.tango.scamscreener.market.AuctionBinIndexRepository;
import eu.tango.scamscreener.market.AuctionSalesRepository;
import eu.tango.scamscreener.market.BazaarRepository;
import eu.tango.scamscreener.market.HypixelMarketApiClient;
import eu.tango.scamscreener.market.MarketClickConfirmStore;
import eu.tango.scamscreener.market.MarketDataRefreshService;
import eu.tango.scamscreener.market.MarketDatabase;
import eu.tango.scamscreener.market.MarketRiskAnalyzer;
import eu.tango.scamscreener.market.MarketUiGuard;
import eu.tango.scamscreener.market.NpcPriceCatalog;
import eu.tango.scamscreener.market.RareItemClassifier;
import eu.tango.scamscreener.lookup.MojangProfileService;
import eu.tango.scamscreener.lookup.PlayerLookup;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.lookup.TargetResolutionService;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.DebugRegistry;
import eu.tango.scamscreener.ui.ChatDecorator;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.AlertReviewRegistry;
import eu.tango.scamscreener.ui.EducationMessages;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.NotificationService;
import eu.tango.scamscreener.util.AsyncDispatcher;
import eu.tango.scamscreener.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import eu.tango.scamscreener.security.EmailSafety;
import eu.tango.scamscreener.security.DiscordSafety;
import eu.tango.scamscreener.security.OutgoingMessageGuard;
import eu.tango.scamscreener.security.BypassCommandHandler;
import eu.tango.scamscreener.security.CoopAddSafety;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.minecraft.client.gui.screens.Screen;

public class ScamScreenerClient implements ClientModInitializer {
	private static final BlacklistManager BLACKLIST = new BlacklistManager();
	private static final WhitelistManager WHITELIST = new WhitelistManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(ScamScreenerClient.class);
	private static ScamScreenerClient INSTANCE;
	private final PlayerLookup playerLookup = new PlayerLookup();
	private final MojangProfileService mojangProfileService = new MojangProfileService();
	private final TrainingDataService trainingDataService = new TrainingDataService(ScamRulesConfig.loadOrCreate().capturedChatCacheSize);
	private final TrainingUploadReminderService trainingUploadReminderService = new TrainingUploadReminderService(trainingDataService::trainingDataPath);
	private final FunnelMetricsService funnelMetricsService = new FunnelMetricsService();
	private final LocalAiTrainer localAiTrainer = new LocalAiTrainer();
	private final ModelUpdateService modelUpdateService = new ModelUpdateService();
	private final MutePatternManager mutePatternManager = new MutePatternManager();
	private final DetectionPipeline detectionPipeline = new DetectionPipeline(
		mutePatternManager,
		WHITELIST,
		playerLookup::findUuidByName,
		new LocalAiScorer()
	);
	private final MarketDatabase marketDatabase = new MarketDatabase();
	private final AuctionBinIndexRepository auctionBinIndexRepository = new AuctionBinIndexRepository(marketDatabase);
	private final AuctionSalesRepository auctionSalesRepository = new AuctionSalesRepository(marketDatabase);
	private final BazaarRepository bazaarRepository = new BazaarRepository(marketDatabase);
	private final NpcPriceCatalog npcPriceCatalog = new NpcPriceCatalog(marketDatabase);
	private final HypixelMarketApiClient hypixelMarketApiClient = new HypixelMarketApiClient();
	private final MarketRiskAnalyzer marketRiskAnalyzer = new MarketRiskAnalyzer(
		auctionBinIndexRepository,
		auctionSalesRepository,
		bazaarRepository,
		npcPriceCatalog
	);
	private final RareItemClassifier rareItemClassifier = new RareItemClassifier();
	private final MarketClickConfirmStore marketClickConfirmStore = new MarketClickConfirmStore();
	private final EmailSafety emailSafety = new EmailSafety();
	private final DiscordSafety discordSafety = new DiscordSafety();
	private final CoopAddSafety coopAddSafety = new CoopAddSafety(BLACKLIST, playerLookup);
	private final TrainingCommandHandler trainingCommandHandler = new TrainingCommandHandler(
		trainingDataService,
		funnelMetricsService,
		localAiTrainer
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
	private MarketDataRefreshService marketDataRefreshService;
	private MarketUiGuard marketUiGuard;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		AsyncDispatcher.init();
		BLACKLIST.load();
		WHITELIST.load();
		ScamRules.reloadConfig();
		autoLeaveOnBlacklist = ScamRulesConfig.loadOrCreate().autoLeaveOnBlacklist;
		mutePatternManager.load();
		loadDebugConfig();
		debugReporter = new DebugReporter(debugConfig);
		blacklistAlertService = new BlacklistAlertService(BLACKLIST, playerLookup, debugReporter, () -> autoLeaveOnBlacklist);
		marketDataRefreshService = new MarketDataRefreshService(
			hypixelMarketApiClient,
			auctionBinIndexRepository,
			auctionSalesRepository,
			bazaarRepository,
			npcPriceCatalog,
			debugReporter
		);
		marketUiGuard = new MarketUiGuard(
			marketRiskAnalyzer,
			rareItemClassifier,
			marketClickConfirmStore,
			debugReporter
		);
		refreshWhitelistDisplayNamesAsync();
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
			trainingUploadReminderService
		);
		registerCommands();
		registerHypixelMessageChecks();
		registerMarketTooltipHook();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if (marketDataRefreshService != null) {
				marketDataRefreshService.close();
			}
			AsyncDispatcher.shutdown();
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			tickController.onClientTick(client, () -> modelUpdateService.checkForUpdateAsync(MessageDispatcher::reply));
			handleMarketTick(client);
		});
	}

	private void registerCommands() {
		Runnable openSettingsHandler = tickController::requestOpenSettings;
		Runnable openWhitelistScreenHandler = this::openWhitelistSettingsScreen;

		ScamScreenerCommands commands = new ScamScreenerCommands(
			BLACKLIST,
			WHITELIST,
			targetResolutionService::resolveTargetOrReply,
			mutePatternManager,
			trainingCommandHandler::captureMessageById,
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
			trainingCommandHandler::showFunnelMetrics,
			trainingCommandHandler::resetFunnelMetrics,
			trainingDataService::lastCapturedLine,
			this::openAlertManageScreen,
			this::openAlertInfoScreen,
			this::openPlayerReviewScreen,
			this::suggestReviewPlayerNames,
			this::openRecentCapturedReviewScreen,
			this::disableEducationMessage,
			ignored -> {},
			openSettingsHandler,
			openWhitelistScreenHandler,
			MessageDispatcher::reply
		);
		commands.register();
	}

	private void openWhitelistSettingsScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		refreshWhitelistDisplayNamesAsync();
		AsyncDispatcher.onClient(client, () -> {
			Screen parent = client.screen;
			client.setScreen(new WhitelistSettingsScreen(parent, WHITELIST, targetResolutionService::resolveTargetOrReply));
		});
	}

	private void refreshWhitelistDisplayNamesAsync() {
		for (WhitelistManager.WhitelistEntry entry : WHITELIST.allEntries()) {
			if (entry == null || entry.uuid() == null) {
				continue;
			}
			String onlineName = playerLookup.findNameByUuid(entry.uuid());
			if (isUsableName(onlineName)) {
				WHITELIST.updateDisplayName(entry.uuid(), onlineName);
				continue;
			}
			ResolvedTarget cached = mojangProfileService.lookupByUuidCached(entry.uuid());
			if (cached != null && isUsableName(cached.name())) {
				WHITELIST.updateDisplayName(entry.uuid(), cached.name());
				continue;
			}
			mojangProfileService.lookupByUuidAsync(entry.uuid()).thenAccept(resolved -> {
				if (resolved == null || !isUsableName(resolved.name())) {
					return;
				}
				WHITELIST.updateDisplayName(entry.uuid(), resolved.name());
			});
		}
	}

	private static boolean isUsableName(String name) {
		return name != null && !name.isBlank() && !"unknown".equalsIgnoreCase(name.trim());
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
			WHITELIST,
			mutePatternManager,
			targetResolutionService::resolveTargetOrReply,
			this::refreshWhitelistDisplayNamesAsync,
			() -> autoLeaveOnBlacklist,
			this::setAutoLeaveEnabled,
			this::setAllDebug,
			this::setDebugKey,
			this::debugStateSnapshot,
			() -> modelUpdateCommandHandler.handleModelUpdateCheck(false),
			() -> modelUpdateCommandHandler.handleModelUpdateCheck(true),
			modelUpdateCommandHandler::latestPendingSnapshot,
			modelUpdateCommandHandler::handleModelUpdateCommand,
			funnelMetricsService::snapshot,
			this::openTrainingCsvReviewScreen,
			() -> trainingCommandHandler.trainLocalAiModel()
		);
	}

	public static boolean allowMarketInventoryClick(int containerId, int slotId, int buttonNum, ClickType clickType) {
		ScamScreenerClient instance = INSTANCE;
		if (instance == null || instance.marketUiGuard == null) {
			return true;
		}
		return instance.marketUiGuard.allowInventoryClick(containerId, slotId, buttonNum, clickType);
	}

	private void handleMarketTick(Minecraft client) {
		if (marketDataRefreshService == null) {
			return;
		}
		if (client == null || client.player == null || client.getConnection() == null) {
			if (marketUiGuard != null) {
				marketUiGuard.reset();
			}
			return;
		}
		marketDataRefreshService.tick(ScamRules.marketSafetyEnabled(), System.currentTimeMillis());
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

	private void registerMarketTooltipHook() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (marketUiGuard == null || lines == null) {
				return;
			}
			marketUiGuard.appendMarketTooltipStatus(stack, lines);
		});
	}

	private void handleHypixelMessage(Component message) {
		if (message == null) {
			return;
		}
		String plain = message.getString().trim();
		long now = System.currentTimeMillis();
		MessageEvent event = MessageEventParser.parse(plain, now);
		int reviewScore = 0;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			if (event != null) {
				trainingDataService.recordChatEvent(event, reviewScore);
			} else {
				trainingDataService.recordChatLine(plain, reviewScore);
			}
			return;
		}

		if (event != null) {
			final int[] scoreHolder = {0};
			detectionPipeline.process(event, MessageDispatcher::reply, NotificationService::playWarningTone, evaluation -> {
				funnelMetricsService.recordEvaluation(evaluation);
				if (evaluation == null || evaluation.result() == null) {
					scoreHolder[0] = 0;
					return;
				}
				scoreHolder[0] = shouldAutoMarkScamInReview(evaluation.result()) ? toReviewScore(evaluation.result()) : 0;
			})
				.ifPresent(this::autoAddFlaggedMessageToTrainingData);
			reviewScore = scoreHolder[0];
		}
		if (event != null) {
			trainingDataService.recordChatEvent(event, reviewScore);
		} else {
			trainingDataService.recordChatLine(plain, reviewScore);
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
			AsyncDispatcher.onClient(client, () -> {
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

	private int openAlertManageScreen(String alertId) {
		AlertReviewRegistry.AlertContext context = AlertReviewRegistry.contextById(alertId);
		if (context == null) {
			MessageDispatcher.reply(Messages.alertReviewContextMissing());
			return 0;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			MessageDispatcher.reply(Messages.alertReviewOpenFailed());
			return 0;
		}
		AsyncDispatcher
			.supplyBackground(() -> collectReviewRows(context))
			.thenAccept(rows -> AsyncDispatcher.onClient(client, () -> {
				Screen parent = client.screen;
				client.setScreen(new AlertManageScreen(
					parent,
					context,
					rows,
					request -> saveAlertReview(context, request)
				));
			}))
			.exceptionally(error -> {
				LOGGER.warn("Failed to open alert manage review screen", error);
				AsyncDispatcher.onClient(client, () -> MessageDispatcher.reply(Messages.alertReviewOpenFailed()));
				return null;
			});
		return 1;
	}

	private int openAlertInfoScreen(String alertId) {
		AlertReviewRegistry.AlertContext context = AlertReviewRegistry.contextById(alertId);
		if (context == null) {
			MessageDispatcher.reply(Messages.alertReviewContextMissing());
			return 0;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			MessageDispatcher.reply(Messages.alertReviewOpenFailed());
			return 0;
		}
		AsyncDispatcher
			.supplyBackground(() -> collectReviewMessages(context))
			.thenAccept(messages -> AsyncDispatcher.onClient(client, () -> {
				Screen parent = client.screen;
				client.setScreen(new AlertInfoScreen(parent, context, messages));
			}))
			.exceptionally(error -> {
				LOGGER.warn("Failed to open alert info review screen", error);
				AsyncDispatcher.onClient(client, () -> MessageDispatcher.reply(Messages.alertReviewOpenFailed()));
				return null;
			});
		return 1;
	}

	private int openPlayerReviewScreen(String playerName) {
		String safePlayerName = playerName == null ? "" : playerName.trim();
		if (safePlayerName.isBlank()) {
			MessageDispatcher.reply(Messages.noChatToCapture());
			return 0;
		}
		String contextId = AlertReviewRegistry.register(
			safePlayerName,
			new ScamRules.ScamAssessment(0, ScamRules.ScamRiskLevel.LOW, Set.of(), Map.of(), null, List.of()),
			Map.of()
		);
		return openAlertManageScreen(contextId);
	}

	private List<String> suggestReviewPlayerNames() {
		Set<String> seen = new LinkedHashSet<>();
		List<String> suggestions = new ArrayList<>();
		for (var onlinePlayer : playerLookup.onlinePlayers()) {
			if (onlinePlayer == null || onlinePlayer.getProfile() == null) {
				continue;
			}
			String name = onlinePlayer.getProfile().name();
			if (name == null || name.isBlank()) {
				continue;
			}
			String safeName = name.trim();
			if (!trainingDataService.hasRecentCaptureForPlayer(safeName)) {
				continue;
			}
			String dedupeKey = safeName.toLowerCase(Locale.ROOT);
			if (seen.add(dedupeKey)) {
				suggestions.add(safeName);
			}
		}
		suggestions.sort(String.CASE_INSENSITIVE_ORDER);
		return suggestions;
	}

	private int openRecentCapturedReviewScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			MessageDispatcher.reply(Messages.alertReviewOpenFailed());
			return 0;
		}

		AsyncDispatcher
			.supplyBackground(this::buildRecentCapturedReviewRows)
			.thenAccept(reviewRows -> AsyncDispatcher.onClient(client, () -> {
				if (reviewRows.isEmpty()) {
					MessageDispatcher.reply(Messages.noChatToCapture());
					return;
				}
				Screen parent = client.screen;
				client.setScreen(new AlertManageScreen(
					parent,
					Component.literal("Review Logged Chat"),
					reviewRows,
					this::saveRecentCapturedReview,
					this::buildRecentCapturedReviewRows
				));
			}))
			.exceptionally(error -> {
				LOGGER.warn("Failed to open recent captured review screen", error);
				AsyncDispatcher.onClient(client, () -> MessageDispatcher.reply(Messages.alertReviewOpenFailed()));
				return null;
			});
		return 1;
	}

	private void openTrainingCsvReviewScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			MessageDispatcher.reply(Messages.trainingCsvReviewFailed("Minecraft client screen context is unavailable."));
			return;
		}

		AsyncDispatcher
			.supplyBackground(this::loadTrainingCsvReviewRows)
			.thenAccept(result -> AsyncDispatcher.onClient(client, () -> {
				if (result.failureDetail() != null) {
					MessageDispatcher.reply(Messages.trainingCsvReviewFailed(result.failureDetail()));
					return;
				}
				if (result.rows().isEmpty()) {
					MessageDispatcher.reply(Messages.trainingCsvReviewNoData(trainingDataService.trainingDataPath().toString()));
					return;
				}
				Screen parent = client.screen;
				client.setScreen(new AlertManageScreen(
					parent,
					Component.literal("Review Training CSV"),
					result.rows(),
					this::saveTrainingCsvReview,
					this::openTrainingCsvFile
				));
			}))
			.exceptionally(error -> {
				LOGGER.warn("Failed to load training csv review rows", error);
				AsyncDispatcher.onClient(client, () -> MessageDispatcher.reply(Messages.trainingCsvReviewFailed(safeErrorMessage(error))));
				return null;
			});
	}

	private void openTrainingCsvFile() {
		var trainingPath = trainingDataService.trainingDataPath();
		if (!Files.isRegularFile(trainingPath)) {
			MessageDispatcher.reply(Messages.trainingCsvReviewNoData(trainingPath.toString()));
			return;
		}
		if (!openFileWithSystemDefaultApp(trainingPath)) {
			MessageDispatcher.reply(Messages.trainingCsvReviewFailed("Failed to open training csv file with system default app."));
		}
	}

	private static boolean openFileWithSystemDefaultApp(Path filePath) {
		if (filePath == null) {
			return false;
		}

		try {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.OPEN)) {
					desktop.open(filePath.toFile());
					return true;
				}
			}
		} catch (Exception ignored) {
		}

		return openFileWithOsFallback(filePath);
	}

	private static boolean openFileWithOsFallback(Path filePath) {
		String absolutePath = filePath.toAbsolutePath().toString();
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		try {
			if (os.contains("win")) {
				String escapedPath = absolutePath.replace("\"", "\"\"");
				new ProcessBuilder("cmd", "/c", "start \"\" \"" + escapedPath + "\"").start();
				return true;
			}
			if (os.contains("mac")) {
				new ProcessBuilder("open", absolutePath).start();
				return true;
			}
			new ProcessBuilder("xdg-open", absolutePath).start();
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private int saveTrainingCsvReview(AlertManageScreen.SaveRequest request) {
		if (request == null) {
			return 0;
		}
		List<TrainingCommandHandler.CsvLabelUpdate> updates = new ArrayList<>();
		for (AlertManageScreen.ReviewedSelection selection : request.selections()) {
			if (selection == null) {
				continue;
			}
			updates.add(new TrainingCommandHandler.CsvLabelUpdate(selection.rowId(), selection.label()));
		}
		return trainingCommandHandler.saveTrainingCsvReview(updates, request.upload());
	}

	private int saveRecentCapturedReview(AlertManageScreen.SaveRequest request) {
		if (request == null) {
			return 0;
		}
		List<TrainingCommandHandler.ReviewedMessage> selections = new ArrayList<>();
		for (AlertManageScreen.ReviewedSelection selection : request.selections()) {
			if (selection == null || selection.message() == null || selection.message().isBlank()) {
				continue;
			}
			selections.add(new TrainingCommandHandler.ReviewedMessage(selection.message(), selection.label()));
		}
		return trainingCommandHandler.saveReviewedMessages(selections, request.upload());
	}

	private int saveAlertReview(AlertReviewRegistry.AlertContext context, AlertManageScreen.SaveRequest request) {
		if (request == null || context == null) {
			return 0;
		}
		List<TrainingCommandHandler.ReviewedMessage> selections = new ArrayList<>();
		for (AlertManageScreen.ReviewedSelection selection : request.selections()) {
			if (selection == null || selection.message() == null || selection.message().isBlank()) {
				continue;
			}
			selections.add(new TrainingCommandHandler.ReviewedMessage(selection.message(), selection.label()));
		}
		int result = trainingCommandHandler.saveReviewedMessages(selections, request.upload());
		if (result <= 0) {
			return result;
		}

		String playerName = context.playerName() == null ? "" : context.playerName().trim();
		if (playerName.isBlank() || "unknown".equalsIgnoreCase(playerName)) {
			return result;
		}

		if (request.addToBlacklist()) {
			int score = Math.max(0, Math.min(100, context.riskScore()));
			String reason = AlertReviewRegistry.bestRuleCode(context);
			if (reason == null || reason.isBlank()) {
				MessageDispatcher.sendCommand("scamscreener add " + playerName + " " + score);
			} else {
				MessageDispatcher.sendCommand("scamscreener add " + playerName + " " + score + " " + reason);
			}
		}
		if (request.addToBlock()) {
			MessageDispatcher.sendCommand("block " + playerName);
		}
		return result;
	}

	private int disableEducationMessage(String messageId) {
		return EducationMessages.disableMessage(messageId);
	}

	private List<AlertManageScreen.ReviewRow> collectReviewRows(AlertReviewRegistry.AlertContext context) {
		if (context == null) {
			return List.of();
		}

		List<AlertManageScreen.ReviewRow> out = new ArrayList<>();
		Map<String, Integer> indexByMessage = new LinkedHashMap<>();
		String playerName = context.playerName();
		if (playerName != null && !playerName.isBlank() && !"unknown".equalsIgnoreCase(playerName.trim())) {
			for (TrainingDataService.CapturedChat capture : trainingDataService.recentCapturedForPlayer(playerName, trainingDataService.maxCapturedChatLines())) {
				if (capture == null) {
					continue;
				}
				String rowId = "player-"
					+ capture.timestampMs()
					+ "-"
					+ capture.speakerKey()
					+ "-"
					+ Integer.toHexString((capture.rawMessage() == null ? "" : capture.rawMessage()).hashCode());
				addOrMergeReviewRow(out, indexByMessage, rowId, capture.rawMessage(), capture.modScore());
			}
		}

		int contextScore = Math.max(0, Math.min(100, context.riskScore()));
		int contextModScore = shouldAutoMarkScamInReview(context.riskLevel()) ? contextScore : 0;
		List<String> contextMessages = context.evaluatedMessages() == null ? List.of() : context.evaluatedMessages();
		for (int i = 0; i < contextMessages.size(); i++) {
			addOrMergeReviewRow(out, indexByMessage, "alert-" + i, contextMessages.get(i), contextModScore);
		}
		return out;
	}

	private List<String> collectReviewMessages(AlertReviewRegistry.AlertContext context) {
		List<AlertManageScreen.ReviewRow> rows = collectReviewRows(context);
		if (rows.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (AlertManageScreen.ReviewRow row : rows) {
			if (row == null || row.message() == null || row.message().isBlank()) {
				continue;
			}
			out.add(row.message());
		}
		return out;
	}

	private static void addOrMergeReviewRow(
		List<AlertManageScreen.ReviewRow> out,
		Map<String, Integer> indexByMessage,
		String rowId,
		String rawMessage,
		int modScore
	) {
		if (out == null || indexByMessage == null || rawMessage == null) {
			return;
		}
		String normalized = rawMessage.replace('\n', ' ').replace('\r', ' ').trim();
		if (normalized.isBlank()) {
			return;
		}
		int clampedScore = Math.max(0, Math.min(100, modScore));
		Integer existingIndex = indexByMessage.get(normalized);
		if (existingIndex != null && existingIndex >= 0 && existingIndex < out.size()) {
			AlertManageScreen.ReviewRow existing = out.get(existingIndex);
			if (existing != null && clampedScore > existing.modScore()) {
				out.set(
					existingIndex,
					new AlertManageScreen.ReviewRow(existing.rowId(), existing.message(), existing.currentLabel(), clampedScore)
				);
			}
			return;
		}

		String safeRowId = (rowId == null || rowId.isBlank())
			? "row-" + out.size() + "-" + Integer.toHexString(normalized.hashCode())
			: rowId;
		out.add(new AlertManageScreen.ReviewRow(safeRowId, normalized, -1, clampedScore));
		indexByMessage.put(normalized, out.size() - 1);
	}

	private List<AlertManageScreen.ReviewRow> buildRecentCapturedReviewRows() {
		List<TrainingDataService.CapturedChat> captures = trainingDataService.recentPendingCaptured(trainingDataService.maxCapturedChatLines());
		if (captures.isEmpty()) {
			return List.of();
		}

		List<AlertManageScreen.ReviewRow> reviewRows = new ArrayList<>(captures.size());
		for (int i = 0; i < captures.size(); i++) {
			TrainingDataService.CapturedChat capture = captures.get(i);
			if (capture == null || capture.rawMessage() == null || capture.rawMessage().isBlank()) {
				continue;
			}
			String rowId = "recent-"
				+ capture.timestampMs()
				+ "-"
				+ capture.speakerKey()
				+ "-"
				+ Integer.toHexString(capture.rawMessage().hashCode());
			reviewRows.add(new AlertManageScreen.ReviewRow(rowId, capture.rawMessage(), -1, capture.modScore()));
		}
		return reviewRows;
	}

	private static int toReviewScore(eu.tango.scamscreener.pipeline.model.DetectionResult result) {
		if (result == null) {
			return 0;
		}
		int rounded = (int) Math.round(result.totalScore());
		return Math.max(0, Math.min(100, rounded));
	}

	private static boolean shouldAutoMarkScamInReview(eu.tango.scamscreener.pipeline.model.DetectionResult result) {
		if (result == null || result.level() == null) {
			return false;
		}
		return shouldAutoMarkScamInReview(DetectionScoring.toScamRiskLevel(result.level()));
	}

	private static boolean shouldAutoMarkScamInReview(ScamRules.ScamRiskLevel riskLevel) {
		if (riskLevel == null) {
			return false;
		}
		ScamRules.ScamRiskLevel minimum = autoCaptureMinimumRiskLevel();
		if (minimum == null) {
			return false;
		}
		return riskLevel.ordinal() >= minimum.ordinal();
	}

	private static ScamRules.ScamRiskLevel autoCaptureMinimumRiskLevel() {
		String setting = ScamRules.autoCaptureAlertLevelSetting();
		if (setting == null || setting.isBlank()) {
			return null;
		}
		String normalized = setting.trim().toUpperCase(Locale.ROOT);
		if ("OFF".equals(normalized)) {
			return null;
		}
		try {
			return ScamRules.ScamRiskLevel.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private TrainingCsvReviewLoadResult loadTrainingCsvReviewRows() {
		List<TrainingCommandHandler.TrainingCsvReviewRow> rows;
		try {
			rows = trainingCommandHandler.loadTrainingCsvForReview();
		} catch (IOException e) {
			return TrainingCsvReviewLoadResult.failed(safeErrorMessage(e));
		}
		if (rows.isEmpty()) {
			return TrainingCsvReviewLoadResult.success(List.of());
		}

		List<AlertManageScreen.ReviewRow> reviewRows = new ArrayList<>(rows.size());
		for (TrainingCommandHandler.TrainingCsvReviewRow row : rows) {
			if (row == null) {
				continue;
			}
			reviewRows.add(new AlertManageScreen.ReviewRow(row.rowId(), row.message(), row.currentLabel()));
		}
		return TrainingCsvReviewLoadResult.success(reviewRows);
	}

	private static String safeErrorMessage(Throwable error) {
		if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
			return "Unknown error.";
		}
		return error.getMessage();
	}

	private record TrainingCsvReviewLoadResult(List<AlertManageScreen.ReviewRow> rows, String failureDetail) {
		private static TrainingCsvReviewLoadResult success(List<AlertManageScreen.ReviewRow> rows) {
			return new TrainingCsvReviewLoadResult(rows == null ? List.of() : rows, null);
		}

		private static TrainingCsvReviewLoadResult failed(String failureDetail) {
			return new TrainingCsvReviewLoadResult(List.of(), failureDetail == null || failureDetail.isBlank() ? "Unknown error." : failureDetail);
		}
	}


}
