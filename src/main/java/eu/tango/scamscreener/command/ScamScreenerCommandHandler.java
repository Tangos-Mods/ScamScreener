package eu.tango.scamscreener.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.gui.ScamScreenerScreens;
import eu.tango.scamscreener.gui.screen.BlacklistScreen;
import eu.tango.scamscreener.gui.screen.MessageSettingsScreen;
import eu.tango.scamscreener.gui.screen.ReviewScreen;
import eu.tango.scamscreener.gui.screen.RulesSettingsScreen;
import eu.tango.scamscreener.gui.screen.RuntimeSettingsScreen;
import eu.tango.scamscreener.gui.screen.WhitelistScreen;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.WhitelistEntry;
import eu.tango.scamscreener.message.ClientMessages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

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
            .then(buildWhitelistCommand())
            .then(buildBlacklistCommand())
            .then(literal("review").executes(context -> openReview(context.getSource())))
            .then(literal("rules").executes(context -> openRules(context.getSource())))
            .then(literal("settings").executes(context -> openSettings(context.getSource())))
            .then(literal("messages").executes(context -> openMessages(context.getSource())));
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

    private static int openRules(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new RulesSettingsScreen(null)));
    }

    private static int openSettings(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new RuntimeSettingsScreen(null)));
    }

    private static int openMessages(FabricClientCommandSource source) {
        return queueScreen(source, () -> source.getClient().setScreen(new MessageSettingsScreen(null)));
    }

    private static int queueScreen(FabricClientCommandSource source, Runnable action) {
        MinecraftClient client = source.getClient();
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
            actionToRun.run();
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
            ClientCommandManager.refreshCommandCompletions();
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
