package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class AiCommand {
	private static final int SCAM_LABEL = 1;
	private static final int LEGIT_LABEL = 0;

	private AiCommand() {
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> modelAction(
		String action,
		ScamScreenerCommands.ModelUpdateHandler modelUpdateHandler
	) {
		return ClientCommandManager.literal(action)
			.then(ClientCommandManager.argument("id", StringArgumentType.word())
				.executes(context -> modelUpdateHandler.handle(action, StringArgumentType.getString(context, "id"))));
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		ScamScreenerCommands.CaptureByMessageHandler captureByMessageHandler,
		ScamScreenerCommands.MigrateTrainingHandler migrateTrainingHandler,
		ScamScreenerCommands.ModelUpdateHandler modelUpdateHandler,
		ScamScreenerCommands.UpdateCheckHandler updateCheckHandler,
		IntSupplier resetAiHandler,
		IntSupplier funnelMetricsStatusHandler,
		IntSupplier funnelMetricsResetHandler,
		Consumer<Component> reply
	) {
		LiteralArgumentBuilder<FabricClientCommandSource> flag = ClientCommandManager.literal("flag")
			.then(ClientCommandManager.argument("messageId", StringArgumentType.word())
				.then(ClientCommandManager.literal("legit").executes(context ->
					captureByMessageHandler.capture(StringArgumentType.getString(context, "messageId"), LEGIT_LABEL)))
				.then(ClientCommandManager.literal("scam").executes(context ->
					captureByMessageHandler.capture(StringArgumentType.getString(context, "messageId"), SCAM_LABEL))));

		LiteralArgumentBuilder<FabricClientCommandSource> migrate = ClientCommandManager.literal("migrate")
			.executes(context -> migrateTrainingHandler.migrate());

		LiteralArgumentBuilder<FabricClientCommandSource> update = ClientCommandManager.literal("update")
			.executes(context -> updateCheckHandler.check(false))
			.then(ClientCommandManager.literal("force").executes(context -> updateCheckHandler.check(true)))
			.then(ClientCommandManager.literal("notify")
				.executes(context -> {
					reply.accept(Messages.aiUpdateJoinNotifyStatus(ScamRules.notifyAiUpToDateOnJoin()));
					return 1;
				})
				.then(ClientCommandManager.literal("on")
					.executes(context -> {
						ScamRules.setNotifyAiUpToDateOnJoin(true);
						reply.accept(Messages.aiUpdateJoinNotifyEnabled());
						return 1;
					}))
				.then(ClientCommandManager.literal("off")
					.executes(context -> {
						ScamRules.setNotifyAiUpToDateOnJoin(false);
						reply.accept(Messages.aiUpdateJoinNotifyDisabled());
						return 1;
					})));

		LiteralArgumentBuilder<FabricClientCommandSource> metrics = ClientCommandManager.literal("metrics")
			.executes(context -> funnelMetricsStatusHandler.getAsInt())
			.then(ClientCommandManager.literal("reset")
				.executes(context -> funnelMetricsResetHandler.getAsInt()));

		return ClientCommandManager.literal("model")
			.executes(context -> {
				reply.accept(Messages.aiCommandHelp());
				return 1;
			})
			.then(flag)
			.then(migrate)
			.then(update)
			.then(modelAction("download", modelUpdateHandler))
			.then(modelAction("accept", modelUpdateHandler))
			.then(modelAction("merge", modelUpdateHandler))
			.then(modelAction("ignore", modelUpdateHandler))
			.then(metrics)
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
							// Code: AUTO-CAPTURE-001
							reply.accept(Messages.invalidAutoCaptureAlertLevel());
							return 0;
						}
						reply.accept(Messages.updatedAutoCaptureAlertLevel(updated));
						return 1;
					})));
	}
}
