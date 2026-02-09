package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class AutoLeaveCommand {
	private AutoLeaveCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		BooleanSupplier enabledSupplier,
		Consumer<Boolean> setEnabledHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("autoleave")
			.executes(context -> {
				reply.accept(Messages.autoLeaveStatus(enabledSupplier.getAsBoolean()));
				return 1;
			})
			.then(ClientCommandManager.literal("on")
				.executes(context -> {
					setEnabledHandler.accept(true);
					reply.accept(Messages.autoLeaveEnabled());
					return 1;
				}))
			.then(ClientCommandManager.literal("off")
				.executes(context -> {
					setEnabledHandler.accept(false);
					reply.accept(Messages.autoLeaveDisabled());
					return 1;
				}));
	}
}
