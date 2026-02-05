package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class VersionCommand {
	private VersionCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(Consumer<Component> reply) {
		return ClientCommandManager.literal("version")
			.executes(context -> {
				reply.accept(Messages.versionInfo(readModVersion(), readAiModelVersion()));
				return 1;
			});
	}

	private static String readModVersion() {
		return FabricLoader.getInstance()
			.getModContainer("scam-screener")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	private static int readAiModelVersion() {
		return LocalAiModelConfig.loadOrCreate().version;
	}
}
