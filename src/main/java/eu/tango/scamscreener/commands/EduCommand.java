package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.EducationMessages;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

final class EduCommand {
	private EduCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.EducationDisableHandler disableHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("edu")
			.executes(context -> {
				reply.accept(Messages.educationCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.literal("disable")
				.then(ClientCommandManager.argument("messageId", StringArgumentType.word())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
						for (String option : EducationMessages.knownMessageIds()) {
							if (option.startsWith(remaining)) {
								builder.suggest(option);
							}
						}
						return builder.buildFuture();
					})
					.executes(context -> disableHandler.disable(StringArgumentType.getString(context, "messageId")))));
	}
}
