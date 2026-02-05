package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
	private static final int SCAM_LABEL = 1;
	private static final int LEGIT_LABEL = 0;

	private AiCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler,
		ScamScreenerCommands.CaptureByMessageHandler captureByMessageHandler,
		ScamScreenerCommands.CaptureBulkHandler captureBulkHandler,
		ScamScreenerCommands.MigrateTrainingHandler migrateTrainingHandler,
		ScamScreenerCommands.ModelUpdateHandler modelUpdateHandler,
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
				.then(withCaptureLabel(
					ClientCommandManager.literal("scam"),
					captureByPlayerHandler,
					SCAM_LABEL,
					false
				))
				.then(withCaptureLabel(
					ClientCommandManager.literal("legit"),
					captureByPlayerHandler,
					LEGIT_LABEL,
					false
				)));

		LiteralArgumentBuilder<FabricClientCommandSource> flag = ClientCommandManager.literal("flag")
			.then(ClientCommandManager.argument("messageId", StringArgumentType.word())
				.then(ClientCommandManager.literal("legit").executes(context ->
					captureByMessageHandler.capture(StringArgumentType.getString(context, "messageId"), LEGIT_LABEL)))
				.then(ClientCommandManager.literal("scam").executes(context ->
					captureByMessageHandler.capture(StringArgumentType.getString(context, "messageId"), SCAM_LABEL))));

		LiteralArgumentBuilder<FabricClientCommandSource> captureBulk = ClientCommandManager.literal("capturebulk")
			.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
				.executes(context -> captureBulkHandler.capture(IntegerArgumentType.getInteger(context, "count"))));

		LiteralArgumentBuilder<FabricClientCommandSource> migrate = ClientCommandManager.literal("migrate")
			.executes(context -> migrateTrainingHandler.migrate());

		LiteralArgumentBuilder<FabricClientCommandSource> model = ClientCommandManager.literal("model")
			.then(ClientCommandManager.literal("download")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> modelUpdateHandler.handle("download", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("accept")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> modelUpdateHandler.handle("accept", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("merge")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> modelUpdateHandler.handle("merge", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("ignore")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> modelUpdateHandler.handle("ignore", StringArgumentType.getString(context, "id")))));

		return ClientCommandManager.literal("ai")
			.executes(context -> {
				reply.accept(Messages.aiCommandHelp());
				return 1;
			})
			.then(capture)
			.then(captureBulk)
			.then(flag)
			.then(migrate)
			.then(model)
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

	static LiteralArgumentBuilder<FabricClientCommandSource> buildCaptureAlias(
		String alias,
		int label,
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler
	) {
		return ClientCommandManager.literal(alias)
			.then(withCapturePlayerArg(captureByPlayerHandler, label, true));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> withCaptureLabel(
		LiteralArgumentBuilder<FabricClientCommandSource> root,
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		boolean requireCount
	) {
		if (!requireCount) {
			root.executes(context -> runCapture(context, captureByPlayerHandler, label, 1));
		}
		return root.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
			.executes(context -> runCapture(
				context,
				captureByPlayerHandler,
				label,
				IntegerArgumentType.getInteger(context, "count")
			)));
	}

	private static RequiredArgumentBuilder<FabricClientCommandSource, String> withCapturePlayerArg(
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		boolean requireCount
	) {
		RequiredArgumentBuilder<FabricClientCommandSource, String> playerArg =
			ClientCommandManager.argument("player", StringArgumentType.word());
		if (!requireCount) {
			playerArg.executes(context -> runCapture(context, captureByPlayerHandler, label, 1));
		}
		return playerArg.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
			.executes(context -> runCapture(
				context,
				captureByPlayerHandler,
				label,
				IntegerArgumentType.getInteger(context, "count")
			)));
	}

	private static int runCapture(
		CommandContext<FabricClientCommandSource> context,
		ScamScreenerCommands.CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		int count
	) {
		return captureByPlayerHandler.capture(
			StringArgumentType.getString(context, "player"),
			label,
			count
		);
	}
}
