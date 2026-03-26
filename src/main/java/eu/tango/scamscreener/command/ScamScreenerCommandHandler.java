package eu.tango.scamscreener.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.gui.ScamScreenerScreens;
import eu.tango.scamscreener.gui.screen.AlertInfoScreen;
import eu.tango.scamscreener.gui.screen.AlertManageScreen;
import eu.tango.scamscreener.gui.screen.BlacklistScreen;
import eu.tango.scamscreener.gui.screen.DebugSettingsScreen;
import eu.tango.scamscreener.gui.screen.MessageSettingsScreen;
import eu.tango.scamscreener.gui.screen.MetricsSettingsScreen;
import eu.tango.scamscreener.gui.screen.ReviewScreen;
import eu.tango.scamscreener.gui.screen.RulesSettingsScreen;
import eu.tango.scamscreener.gui.screen.RuntimeSettingsScreen;
import eu.tango.scamscreener.gui.screen.WhitelistScreen;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.WhitelistEntry;
import eu.tango.scamscreener.message.AlertContextRegistry;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.debug.DebugKeys;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import eu.tango.scamscreener.profiler.web.ProfilerWebOpenResult;
import eu.tango.scamscreener.profiler.web.ProfilerWebService;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Registers the local client-side ScamScreener commands.
 */
public final class ScamScreenerCommandHandler {
    private static final String DEFAULT_BLACKLIST_REASON = "Manual command";

    private static boolean initialized;
    private static Runnable pendingUiAction;

    private ScamScreenerCommandHandler() {
    }

    /**
     * Registers the local client-side commands once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildRootCommand("scamscreener"));
            dispatcher.register(buildRootCommand("ss"));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> runPendingUiAction());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRootCommand(String literalName) {
        return literal(literalName)
            .executes(context -> openRoot(context.getSource()))
            .then(literal("open").executes(context -> openRoot(context.getSource())))
            .then(literal("enable").executes(context -> setScamScreenerEnabled(context.getSource(), true)))
            .then(literal("disable").executes(context -> setScamScreenerEnabled(context.getSource(), false)))
            .then(buildWhitelistCommand())
            .then(buildBlacklistCommand())
            .then(buildReviewCommand())
            .then(buildAlertLevelCommand())
            .then(buildAutoLeaveCommand())
            .then(buildMuteCommand())
            .then(buildUnmuteCommand())
            .then(buildDebugCommand())
            .then(literal("metrics").executes(context -> openMetrics(context.getSource())))
            .then(buildProfilerCommand())
            .then(literal("version").executes(context -> showVersion(context.getSource())))
            .then(literal("rules").executes(context -> openRules(context.getSource())))
            .then(literal("runtime").executes(context -> openRuntime(context.getSource())))
            .then(literal("settings").executes(context -> openSettings(context.getSource())))
            .then(literal("messages").executes(context -> openMessages(context.getSource())))
            .then(literal("help").executes(context -> showHelp(context.getSource())));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildReviewCommand() {
        return literal("review")
            .executes(context -> openReview(context.getSource()))
            .then(literal("export").executes(context -> exportTrainingCases(context.getSource())))
            .then(literal("manage")
                .then(argument("alertId", StringArgumentType.word())
                    .executes(context -> openReviewManage(context.getSource(), StringArgumentType.getString(context, "alertId")))))
            .then(literal("info")
                .then(argument("alertId", StringArgumentType.word())
                    .executes(context -> openReviewInfo(context.getSource(), StringArgumentType.getString(context, "alertId")))))
            .then(literal("player")
                .then(argument("playerName", StringArgumentType.word())
                    .suggests((context, builder) -> suggestReviewPlayers(builder))
                    .executes(context -> openReviewPlayer(context.getSource(), StringArgumentType.getString(context, "playerName")))))
            .then(argument("playerName", StringArgumentType.word())
                .suggests((context, builder) -> suggestReviewPlayers(builder))
                .executes(context -> openReviewPlayer(context.getSource(), StringArgumentType.getString(context, "playerName"))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAlertLevelCommand() {
        return literal("alertlevel")
            .executes(context -> showAlertLevel(context.getSource()))
            .then(argument("level", StringArgumentType.word())
                .suggests((context, builder) -> suggestAlertLevels(builder))
                .executes(context -> setAlertLevel(context.getSource(), StringArgumentType.getString(context, "level"))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildAutoLeaveCommand() {
        return literal("autoleave")
            .executes(context -> showAutoLeave(context.getSource()))
            .then(literal("on").executes(context -> setAutoLeave(context.getSource(), true)))
            .then(literal("off").executes(context -> setAutoLeave(context.getSource(), false)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildMuteCommand() {
        return literal("mute")
            .executes(context -> enableMuteFilter(context.getSource()))
            .then(argument("pattern", StringArgumentType.greedyString())
                .executes(context -> addMutePattern(context.getSource(), StringArgumentType.getString(context, "pattern"))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildUnmuteCommand() {
        return literal("unmute")
            .executes(context -> disableMuteFilter(context.getSource()))
            .then(argument("pattern", StringArgumentType.greedyString())
                .suggests((context, builder) -> suggestMutePatterns(builder))
                .executes(context -> removeMutePattern(context.getSource(), StringArgumentType.getString(context, "pattern"))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildDebugCommand() {
        return literal("debug")
            .executes(context -> showDebugStatus(context.getSource()))
            .then(argument("enabled", BoolArgumentType.bool())
                .executes(context -> setAllDebug(context.getSource(), BoolArgumentType.getBool(context, "enabled")))
                .then(argument("debug", StringArgumentType.word())
                    .suggests((context, builder) -> suggestDebugKeys(builder))
                    .executes(context -> setDebugKey(
                        context.getSource(),
                        StringArgumentType.getString(context, "debug"),
                        BoolArgumentType.getBool(context, "enabled")
                    ))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildProfilerCommand() {
        return literal("profiler")
            .executes(context -> showProfilerStatus(context.getSource()))
            .then(literal("on").executes(context -> setProfilerHud(context.getSource(), true)))
            .then(literal("off").executes(context -> setProfilerHud(context.getSource(), false)))
            .then(literal("open").executes(context -> openProfilerWeb(context.getSource())));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildWhitelistCommand() {
        return literal("whitelist")
            .executes(context -> openWhitelist(context.getSource()))
            .then(literal("add")
                .then(argument("target", StringArgumentType.word())
                    .executes(context -> addWhitelist(context.getSource(), readTarget(context)))))
            .then(literal("remove")
                .then(argument("target", StringArgumentType.word())
                    .suggests((context, builder) -> suggestWhitelistEntries(builder))
                    .executes(context -> removeWhitelist(context.getSource(), readTarget(context)))))
            .then(literal("clear").executes(context -> clearWhitelist(context.getSource())));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildBlacklistCommand() {
        return literal("blacklist")
            .executes(context -> openBlacklist(context.getSource()))
            .then(literal("add")
                .then(argument("target", StringArgumentType.word())
                    .executes(context -> addBlacklist(context.getSource(), readTarget(context), 100, DEFAULT_BLACKLIST_REASON))
                    .then(argument("score", IntegerArgumentType.integer(0))
                        .executes(context -> addBlacklist(
                            context.getSource(),
                            readTarget(context),
                            IntegerArgumentType.getInteger(context, "score"),
                            DEFAULT_BLACKLIST_REASON
                        ))
                        .then(argument("reason", StringArgumentType.greedyString())
                            .executes(context -> addBlacklist(
                                context.getSource(),
                                readTarget(context),
                                IntegerArgumentType.getInteger(context, "score"),
                                StringArgumentType.getString(context, "reason")
                            ))))
                    .then(argument("reason", StringArgumentType.greedyString())
                        .executes(context -> addBlacklist(
                            context.getSource(),
                            readTarget(context),
                            100,
                            StringArgumentType.getString(context, "reason")
                        )))))
            .then(literal("remove")
                .then(argument("target", StringArgumentType.word())
                    .suggests((context, builder) -> suggestBlacklistEntries(builder))
                    .executes(context -> removeBlacklist(context.getSource(), readTarget(context)))))
            .then(literal("clear").executes(context -> clearBlacklist(context.getSource())));
    }

    private static int openRoot(FabricClientCommandSource source) {
        return queueScreen(source, () -> ScamScreenerScreens.openRoot(null));
    }

    private static int openWhitelist(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new WhitelistScreen(null)));
    }

    private static int openBlacklist(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new BlacklistScreen(null)));
    }

    private static int openReview(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new ReviewScreen(null)));
    }

    private static int openReviewManage(FabricClientCommandSource source, String alertId) {
        AlertContextRegistry.AlertContext context = AlertContextRegistry.find(alertId).orElse(null);
        if (context == null) {
            source.sendError(ClientMessages.alertContextMissing());
            return 0;
        }

        return queueScreen(source, () -> source.getClient().setScreen(new AlertManageScreen(null, context)));
    }

    private static int openReviewInfo(FabricClientCommandSource source, String alertId) {
        AlertContextRegistry.AlertContext context = AlertContextRegistry.find(alertId).orElse(null);
        if (context == null) {
            source.sendError(ClientMessages.alertContextMissing());
            return 0;
        }

        return queueScreen(source, () -> source.getClient().setScreen(new AlertInfoScreen(null, context)));
    }

    private static int openReviewPlayer(FabricClientCommandSource source, String playerName) {
        AlertContextRegistry.AlertContext context = AlertContextRegistry.createPlayerReviewContext(playerName).orElse(null);
        if (context == null) {
            source.sendError(ClientMessages.alertContextMissing());
            return 0;
        }

        return queueScreen(source, () -> source.getClient().setScreen(new AlertManageScreen(null, context)));
    }

    private static int openRules(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new RulesSettingsScreen(null)));
    }

    private static int openRuntime(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new RuntimeSettingsScreen(null)));
    }

    private static int openSettings(FabricClientCommandSource source) {
        return queueScreen(source, () -> ScamScreenerScreens.openRoot(null));
    }

    private static int openMessages(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new MessageSettingsScreen(null)));
    }

    private static int openMetrics(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new MetricsSettingsScreen(null)));
    }

    private static int showHelp(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.commandHelp());
        return 1;
    }

    private static int setScamScreenerEnabled(FabricClientCommandSource source, boolean enabled) {
        ScamScreenerRuntime.getInstance().setEnabled(enabled);
        source.sendFeedback(enabled ? ClientMessages.scamScreenerEnabled() : ClientMessages.scamScreenerDisabled());
        return 1;
    }

    private static int showAlertLevel(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.currentAlertLevel(ScamScreenerRuntime.getInstance().config().alerts().minimumRiskLevel()));
        return 1;
    }

    private static int setAlertLevel(FabricClientCommandSource source, String value) {
        AlertRiskLevel level = parseAlertLevel(value);
        if (level == null) {
            source.sendError(ClientMessages.invalidAlertLevel());
            return 0;
        }

        ScamScreenerRuntime.getInstance().config().alerts().setMinimumRiskLevel(level);
        ScamScreenerRuntime.getInstance().saveConfig();
        source.sendFeedback(ClientMessages.updatedAlertLevel(level));
        return 1;
    }

    private static int showAutoLeave(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.autoLeaveStatus(ScamScreenerRuntime.getInstance().config().safety().isAutoLeaveOnBlacklist()));
        return 1;
    }

    private static int setAutoLeave(FabricClientCommandSource source, boolean enabled) {
        ScamScreenerRuntime.getInstance().config().safety().setAutoLeaveOnBlacklist(enabled);
        ScamScreenerRuntime.getInstance().saveConfig();
        source.sendFeedback(enabled ? ClientMessages.autoLeaveEnabled() : ClientMessages.autoLeaveDisabled());
        return 1;
    }

    private static int enableMuteFilter(FabricClientCommandSource source) {
        ScamScreenerRuntime.getInstance().mutePatternManager().setEnabled(true);
        source.sendFeedback(ClientMessages.muteEnabled());
        return 1;
    }

    private static int disableMuteFilter(FabricClientCommandSource source) {
        ScamScreenerRuntime.getInstance().mutePatternManager().setEnabled(false);
        source.sendFeedback(ClientMessages.muteDisabled());
        return 1;
    }

    private static int addMutePattern(FabricClientCommandSource source, String pattern) {
        var result = ScamScreenerRuntime.getInstance().mutePatternManager().addPattern(pattern);
        if (result == eu.tango.scamscreener.chat.mute.MutePatternManager.AddResult.ADDED) {
            source.sendFeedback(ClientMessages.mutePatternAdded(pattern));
            return 1;
        }
        if (result == eu.tango.scamscreener.chat.mute.MutePatternManager.AddResult.ALREADY_EXISTS) {
            source.sendError(ClientMessages.mutePatternAlreadyExists(pattern));
            return 0;
        }

        source.sendError(ClientMessages.mutePatternInvalid(pattern));
        return 0;
    }

    private static int removeMutePattern(FabricClientCommandSource source, String pattern) {
        boolean removed = ScamScreenerRuntime.getInstance().mutePatternManager().removePattern(pattern);
        if (!removed) {
            source.sendError(ClientMessages.mutePatternNotFound(pattern));
            return 0;
        }

        source.sendFeedback(ClientMessages.mutePatternRemoved(pattern));
        return 1;
    }

    private static int showDebugStatus(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.debugStatus(debugStates()));
        return 1;
    }

    private static int showProfilerStatus(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.profilerStatus(ScamScreenerRuntime.getInstance().config().profiler().isHudEnabled()));
        return 1;
    }

    private static int setProfilerHud(FabricClientCommandSource source, boolean enabled) {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.config().profiler().setHudEnabled(enabled);
        runtime.saveConfig();
        ScamScreenerProfiler.getInstance().onEnabledStateChanged(enabled);
        source.sendFeedback(enabled ? ClientMessages.profilerEnabled() : ClientMessages.profilerDisabled());
        return 1;
    }

    private static int openProfilerWeb(FabricClientCommandSource source) {
        ProfilerWebOpenResult result = ProfilerWebService.getInstance().open();
        if (result.isMissingDependency()) {
            source.sendError(ClientMessages.profilerWebMissingDependency(ProfilerWebService.DOWNLOAD_URL));
            return 0;
        }
        if (!result.isSuccess()) {
            source.sendError(ClientMessages.profilerWebUnavailable(result.detail()));
            return 0;
        }

        try {
            Util.getPlatform().openUri(result.uri().toString());
            source.sendFeedback(ClientMessages.profilerWebOpened(result.uri().toString()));
            return 1;
        } catch (Exception exception) {
            ScamScreenerMod.LOGGER.warn("Could not open profiler web view.", exception);
            source.sendError(ClientMessages.profilerWebOpenFailed(exception.getMessage()));
            return 0;
        }
    }

    private static int setAllDebug(FabricClientCommandSource source, boolean enabled) {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.config().debug().flags().clear();
        for (String key : DebugKeys.keys()) {
            runtime.config().debug().flags().put(key, enabled);
        }
        runtime.config().output().setDebugLogging(enabled);
        runtime.saveConfig();
        source.sendFeedback(ClientMessages.debugUpdated("all " + (enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int setDebugKey(FabricClientCommandSource source, String key, boolean enabled) {
        String normalizedKey = DebugKeys.normalize(key);
        if (normalizedKey.isBlank() || !DebugKeys.keys().contains(normalizedKey)) {
            source.sendError(ClientMessages.debugKeyUnknown(key));
            return 0;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        runtime.config().debug().flags().put(normalizedKey, enabled);
        runtime.config().output().setDebugLogging(allDebugEnabled());
        runtime.saveConfig();
        source.sendFeedback(ClientMessages.debugUpdated(DebugKeys.label(normalizedKey) + " " + (enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int showVersion(FabricClientCommandSource source) {
        source.sendFeedback(ClientMessages.versionInfo());
        return 1;
    }

    private static int exportTrainingCases(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (client == null) {
            source.sendError(ClientMessages.uiUnavailable());
            return 0;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        source.sendFeedback(ClientMessages.trainingCasesExportStarted());
        runtime.trainingCaseExportService()
            .exportReviewedCasesAsync(runtime.reviewStore().entries())
            .whenComplete((result, throwable) -> client.execute(() -> {
                if (throwable != null) {
                    source.sendError(ClientMessages.trainingCasesExportFailed(rootCauseMessage(throwable)));
                    return;
                }

                source.sendFeedback(ClientMessages.trainingCasesExported(result));
            }));
        return 1;
    }

    private static int queueScreen(FabricClientCommandSource source, Runnable action) {
        Minecraft client = source.getClient();
        if (client == null) {
            source.sendError(ClientMessages.uiUnavailable());
            return 0;
        }

        queueUiAction(action);
        return 1;
    }

    private static synchronized void queueUiAction(Runnable action) {
        pendingUiAction = action;
    }

    private static void runPendingUiAction() {
        Runnable actionToRun;
        synchronized (ScamScreenerCommandHandler.class) {
            actionToRun = pendingUiAction;
            pendingUiAction = null;
        }

        if (actionToRun != null) {
            try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("command.pending_ui", "Pending UI Action")) {
                actionToRun.run();
            }
        }
    }

    private static int addWhitelist(FabricClientCommandSource source, PlayerTarget target) {
        WhitelistAccess whitelist = ScamScreenerRuntime.getInstance().whitelist();
        boolean stored = whitelist.add(target.playerUuid(), target.playerName());
        if (!stored) {
            source.sendError(ClientMessages.whitelistUpdateFailed());
            return 0;
        }

        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.whitelistUpdated(target.displayValue()));
        return 1;
    }

    private static int removeWhitelist(FabricClientCommandSource source, PlayerTarget target) {
        WhitelistAccess whitelist = ScamScreenerRuntime.getInstance().whitelist();
        boolean removed = removeFromWhitelist(whitelist, target);
        if (!removed) {
            source.sendError(ClientMessages.whitelistEntryMissing(target.displayValue()));
            return 0;
        }

        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.whitelistRemoved(target.displayValue()));
        return 1;
    }

    private static int clearWhitelist(FabricClientCommandSource source) {
        ScamScreenerRuntime.getInstance().whitelist().clear();
        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.whitelistCleared());
        return 1;
    }

    private static int addBlacklist(FabricClientCommandSource source, PlayerTarget target, int score, String reason) {
        BlacklistAccess blacklist = ScamScreenerRuntime.getInstance().blacklist();
        boolean stored = blacklist.add(target.playerUuid(), target.playerName(), score, normalizeReason(reason));
        if (!stored) {
            source.sendError(ClientMessages.blacklistUpdateFailed());
            return 0;
        }

        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.blacklistUpdated(target.displayValue(), score));
        return 1;
    }

    private static int removeBlacklist(FabricClientCommandSource source, PlayerTarget target) {
        BlacklistAccess blacklist = ScamScreenerRuntime.getInstance().blacklist();
        boolean removed = removeFromBlacklist(blacklist, target);
        if (!removed) {
            source.sendError(ClientMessages.blacklistEntryMissing(target.displayValue()));
            return 0;
        }

        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.blacklistRemoved(target.displayValue()));
        return 1;
    }

    private static int clearBlacklist(FabricClientCommandSource source) {
        ScamScreenerRuntime.getInstance().blacklist().clear();
        refreshCommandCompletions();
        source.sendFeedback(ClientMessages.blacklistCleared());
        return 1;
    }

    private static boolean removeFromWhitelist(WhitelistAccess whitelist, PlayerTarget target) {
        if (target.playerUuid() != null && whitelist.remove(target.playerUuid())) {
            return true;
        }

        return !target.playerName().isBlank() && whitelist.removeByName(target.playerName());
    }

    private static boolean removeFromBlacklist(BlacklistAccess blacklist, PlayerTarget target) {
        if (target.playerUuid() != null && blacklist.remove(target.playerUuid())) {
            return true;
        }

        return !target.playerName().isBlank() && blacklist.removeByName(target.playerName());
    }

    private static PlayerTarget readTarget(CommandContext<FabricClientCommandSource> context) {
        return parseTarget(StringArgumentType.getString(context, "target"));
    }

    private static PlayerTarget parseTarget(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new PlayerTarget(null, "");
        }

        String trimmedValue = rawValue.trim();
        try {
            return new PlayerTarget(UUID.fromString(trimmedValue), "");
        } catch (IllegalArgumentException ignored) {
            return new PlayerTarget(null, trimmedValue);
        }
    }

    private static String normalizeReason(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEFAULT_BLACKLIST_REASON;
        }

        String normalizedValue = rawValue.trim();
        return normalizedValue.isEmpty() ? DEFAULT_BLACKLIST_REASON : normalizedValue;
    }

    private static void refreshCommandCompletions() {
        try {
            ClientCommands.refreshCommandCompletions();
        } catch (IllegalStateException ignored) {
            // Command suggestions are optional; ignore refresh failures when the dispatcher is unavailable.
        }
    }

    private static CompletableFuture<Suggestions> suggestWhitelistEntries(SuggestionsBuilder builder) {
        for (WhitelistEntry entry : ScamScreenerRuntime.getInstance().whitelist().allEntries()) {
            suggestValue(builder, entry.playerName());
            if (entry.playerUuid() != null) {
                suggestValue(builder, entry.playerUuid().toString());
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBlacklistEntries(SuggestionsBuilder builder) {
        for (BlacklistEntry entry : ScamScreenerRuntime.getInstance().blacklist().allEntries()) {
            suggestValue(builder, entry.playerName());
            if (entry.playerUuid() != null) {
                suggestValue(builder, entry.playerUuid().toString());
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestReviewPlayers(SuggestionsBuilder builder) {
        for (String playerName : AlertContextRegistry.recentPlayerNames()) {
            suggestValue(builder, playerName);
        }
        for (var entry : ScamScreenerRuntime.getInstance().reviewStore().entries()) {
            if (entry != null) {
                suggestValue(builder, entry.getSenderName());
            }
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAlertLevels(SuggestionsBuilder builder) {
        for (AlertRiskLevel level : AlertRiskLevel.values()) {
            suggestValue(builder, level.name().toLowerCase(Locale.ROOT));
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMutePatterns(SuggestionsBuilder builder) {
        for (String pattern : ScamScreenerRuntime.getInstance().mutePatternManager().allPatterns()) {
            suggestValue(builder, pattern);
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestDebugKeys(SuggestionsBuilder builder) {
        for (String key : DebugKeys.keys()) {
            suggestValue(builder, key);
        }

        return builder.buildFuture();
    }

    private static void suggestValue(SuggestionsBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String remaining = builder.getRemainingLowerCase();
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        if (remaining.isBlank() || normalizedValue.startsWith(remaining)) {
            builder.suggest(value);
        }
    }

    private static AlertRiskLevel parseAlertLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AlertRiskLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static java.util.Map<String, Boolean> debugStates() {
        return DebugKeys.withDefaults(ScamScreenerRuntime.getInstance().config().debug().flags());
    }

    private static boolean allDebugEnabled() {
        for (boolean enabled : debugStates().values()) {
            if (!enabled) {
                return false;
            }
        }

        return true;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause instanceof CompletionException && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause == null ? null : rootCause.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }

    private record PlayerTarget(UUID playerUuid, String playerName) {
        private PlayerTarget {
            playerName = playerName == null ? "" : playerName.trim();
        }

        private String displayValue() {
            if (playerUuid != null) {
                return playerUuid.toString();
            }

            return playerName.isBlank() ? "<unknown>" : playerName;
        }
    }
}
