package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class ListCommand {
	private ListCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		BlacklistManager blacklist,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("list")
			.executes(context -> {
				if (blacklist.isEmpty()) {
					reply.accept(Messages.blacklistEmpty());
					return 1;
				}

				reply.accept(Messages.blacklistHeader());
				for (BlacklistManager.ScamEntry entry : blacklist.allEntries()) {
					reply.accept(Messages.blacklistEntry(entry));
				}
				return 1;
			});
	}
}
