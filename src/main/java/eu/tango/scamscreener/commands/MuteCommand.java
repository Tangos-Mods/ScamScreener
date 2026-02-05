package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.detection.MutePatternManager;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class MuteCommand {
	private MuteCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		MutePatternManager mutePatternManager,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("mute")
			.executes(context -> {
				reply.accept(Messages.mutePatternList(mutePatternManager.allPatterns()));
				return 1;
			})
			.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
				.executes(context -> {
					String pattern = StringArgumentType.getString(context, "pattern");
					MutePatternManager.AddResult result = mutePatternManager.addPattern(pattern);
					if (result == MutePatternManager.AddResult.ADDED) {
						reply.accept(Messages.mutePatternAdded(pattern));
						return 1;
					}
					if (result == MutePatternManager.AddResult.ALREADY_EXISTS) {
						reply.accept(Messages.mutePatternAlreadyExists(pattern));
						return 0;
					}
					reply.accept(Messages.mutePatternInvalid(pattern));
					return 0;
				}));
	}
}
