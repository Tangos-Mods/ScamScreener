package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.detection.MutePatternManager;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

final class UnmuteCommand {
	private UnmuteCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		MutePatternManager mutePatternManager,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("unmute")
			.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
				.suggests((context, builder) -> {
					String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
					for (String pattern : mutePatternManager.allPatterns()) {
						if (pattern.toLowerCase(Locale.ROOT).startsWith(remaining)) {
							builder.suggest(pattern);
						}
					}
					return builder.buildFuture();
				})
				.executes(context -> {
					String pattern = StringArgumentType.getString(context, "pattern");
					boolean removed = mutePatternManager.removePattern(pattern);
					reply.accept(removed
						? Messages.mutePatternRemoved(pattern)
						: Messages.mutePatternNotFound(pattern));
					return removed ? 1 : 0;
				}));
	}
}
