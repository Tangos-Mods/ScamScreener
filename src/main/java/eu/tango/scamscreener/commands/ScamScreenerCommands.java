package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ScamScreenerCommands {
	private final BlacklistManager blacklist;
	private final Function<String, ResolvedTarget> targetResolver;
	private final MutePatternManager mutePatternManager;
	private final CaptureByMessageHandler captureByMessageHandler;
	private final MigrateTrainingHandler migrateTrainingHandler;
	private final ModelUpdateHandler modelUpdateHandler;
	private final UpdateCheckHandler updateCheckHandler;
	private final EmailBypassHandler emailBypassHandler;
	private final java.util.function.Consumer<Boolean> setAllDebugHandler;
	private final java.util.function.BiConsumer<String, Boolean> setDebugKeyHandler;
	private final java.util.function.Supplier<java.util.Map<String, Boolean>> debugStateSupplier;
	private final BooleanSupplier autoLeaveEnabledSupplier;
	private final Consumer<Boolean> setAutoLeaveEnabledHandler;
	private final IntSupplier trainHandler;
	private final IntSupplier resetAiHandler;
	private final IntSupplier funnelMetricsStatusHandler;
	private final IntSupplier funnelMetricsResetHandler;
	private final Supplier<String> lastCapturedChatSupplier;
	private final AlertReviewManageHandler alertReviewManageHandler;
	private final AlertReviewInfoHandler alertReviewInfoHandler;
	private final AlertReviewPlayerHandler alertReviewPlayerHandler;
	private final EducationDisableHandler educationDisableHandler;
	private final Consumer<UUID> onBlacklistRemoved;
	private final Runnable openSettingsHandler;
	private final Consumer<Component> reply;

	public ScamScreenerCommands(
		BlacklistManager blacklist,
		Function<String, ResolvedTarget> targetResolver,
		MutePatternManager mutePatternManager,
		CaptureByMessageHandler captureByMessageHandler,
		MigrateTrainingHandler migrateTrainingHandler,
		ModelUpdateHandler modelUpdateHandler,
		UpdateCheckHandler updateCheckHandler,
		EmailBypassHandler emailBypassHandler,
		java.util.function.Consumer<Boolean> setAllDebugHandler,
		java.util.function.BiConsumer<String, Boolean> setDebugKeyHandler,
		java.util.function.Supplier<java.util.Map<String, Boolean>> debugStateSupplier,
		BooleanSupplier autoLeaveEnabledSupplier,
		Consumer<Boolean> setAutoLeaveEnabledHandler,
		IntSupplier trainHandler,
		IntSupplier resetAiHandler,
		IntSupplier funnelMetricsStatusHandler,
		IntSupplier funnelMetricsResetHandler,
		Supplier<String> lastCapturedChatSupplier,
		AlertReviewManageHandler alertReviewManageHandler,
		AlertReviewInfoHandler alertReviewInfoHandler,
		AlertReviewPlayerHandler alertReviewPlayerHandler,
		EducationDisableHandler educationDisableHandler,
		Consumer<UUID> onBlacklistRemoved,
		Runnable openSettingsHandler,
		Consumer<Component> reply
	) {
		this.blacklist = blacklist;
		this.targetResolver = targetResolver;
		this.mutePatternManager = mutePatternManager;
		this.captureByMessageHandler = captureByMessageHandler;
		this.migrateTrainingHandler = migrateTrainingHandler;
		this.modelUpdateHandler = modelUpdateHandler;
		this.updateCheckHandler = updateCheckHandler;
		this.emailBypassHandler = emailBypassHandler;
		this.setAllDebugHandler = setAllDebugHandler;
		this.setDebugKeyHandler = setDebugKeyHandler;
		this.debugStateSupplier = debugStateSupplier;
		this.autoLeaveEnabledSupplier = autoLeaveEnabledSupplier;
		this.setAutoLeaveEnabledHandler = setAutoLeaveEnabledHandler;
		this.trainHandler = trainHandler;
		this.resetAiHandler = resetAiHandler;
		this.funnelMetricsStatusHandler = funnelMetricsStatusHandler;
		this.funnelMetricsResetHandler = funnelMetricsResetHandler;
		this.lastCapturedChatSupplier = lastCapturedChatSupplier;
		this.alertReviewManageHandler = alertReviewManageHandler;
		this.alertReviewInfoHandler = alertReviewInfoHandler;
		this.alertReviewPlayerHandler = alertReviewPlayerHandler;
		this.educationDisableHandler = educationDisableHandler;
		this.onBlacklistRemoved = onBlacklistRemoved;
		this.openSettingsHandler = openSettingsHandler;
		this.reply = reply;
	}

	public void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(buildRoot("scamscreener"));
		});
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildRoot(String rootName) {
		return ClientCommandManager.literal(rootName)
			.executes(context -> {
				reply.accept(Messages.commandHelp());
				return 1;
			})
			.then(AddCommand.build(blacklist, targetResolver, reply))
			.then(RemoveCommand.build(blacklist, targetResolver, onBlacklistRemoved, reply))
			.then(ListCommand.build(blacklist, reply))
			.then(MuteCommand.build(mutePatternManager, reply))
			.then(UnmuteCommand.build(mutePatternManager, reply))
			.then(EmailBypassCommand.build(emailBypassHandler, reply))
			.then(AiCommand.build(
				captureByMessageHandler,
				migrateTrainingHandler,
				modelUpdateHandler,
				updateCheckHandler,
				resetAiHandler,
				funnelMetricsStatusHandler,
				funnelMetricsResetHandler,
				reply
			))
			.then(ClientCommandManager.literal("upload").executes(context -> trainHandler.getAsInt()))
			.then(ReviewCommand.build(alertReviewManageHandler, alertReviewInfoHandler, alertReviewPlayerHandler, reply))
			.then(EduCommand.build(educationDisableHandler, reply))
			.then(RuleCommand.build(reply))
			.then(AlertLevelCommand.build(reply))
			.then(AutoLeaveCommand.build(autoLeaveEnabledSupplier, setAutoLeaveEnabledHandler, reply))
			.then(SettingsCommand.build(openSettingsHandler))
			.then(DebugCommand.build(setAllDebugHandler, setDebugKeyHandler, debugStateSupplier, reply))
			.then(VersionCommand.build(reply))
			.then(PreviewCommand.build(reply, lastCapturedChatSupplier));
	}

	@FunctionalInterface
	public interface CaptureByMessageHandler {
		int capture(String messageId, int label);
	}

	@FunctionalInterface
	public interface MigrateTrainingHandler {
		int migrate();
	}

	@FunctionalInterface
	public interface ModelUpdateHandler {
		int handle(String action, String id);
	}

	@FunctionalInterface
	public interface UpdateCheckHandler {
		int check(boolean force);
	}

	@FunctionalInterface
	public interface EmailBypassHandler {
		int bypass(String id);
	}

	@FunctionalInterface
	public interface AlertReviewManageHandler {
		int open(String alertId);
	}

	@FunctionalInterface
	public interface AlertReviewInfoHandler {
		int open(String alertId);
	}

	@FunctionalInterface
	public interface AlertReviewPlayerHandler {
		int open(String playerName);
	}

	@FunctionalInterface
	public interface EducationDisableHandler {
		int disable(String messageId);
	}
}
