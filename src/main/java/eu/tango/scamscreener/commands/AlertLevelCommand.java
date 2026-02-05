package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class AlertLevelCommand {
	private AlertLevelCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(Consumer<Component> reply) {
		return ClientCommandManager.literal("alertlevel")
			.executes(context -> {
				reply.accept(Messages.currentAlertRiskLevel(ScamRules.minimumAlertRiskLevel()));
				return 1;
			})
			.then(ClientCommandManager.argument("level", StringArgumentType.word())
				.suggests(AlertLevelCommand::suggestRiskLevels)
				.executes(context -> {
					String levelArg = StringArgumentType.getString(context, "level");
					ScamRules.ScamRiskLevel level = parseRiskLevel(levelArg);
					if (level == null) {
						reply.accept(Component.literal("[ScamScreener] Invalid level. Use: LOW, MEDIUM, HIGH, CRITICAL."));
						return 0;
					}
					ScamRules.ScamRiskLevel updated = ScamRules.setMinimumAlertRiskLevel(level);
					reply.accept(Messages.updatedAlertRiskLevel(updated));
					return 1;
				}));
	}

	private static CompletableFuture<Suggestions> suggestRiskLevels(
		com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context,
		SuggestionsBuilder builder
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		for (ScamRules.ScamRiskLevel level : ScamRules.ScamRiskLevel.values()) {
			String value = level.name().toLowerCase(Locale.ROOT);
			if (value.startsWith(remaining)) {
				builder.suggest(value);
			}
		}
		return builder.buildFuture();
	}

	private static ScamRules.ScamRiskLevel parseRiskLevel(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return ScamRules.ScamRiskLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
