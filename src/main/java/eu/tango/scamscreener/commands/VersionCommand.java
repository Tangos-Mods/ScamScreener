package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.VersionInfo;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class VersionCommand {
	private VersionCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(Consumer<Component> reply) {
		return ClientCommandManager.literal("version")
			.executes(context -> {
				reply.accept(Messages.versionInfo(VersionInfo.modVersion(), VersionInfo.aiModelVersion()));
				return 1;
			});
	}
}
