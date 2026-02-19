package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class WhitelistCommand {
	private WhitelistCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		WhitelistManager whitelist,
		Function<String, ResolvedTarget> targetResolver,
		Runnable openWhitelistScreenHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("whitelist")
			.executes(context -> {
				if (openWhitelistScreenHandler != null) {
					openWhitelistScreenHandler.run();
					return 1;
				}
				reply.accept(Messages.whitelistCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.literal("add")
				.executes(context -> {
					reply.accept(Messages.whitelistCommandHelp());
					return 1;
				})
				.then(ClientCommandManager.argument("player", StringArgumentType.word())
					.executes(context -> {
						if (whitelist == null || targetResolver == null) {
							reply.accept(Messages.whitelistCommandHelp());
							return 0;
						}
						ResolvedTarget target = targetResolver.apply(StringArgumentType.getString(context, "player"));
						if (target == null) {
							return 0;
						}

						WhitelistManager.AddOrUpdateResult result = whitelist.addOrUpdate(target.uuid(), target.name());
						if (result == WhitelistManager.AddOrUpdateResult.ADDED) {
							reply.accept(Messages.addedToWhitelist(target.name(), target.uuid()));
							return 1;
						}
						if (result == WhitelistManager.AddOrUpdateResult.UPDATED) {
							reply.accept(Messages.updatedWhitelistEntry(target.name(), target.uuid()));
							return 1;
						}
						if (result == WhitelistManager.AddOrUpdateResult.UNCHANGED) {
							reply.accept(Messages.alreadyWhitelisted(target.name(), target.uuid()));
							return 1;
						}
						reply.accept(Messages.whitelistCommandHelp());
						return 0;
					})))
			.then(ClientCommandManager.literal("remove")
				.executes(context -> {
					reply.accept(Messages.whitelistCommandHelp());
					return 1;
				})
				.then(ClientCommandManager.argument("player", StringArgumentType.word())
					.suggests((context, builder) -> suggestWhitelistedPlayers(whitelist, builder))
					.executes(context -> {
						if (whitelist == null) {
							reply.accept(Messages.whitelistCommandHelp());
							return 0;
						}
						String player = StringArgumentType.getString(context, "player");
						WhitelistManager.WhitelistEntry removed = whitelist.removeByName(player);
						if (removed == null) {
							reply.accept(Messages.notOnWhitelist(player));
							return 1;
						}
						reply.accept(Messages.removedFromWhitelist(removed.name(), removed.uuid()));
						return 1;
					})))
			.then(ClientCommandManager.literal("list")
				.executes(context -> {
					if (whitelist == null) {
						reply.accept(Messages.whitelistCommandHelp());
						return 0;
					}
					if (whitelist.isEmpty()) {
						reply.accept(Messages.whitelistEmpty());
						return 1;
					}
					reply.accept(Messages.whitelistHeader());
					for (WhitelistManager.WhitelistEntry entry : whitelist.allEntries()) {
						reply.accept(Messages.whitelistEntry(entry));
					}
					return 1;
				}));
	}

	private static CompletableFuture<Suggestions> suggestWhitelistedPlayers(WhitelistManager whitelist, SuggestionsBuilder builder) {
		if (whitelist == null) {
			return builder.buildFuture();
		}
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (WhitelistManager.WhitelistEntry entry : whitelist.allEntries()) {
			String name = entry.name();
			if (name != null && !name.isBlank() && name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	}
}
