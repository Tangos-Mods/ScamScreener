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
	private final CaptureByPlayerHandler captureByPlayerHandler;
	private final CaptureByMessageHandler captureByMessageHandler;
	private final CaptureBulkHandler captureBulkHandler;
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
	private final Supplier<String> lastCapturedChatSupplier;
	private final Consumer<UUID> onBlacklistRemoved;
	private final Consumer<Component> reply;

	public ScamScreenerCommands(
		BlacklistManager blacklist,
		Function<String, ResolvedTarget> targetResolver,
		MutePatternManager mutePatternManager,
		CaptureByPlayerHandler captureByPlayerHandler,
		CaptureByMessageHandler captureByMessageHandler,
		CaptureBulkHandler captureBulkHandler,
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
		Supplier<String> lastCapturedChatSupplier,
		Consumer<UUID> onBlacklistRemoved,
		Consumer<Component> reply
	) {
		this.blacklist = blacklist;
		this.targetResolver = targetResolver;
		this.mutePatternManager = mutePatternManager;
		this.captureByPlayerHandler = captureByPlayerHandler;
		this.captureByMessageHandler = captureByMessageHandler;
		this.captureBulkHandler = captureBulkHandler;
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
		this.lastCapturedChatSupplier = lastCapturedChatSupplier;
		this.onBlacklistRemoved = onBlacklistRemoved;
		this.reply = reply;
	}

	public void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(buildRoot("scamscreener"));
			dispatcher.register(AiCommand.buildCaptureAlias("1", 1, captureByPlayerHandler));
			dispatcher.register(AiCommand.buildCaptureAlias("0", 0, captureByPlayerHandler));
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
					captureByPlayerHandler,
					captureByMessageHandler,
					captureBulkHandler,
					migrateTrainingHandler,
					modelUpdateHandler,
					updateCheckHandler,
					trainHandler,
					resetAiHandler,
					reply
				))
				.then(RuleCommand.build(reply))
				.then(AlertLevelCommand.build(reply))
				.then(AutoLeaveCommand.build(autoLeaveEnabledSupplier, setAutoLeaveEnabledHandler, reply))
				.then(DebugCommand.build(setAllDebugHandler, setDebugKeyHandler, debugStateSupplier, reply))
			.then(VersionCommand.build(reply))
			.then(PreviewCommand.build(reply, lastCapturedChatSupplier));
	}

	@FunctionalInterface
	public interface CaptureByPlayerHandler {
		int capture(String playerName, int label, int count);
	}

	@FunctionalInterface
	public interface CaptureByMessageHandler {
		int capture(String messageId, int label);
	}

	@FunctionalInterface
	public interface CaptureBulkHandler {
		int capture(int count);
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
}
