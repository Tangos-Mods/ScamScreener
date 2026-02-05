package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.ui.DebugMessages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DebugCommand {
	private static final List<String> DEBUG_KEYS = List.of("updater", "party", "trade", "mute");

	private DebugCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		Consumer<Boolean> setAllHandler,
		BiConsumer<String, Boolean> setKeyHandler,
		Supplier<Map<String, Boolean>> debugStateSupplier,
		Consumer<Component> reply
	) {
		return ClientCommandManager.literal("debug")
			.executes(context -> {
				reply.accept(DebugMessages.debugStatus(debugStateSupplier.get()));
				return 1;
			})
			.then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
				.executes(context -> {
					boolean enabled = BoolArgumentType.getBool(context, "enabled");
					setAllHandler.accept(enabled);
					reply.accept(DebugMessages.debugStatus("all " + (enabled ? "enabled" : "disabled")));
					return 1;
				})
				.then(ClientCommandManager.argument("debug", StringArgumentType.word())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
						for (String key : DEBUG_KEYS) {
							if (key.startsWith(remaining)) {
								builder.suggest(key);
							}
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						boolean enabled = BoolArgumentType.getBool(context, "enabled");
						String key = StringArgumentType.getString(context, "debug");
						setKeyHandler.accept(key, enabled);
						reply.accept(DebugMessages.debugStatus(key + " " + (enabled ? "enabled" : "disabled")));
						return 1;
					})));
	}

}
