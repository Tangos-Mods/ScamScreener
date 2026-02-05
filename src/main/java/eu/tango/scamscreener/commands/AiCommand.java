package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Consumer;

final class AiCommand {
	private AiCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler,
		IntSupplier trainHandler,
		IntSupplier resetAiHandler,
		Consumer<Component> reply
	) {
		LiteralArgumentBuilder<FabricClientCommandSource> capture = ClientCommandManager.literal("capture")
			.executes(context -> {
				reply.accept(Messages.aiCaptureCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.then(ClientCommandManager.literal("scam")
					.executes(context -> captureByPlayerHandler.capture(
						StringArgumentType.getString(context, "player"),
						1,
						1
					))
					.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
						.executes(context -> captureByPlayerHandler.capture(
							StringArgumentType.getString(context, "player"),
							1,
							IntegerArgumentType.getInteger(context, "count")
						))))
				.then(ClientCommandManager.literal("legit")
					.executes(context -> captureByPlayerHandler.capture(
						StringArgumentType.getString(context, "player"),
						0,
						1
					))
					.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
						.executes(context -> captureByPlayerHandler.capture(
							StringArgumentType.getString(context, "player"),
							0,
							IntegerArgumentType.getInteger(context, "count")
						)))));

		return ClientCommandManager.literal("ai")
			.executes(context -> {
				reply.accept(Messages.aiCommandHelp());
				return 1;
			})
			.then(capture)
			.then(ClientCommandManager.literal("train").executes(context -> trainHandler.getAsInt()))
			.then(ClientCommandManager.literal("reset").executes(context -> resetAiHandler.getAsInt()))
			.then(ClientCommandManager.literal("autocapture")
				.executes(context -> {
					reply.accept(Messages.currentAutoCaptureAlertLevel(ScamRules.autoCaptureAlertLevelSetting()));
					return 1;
				})
				.then(ClientCommandManager.argument("level", StringArgumentType.word())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
						for (String option : new String[]{"off", "low", "medium", "high", "critical"}) {
							if (option.startsWith(remaining)) {
								builder.suggest(option);
							}
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						String input = StringArgumentType.getString(context, "level");
						String updated = ScamRules.setAutoCaptureAlertLevelSetting(input);
						if (updated == null) {
							reply.accept(Messages.invalidAutoCaptureAlertLevel());
							return 0;
						}
						reply.accept(Messages.updatedAutoCaptureAlertLevel(updated));
						return 1;
					})));
	}
}
