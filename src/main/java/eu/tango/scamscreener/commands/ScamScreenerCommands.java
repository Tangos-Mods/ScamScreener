package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.detection.MutePatternManager;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class ScamScreenerCommands {
	private final BlacklistManager blacklist;
	private final Function<String, ResolvedTarget> targetResolver;
	private final MutePatternManager mutePatternManager;
	private final CaptureByPlayerHandler captureByPlayerHandler;
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
		this.trainHandler = trainHandler;
		this.resetAiHandler = resetAiHandler;
		this.lastCapturedChatSupplier = lastCapturedChatSupplier;
		this.onBlacklistRemoved = onBlacklistRemoved;
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
			.then(AiCommand.build(captureByPlayerHandler, trainHandler, resetAiHandler, reply))
			.then(RuleCommand.build(reply))
			.then(AlertLevelCommand.build(reply))
			.then(VersionCommand.build(reply))
			.then(PreviewCommand.build(reply, lastCapturedChatSupplier));
	}

	@FunctionalInterface
	public interface CaptureByPlayerHandler {
		int capture(String playerName, int label, int count);
	}
}
