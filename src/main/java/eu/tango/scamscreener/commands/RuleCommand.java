package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

final class RuleCommand {
	private RuleCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(Consumer<Component> reply) {
		return ClientCommandManager.literal("rules")
			.executes(context -> {
				reply.accept(Messages.ruleCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.literal("list")
				.executes(context -> {
					reply.accept(Messages.disabledRulesList(ScamRules.disabledRules()));
					return 1;
				}))
			.then(ClientCommandManager.literal("disable")
				.then(ClientCommandManager.argument("rule", StringArgumentType.word())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
						for (ScamRules.ScamRule rule : ScamRules.allRules()) {
							String name = rule.name().toLowerCase(Locale.ROOT);
							if (name.startsWith(remaining)) {
								builder.suggest(name);
							}
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						ScamRules.ScamRule rule = parseRule(StringArgumentType.getString(context, "rule"));
						if (rule == null) {
							reply.accept(Messages.invalidRuleName());
							return 0;
						}
						boolean changed = ScamRules.disableRule(rule);
						reply.accept(changed ? Messages.ruleDisabled(rule) : Messages.ruleAlreadyDisabled(rule));
						return 1;
					})))
			.then(ClientCommandManager.literal("enable")
				.then(ClientCommandManager.argument("rule", StringArgumentType.word())
					.suggests((context, builder) -> {
						String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
						Set<ScamRules.ScamRule> disabled = ScamRules.disabledRules();
						for (ScamRules.ScamRule rule : disabled) {
							String name = rule.name().toLowerCase(Locale.ROOT);
							if (name.startsWith(remaining)) {
								builder.suggest(name);
							}
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						ScamRules.ScamRule rule = parseRule(StringArgumentType.getString(context, "rule"));
						if (rule == null) {
							reply.accept(Messages.invalidRuleName());
							return 0;
						}
						boolean changed = ScamRules.enableRule(rule);
						reply.accept(changed ? Messages.ruleEnabled(rule) : Messages.ruleNotDisabled(rule));
						return 1;
					})));
	}

	private static ScamRules.ScamRule parseRule(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return ScamRules.ScamRule.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
