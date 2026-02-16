package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ReviewCommand {
	private ReviewCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.AlertReviewManageHandler manageHandler,
		ScamScreenerCommands.AlertReviewInfoHandler infoHandler,
		ScamScreenerCommands.AlertReviewPlayerHandler playerHandler,
		Supplier<List<String>> playerSuggestionsSupplier,
		ScamScreenerCommands.AlertReviewRecentChatHandler recentChatHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("review")
			.executes(context -> openRecentReviewOrHelp(recentChatHandler, reply))
			.then(ClientCommandManager.literal("help").executes(context -> {
				reply.accept(Messages.reviewCommandHelp());
				return 1;
			}))
			.then(ClientCommandManager.literal("manage")
				.then(ClientCommandManager.argument("alertId", StringArgumentType.word())
					.executes(context -> manageHandler.open(StringArgumentType.getString(context, "alertId")))))
			.then(ClientCommandManager.literal("info")
				.then(ClientCommandManager.argument("alertId", StringArgumentType.word())
					.executes(context -> infoHandler.open(StringArgumentType.getString(context, "alertId")))))
			.then(ClientCommandManager.literal("player")
				.then(ClientCommandManager.argument("playerName", StringArgumentType.word())
					.suggests((context, builder) -> suggestReviewPlayers(playerSuggestionsSupplier, builder))
					.executes(context -> playerHandler.open(StringArgumentType.getString(context, "playerName")))));
	}

	private static CompletableFuture<Suggestions> suggestReviewPlayers(
		Supplier<List<String>> playerSuggestionsSupplier,
		SuggestionsBuilder builder
	) {
		if (playerSuggestionsSupplier == null) {
			return builder.buildFuture();
		}
		List<String> candidates = playerSuggestionsSupplier.get();
		if (candidates == null || candidates.isEmpty()) {
			return builder.buildFuture();
		}
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (String candidate : candidates) {
			if (candidate == null || candidate.isBlank()) {
				continue;
			}
			String safeCandidate = candidate.trim();
			if (safeCandidate.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(safeCandidate);
			}
		}
		return builder.buildFuture();
	}

	private static int openRecentReviewOrHelp(ScamScreenerCommands.AlertReviewRecentChatHandler recentChatHandler, Consumer<Component> reply) {
		if (recentChatHandler == null) {
			reply.accept(Messages.reviewCommandHelp());
			return 1;
		}
		return recentChatHandler.open();
	}
}
