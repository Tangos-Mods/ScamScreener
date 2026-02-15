package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class ReviewCommand {
	private ReviewCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.AlertReviewManageHandler manageHandler,
		ScamScreenerCommands.AlertReviewInfoHandler infoHandler,
		ScamScreenerCommands.AlertReviewPlayerHandler playerHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("review")
			.executes(context -> {
				reply.accept(Messages.reviewCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.literal("manage")
				.then(ClientCommandManager.argument("alertId", StringArgumentType.word())
					.executes(context -> manageHandler.open(StringArgumentType.getString(context, "alertId")))))
			.then(ClientCommandManager.literal("info")
				.then(ClientCommandManager.argument("alertId", StringArgumentType.word())
					.executes(context -> infoHandler.open(StringArgumentType.getString(context, "alertId")))))
			.then(ClientCommandManager.literal("player")
				.then(ClientCommandManager.argument("playerName", StringArgumentType.word())
					.executes(context -> playerHandler.open(StringArgumentType.getString(context, "playerName")))));
	}
}
