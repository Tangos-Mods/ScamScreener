package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

final class AddCommand {
	private AddCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		BlacklistManager blacklist,
		Function<String, ResolvedTarget> targetResolver,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("add")
			.executes(context -> {
				reply.accept(Messages.addCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.executes(context -> {
					ResolvedTarget target = targetResolver.apply(StringArgumentType.getString(context, "player"));
					if (target == null) {
						return 0;
					}

					boolean added = blacklist.add(target.uuid(), target.name(), 50, "manual-entry");
					reply.accept(added
						? Messages.addedToBlacklist(target.name(), target.uuid())
						: Messages.alreadyBlacklisted(target.name(), target.uuid()));
					return 1;
				})
				.then(ClientCommandManager.argument("score", IntegerArgumentType.integer(0, 100))
					.executes(context -> {
						ResolvedTarget target = targetResolver.apply(StringArgumentType.getString(context, "player"));
						if (target == null) {
							return 0;
						}

						int score = IntegerArgumentType.getInteger(context, "score");
						boolean added = blacklist.add(target.uuid(), target.name(), score, "manual-entry");
						reply.accept(added
							? Messages.addedToBlacklistWithScore(target.name(), target.uuid(), score)
							: Messages.alreadyBlacklisted(target.name(), target.uuid()));
						return 1;
					})
					.then(ClientCommandManager.argument("reason", StringArgumentType.greedyString())
						.executes(context -> {
							ResolvedTarget target = targetResolver.apply(StringArgumentType.getString(context, "player"));
							if (target == null) {
								return 0;
							}

							int score = IntegerArgumentType.getInteger(context, "score");
							String reason = StringArgumentType.getString(context, "reason");
							boolean added = blacklist.add(target.uuid(), target.name(), score, reason);
							reply.accept(added
								? Messages.addedToBlacklistWithMetadata(target.name(), target.uuid())
								: Messages.alreadyBlacklisted(target.name(), target.uuid()));
							return 1;
						}))));
	}
}
