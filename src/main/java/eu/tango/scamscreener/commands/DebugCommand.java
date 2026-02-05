package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.DebugMessages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

final class DebugCommand {
	private DebugCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		IntConsumer updaterDebugHandler,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("debug")
			.then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
				.executes(context -> {
					boolean enabled = BoolArgumentType.getBool(context, "enabled");
					updaterDebugHandler.accept(enabled ? 1 : 0);
					reply.accept(DebugMessages.updater("debug " + (enabled ? "enabled" : "disabled")));
					return 1;
				}));
	}
}
