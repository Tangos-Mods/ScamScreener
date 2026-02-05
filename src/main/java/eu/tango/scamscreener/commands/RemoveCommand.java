package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class RemoveCommand {
	private RemoveCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		BlacklistManager blacklist,
		Function<String, ResolvedTarget> targetResolver,
		Consumer<UUID> onBlacklistRemoved,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("remove")
			.executes(context -> {
				reply.accept(Messages.removeCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.suggests((context, builder) -> suggestBlacklistedPlayers(blacklist, builder))
				.executes(context -> {
					ResolvedTarget target = targetResolver.apply(StringArgumentType.getString(context, "player"));
					if (target == null) {
						return 0;
					}

					boolean removed = blacklist.remove(target.uuid());
					reply.accept(removed
						? Messages.removedFromBlacklist(target.name(), target.uuid())
						: Messages.notOnBlacklist(target.name(), target.uuid()));
					onBlacklistRemoved.accept(target.uuid());
					return 1;
				}));
	}

	private static CompletableFuture<Suggestions> suggestBlacklistedPlayers(BlacklistManager blacklist, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (BlacklistManager.ScamEntry entry : blacklist.allEntries()) {
			String name = entry.name();
			if (name != null && !name.isBlank() && name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	}
}
