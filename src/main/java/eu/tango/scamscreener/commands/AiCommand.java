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
