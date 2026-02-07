package eu.tango.scamscreener.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.lookup.ResolvedTarget;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.DebugRegistry;
import eu.tango.scamscreener.ui.UiPreview;
import eu.tango.scamscreener.ui.messages.BlacklistMessages;
import eu.tango.scamscreener.ui.messages.CommandMessages;
import eu.tango.scamscreener.ui.messages.DebugMessages;
import eu.tango.scamscreener.ui.messages.ScreenMessages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class ScamScreenerCommands {
	private static final List<String> DEBUG_KEYS = DebugRegistry.keys();
	private static final int SCAM_LABEL = 1;
	private static final int LEGIT_LABEL = 0;
	private static final List<String> AUTO_CAPTURE_LEVELS = List.of("off", "low", "medium", "high", "critical");

	private final CoreHandlers core;
	private final AiHandlers ai;
	private final DebugHandlers debug;
	private final ToIntFunction<String> emailBypassHandler;
	private final Consumer<Component> reply;

	public ScamScreenerCommands(
		CoreHandlers core,
		AiHandlers ai,
		ToIntFunction<String> emailBypassHandler,
		DebugHandlers debug,
		Consumer<Component> reply
	) {
		this.core = core;
		this.ai = ai;
		this.emailBypassHandler = emailBypassHandler;
		this.debug = debug;
		this.reply = reply;
	}

	public void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(buildRoot("scamscreener"));
			dispatcher.register(buildCaptureAlias("1", 1, ai.captureByPlayerHandler()));
			dispatcher.register(buildCaptureAlias("0", 0, ai.captureByPlayerHandler()));
		});
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildRoot(String rootName) {
		return ClientCommandManager.literal(rootName)
			.executes(context -> {
				reply.accept(CommandMessages.commandHelp());
				return 1;
			})
			.then(buildAddCommand())
			.then(buildRemoveCommand())
			.then(buildListCommand())
			.then(buildMuteCommand())
			.then(buildUnmuteCommand())
			.then(buildBypassCommand())
			.then(buildAiCommand())
			.then(buildRuleCommand())
			.then(buildAlertLevelCommand())
			.then(buildScreenCommand())
			.then(buildDebugCommand())
			.then(buildVersionCommand())
			.then(buildPreviewCommand());
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildAiCommand() {
		LiteralArgumentBuilder<FabricClientCommandSource> capture = ClientCommandManager.literal("capture")
			.executes(context -> {
				reply.accept(CommandMessages.aiCaptureCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.then(withCaptureLabel(
					ClientCommandManager.literal("scam"),
					ai.captureByPlayerHandler(),
					SCAM_LABEL,
					false
				))
				.then(withCaptureLabel(
					ClientCommandManager.literal("legit"),
					ai.captureByPlayerHandler(),
					LEGIT_LABEL,
					false
				)));

		LiteralArgumentBuilder<FabricClientCommandSource> flag = ClientCommandManager.literal("flag")
			.then(ClientCommandManager.argument("messageId", StringArgumentType.word())
				.then(ClientCommandManager.literal("legit").executes(context ->
					ai.captureByMessageHandler().apply(StringArgumentType.getString(context, "messageId"), LEGIT_LABEL)))
				.then(ClientCommandManager.literal("scam").executes(context ->
					ai.captureByMessageHandler().apply(StringArgumentType.getString(context, "messageId"), SCAM_LABEL))));

		LiteralArgumentBuilder<FabricClientCommandSource> captureBulk = ClientCommandManager.literal("capturebulk")
			.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
				.executes(context -> ai.captureBulkHandler().applyAsInt(IntegerArgumentType.getInteger(context, "count"))));

		LiteralArgumentBuilder<FabricClientCommandSource> migrate = ClientCommandManager.literal("migrate")
			.executes(context -> ai.migrateTrainingHandler().getAsInt());

		LiteralArgumentBuilder<FabricClientCommandSource> model = ClientCommandManager.literal("model")
			.then(ClientCommandManager.literal("download")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> ai.modelUpdateHandler().apply("download", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("accept")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> ai.modelUpdateHandler().apply("accept", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("merge")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> ai.modelUpdateHandler().apply("merge", StringArgumentType.getString(context, "id")))))
			.then(ClientCommandManager.literal("ignore")
				.then(ClientCommandManager.argument("id", StringArgumentType.word())
					.executes(context -> ai.modelUpdateHandler().apply("ignore", StringArgumentType.getString(context, "id")))));

		LiteralArgumentBuilder<FabricClientCommandSource> update = ClientCommandManager.literal("update")
			.executes(context -> ai.updateCheckHandler().apply(false))
			.then(ClientCommandManager.literal("force").executes(context -> ai.updateCheckHandler().apply(true)));

		return ClientCommandManager.literal("ai")
			.executes(context -> {
				reply.accept(CommandMessages.aiCommandHelp());
				return 1;
			})
			.then(capture)
			.then(captureBulk)
			.then(flag)
			.then(migrate)
			.then(model)
			.then(update)
			.then(ClientCommandManager.literal("train").executes(context -> ai.trainHandler().getAsInt()))
			.then(ClientCommandManager.literal("reset").executes(context -> ai.resetAiHandler().getAsInt()))
			.then(ClientCommandManager.literal("autocapture")
				.executes(context -> {
					reply.accept(CommandMessages.currentAutoCaptureAlertLevel(ScamRules.autoCaptureAlertLevelSetting()));
					return 1;
				})
				.then(ClientCommandManager.argument("level", StringArgumentType.word())
					.suggests((context, builder) -> suggestStrings(builder, AUTO_CAPTURE_LEVELS))
					.executes(context -> {
						String input = StringArgumentType.getString(context, "level");
						String updated = ScamRules.setAutoCaptureAlertLevelSetting(input);
						if (updated == null) {
							// Code: AI-CAPTURE-001
							reply.accept(CommandMessages.invalidAutoCaptureAlertLevel());
							return 0;
						}
						reply.accept(CommandMessages.updatedAutoCaptureAlertLevel(updated));
						return 1;
					})));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCaptureAlias(
		String alias,
		int label,
		CaptureByPlayerHandler captureByPlayerHandler
	) {
		return ClientCommandManager.literal(alias)
			.then(withCapturePlayerArg(captureByPlayerHandler, label, true));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> withCaptureLabel(
		LiteralArgumentBuilder<FabricClientCommandSource> root,
		CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		boolean requireCount
	) {
		if (!requireCount) {
			root.executes(context -> runCapture(context, captureByPlayerHandler, label, 1));
		}
		return root.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
			.executes(context -> runCapture(
				context,
				captureByPlayerHandler,
				label,
				IntegerArgumentType.getInteger(context, "count")
			)));
	}

	private static RequiredArgumentBuilder<FabricClientCommandSource, String> withCapturePlayerArg(
		CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		boolean requireCount
	) {
		RequiredArgumentBuilder<FabricClientCommandSource, String> playerArg =
			ClientCommandManager.argument("player", StringArgumentType.word());
		if (!requireCount) {
			playerArg.executes(context -> runCapture(context, captureByPlayerHandler, label, 1));
		}
		return playerArg.then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, TrainingDataService.MAX_CAPTURED_CHAT_LINES))
			.executes(context -> runCapture(
				context,
				captureByPlayerHandler,
				label,
				IntegerArgumentType.getInteger(context, "count")
			)));
	}

	private static int runCapture(
		CommandContext<FabricClientCommandSource> context,
		CaptureByPlayerHandler captureByPlayerHandler,
		int label,
		int count
	) {
		return captureByPlayerHandler.capture(
			StringArgumentType.getString(context, "player"),
			label,
			count
		);
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildAddCommand() {
		return ClientCommandManager.literal("add")
			.executes(context -> {
				reply.accept(CommandMessages.addCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.executes(context -> {
					ResolvedTarget target = core.targetResolver().apply(StringArgumentType.getString(context, "player"));
					if (target == null) {
						return 0;
					}

					if (core.blacklist().contains(target.uuid())) {
						core.blacklist().update(target.uuid(), target.name(), 50, "manual-entry");
						reply.accept(BlacklistMessages.updatedBlacklistEntry(target.name(), 50, "manual-entry"));
						return 1;
					}
					boolean added = core.blacklist().add(target.uuid(), target.name(), 50, "manual-entry");
					reply.accept(added
						? BlacklistMessages.addedToBlacklist(target.name(), target.uuid())
						: BlacklistMessages.alreadyBlacklisted(target.name(), target.uuid()));
					return 1;
				})
				.then(ClientCommandManager.argument("score", IntegerArgumentType.integer(0, 100))
					.executes(context -> {
						ResolvedTarget target = core.targetResolver().apply(StringArgumentType.getString(context, "player"));
						if (target == null) {
							return 0;
						}

						int score = IntegerArgumentType.getInteger(context, "score");
						if (core.blacklist().contains(target.uuid())) {
							core.blacklist().update(target.uuid(), target.name(), score, "manual-entry");
							reply.accept(BlacklistMessages.updatedBlacklistEntry(target.name(), score, "manual-entry"));
							return 1;
						}
						boolean added = core.blacklist().add(target.uuid(), target.name(), score, "manual-entry");
						reply.accept(added
							? BlacklistMessages.addedToBlacklistWithScore(target.name(), target.uuid(), score)
							: BlacklistMessages.alreadyBlacklisted(target.name(), target.uuid()));
						return 1;
					})
					.then(ClientCommandManager.argument("reason", StringArgumentType.greedyString())
						.executes(context -> {
							ResolvedTarget target = core.targetResolver().apply(StringArgumentType.getString(context, "player"));
							if (target == null) {
								return 0;
							}

							int score = IntegerArgumentType.getInteger(context, "score");
							String reason = StringArgumentType.getString(context, "reason");
							if (core.blacklist().contains(target.uuid())) {
								core.blacklist().update(target.uuid(), target.name(), score, reason);
								reply.accept(BlacklistMessages.updatedBlacklistEntry(target.name(), score, reason));
								return 1;
							}
							boolean added = core.blacklist().add(target.uuid(), target.name(), score, reason);
							reply.accept(added
								? BlacklistMessages.addedToBlacklistWithMetadata(target.name(), target.uuid())
								: BlacklistMessages.alreadyBlacklisted(target.name(), target.uuid()));
							return 1;
						}))));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildRemoveCommand() {
		return ClientCommandManager.literal("remove")
			.executes(context -> {
				reply.accept(CommandMessages.removeCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.argument("player", StringArgumentType.word())
				.suggests((context, builder) -> suggestBlacklistedPlayers(builder))
				.executes(context -> {
					ResolvedTarget target = core.targetResolver().apply(StringArgumentType.getString(context, "player"));
					if (target == null) {
						return 0;
					}

					boolean removed = core.blacklist().remove(target.uuid());
					reply.accept(removed
						? BlacklistMessages.removedFromBlacklist(target.name(), target.uuid())
						: BlacklistMessages.notOnBlacklist(target.name(), target.uuid()));
					core.onBlacklistRemoved().accept(target.uuid());
					return 1;
				}));
	}

	private CompletableFuture<Suggestions> suggestBlacklistedPlayers(SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		for (BlacklistManager.ScamEntry entry : core.blacklist().allEntries()) {
			String name = entry.name();
			if (name != null && !name.isBlank()) {
				names.add(name);
			}
		}
		return suggestStrings(builder, names);
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildListCommand() {
		return ClientCommandManager.literal("list")
			.executes(context -> {
				if (core.blacklist().isEmpty()) {
					reply.accept(BlacklistMessages.blacklistEmpty());
					return 1;
				}

				reply.accept(BlacklistMessages.blacklistHeader());
				for (BlacklistManager.ScamEntry entry : core.blacklist().allEntries()) {
					reply.accept(BlacklistMessages.blacklistEntry(entry));
				}
				return 1;
			});
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildMuteCommand() {
		return ClientCommandManager.literal("mute")
			.executes(context -> {
				core.mutePatternManager().setEnabled(true);
				reply.accept(CommandMessages.muteEnabled());
				return 1;
			})
			.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
				.executes(context -> {
					String pattern = StringArgumentType.getString(context, "pattern");
					MutePatternManager.AddResult result = core.mutePatternManager().addPattern(pattern);
					if (result == MutePatternManager.AddResult.ADDED) {
						reply.accept(CommandMessages.mutePatternAdded(pattern));
						return 1;
					}
					if (result == MutePatternManager.AddResult.ALREADY_EXISTS) {
						reply.accept(CommandMessages.mutePatternAlreadyExists(pattern));
						return 0;
					}
					// Code: MUTE-REGEX-001
					reply.accept(CommandMessages.mutePatternInvalid(pattern));
					return 0;
				}));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildUnmuteCommand() {
		return ClientCommandManager.literal("unmute")
			.executes(context -> {
				core.mutePatternManager().setEnabled(false);
				reply.accept(CommandMessages.muteDisabled());
				return 1;
			})
			.then(ClientCommandManager.argument("pattern", StringArgumentType.greedyString())
				.suggests((context, builder) -> suggestStrings(builder, core.mutePatternManager().allPatterns()))
				.executes(context -> {
					String pattern = StringArgumentType.getString(context, "pattern");
					boolean removed = core.mutePatternManager().removePattern(pattern);
					reply.accept(removed
						? CommandMessages.mutePatternRemoved(pattern)
						// Code: MUTE-LOOKUP-001
						: CommandMessages.mutePatternNotFound(pattern));
					return removed ? 1 : 0;
				}));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildBypassCommand() {
		return ClientCommandManager.literal("bypass")
			.then(ClientCommandManager.argument("id", StringArgumentType.word())
				.executes(context -> {
					String id = StringArgumentType.getString(context, "id");
					if (emailBypassHandler == null) {
						reply.accept(CommandMessages.emailBypassExpired());
						return 0;
					}
					return emailBypassHandler.applyAsInt(id);
				}));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildRuleCommand() {
		return ClientCommandManager.literal("rules")
			.executes(context -> {
				reply.accept(CommandMessages.ruleCommandHelp());
				return 1;
			})
			.then(ClientCommandManager.literal("list")
				.executes(context -> {
					reply.accept(CommandMessages.disabledRulesList(ScamRules.disabledRules()));
					return 1;
				}))
			.then(ClientCommandManager.literal("disable")
				.then(ClientCommandManager.argument("rule", StringArgumentType.word())
					.suggests((context, builder) -> suggestEnumsLowerCase(builder, ScamRules.ScamRule.values()))
					.executes(context -> {
						ScamRules.ScamRule rule = parseRule(StringArgumentType.getString(context, "rule"));
						if (rule == null) {
							// Code: RULE-NAME-001
							reply.accept(CommandMessages.invalidRuleName());
							return 0;
						}
						boolean changed = ScamRules.disableRule(rule);
						reply.accept(changed ? CommandMessages.ruleDisabled(rule) : CommandMessages.ruleAlreadyDisabled(rule));
						return 1;
					})))
			.then(ClientCommandManager.literal("enable")
				.then(ClientCommandManager.argument("rule", StringArgumentType.word())
					.suggests((context, builder) -> suggestDisabledRules(builder))
					.executes(context -> {
						ScamRules.ScamRule rule = parseRule(StringArgumentType.getString(context, "rule"));
						if (rule == null) {
							// Code: RULE-NAME-001
							reply.accept(CommandMessages.invalidRuleName());
							return 0;
						}
						boolean changed = ScamRules.enableRule(rule);
						reply.accept(changed ? CommandMessages.ruleEnabled(rule) : CommandMessages.ruleNotDisabled(rule));
						return 1;
					})));
	}

	private CompletableFuture<Suggestions> suggestDisabledRules(SuggestionsBuilder builder) {
		Set<ScamRules.ScamRule> disabled = ScamRules.disabledRules();
		List<String> options = new ArrayList<>(disabled.size());
		for (ScamRules.ScamRule rule : disabled) {
			options.add(rule.name().toLowerCase(Locale.ROOT));
		}
		return suggestStrings(builder, options);
	}

	private ScamRules.ScamRule parseRule(String raw) {
		return parseEnumIgnoreCase(ScamRules.ScamRule.class, raw);
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildAlertLevelCommand() {
		return ClientCommandManager.literal("alertlevel")
			.executes(context -> {
				reply.accept(CommandMessages.currentAlertRiskLevel(ScamRules.minimumAlertRiskLevel()));
				return 1;
			})
			.then(ClientCommandManager.argument("level", StringArgumentType.word())
				.suggests(this::suggestRiskLevels)
				.executes(context -> {
					String levelArg = StringArgumentType.getString(context, "level");
					ScamRules.ScamRiskLevel level = parseRiskLevel(levelArg);
					if (level == null) {
						reply.accept(Component.literal("[ScamScreener] Invalid level. Use: LOW, MEDIUM, HIGH, CRITICAL."));
						return 0;
					}
					ScamRules.ScamRiskLevel updated = ScamRules.setMinimumAlertRiskLevel(level);
					reply.accept(CommandMessages.updatedAlertRiskLevel(updated));
					return 1;
				}));
	}

	private CompletableFuture<Suggestions> suggestRiskLevels(
		com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context,
		SuggestionsBuilder builder
	) {
		return suggestEnumsLowerCase(builder, ScamRules.ScamRiskLevel.values());
	}

	private ScamRules.ScamRiskLevel parseRiskLevel(String value) {
		return parseEnumIgnoreCase(ScamRules.ScamRiskLevel.class, value);
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildScreenCommand() {
		return ClientCommandManager.literal("screen")
			.executes(context -> {
				boolean enabled = debug.screenEnabledSupplier().getAsBoolean();
				reply.accept(ScreenMessages.modeStatus(enabled));
				return 1;
			})
			.then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
				.executes(context -> {
					boolean enabled = BoolArgumentType.getBool(context, "enabled");
					debug.setScreenEnabledHandler().accept(enabled);
					reply.accept(ScreenMessages.modeStatus(enabled));
					return 1;
				}));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildDebugCommand() {
		return ClientCommandManager.literal("debug")
			.executes(context -> {
				reply.accept(DebugMessages.debugStatus(debug.debugStateSupplier().get()));
				return 1;
			})
			.then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
				.executes(context -> {
					boolean enabled = BoolArgumentType.getBool(context, "enabled");
					debug.setAllDebugHandler().accept(enabled);
					reply.accept(DebugMessages.debugStatus("all " + (enabled ? "enabled" : "disabled")));
					return 1;
				})
				.then(ClientCommandManager.argument("debug", StringArgumentType.word())
					.suggests((context, builder) -> suggestStrings(builder, DEBUG_KEYS))
					.executes(context -> {
						boolean enabled = BoolArgumentType.getBool(context, "enabled");
						String key = StringArgumentType.getString(context, "debug");
						debug.setDebugKeyHandler().accept(key, enabled);
						reply.accept(DebugMessages.debugStatus(key + " " + (enabled ? "enabled" : "disabled")));
						return 1;
					})));
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> buildVersionCommand() {
		return ClientCommandManager.literal("version")
			.executes(context -> {
				reply.accept(CommandMessages.versionInfo(readModVersion(), readAiModelVersion()));
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

	private LiteralArgumentBuilder<FabricClientCommandSource> buildPreviewCommand() {
		return ClientCommandManager.literal("preview")
			.executes(context -> runDryRun());
	}

	private int runDryRun() {
		reply.accept(Component.literal("[ScamScreener] Preview dry run started."));
		for (Component component : UiPreview.buildAll(ai.lastCapturedChatSupplier())) {
			reply.accept(component);
		}
		reply.accept(Component.literal("[ScamScreener] Preview dry run finished."));
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestStrings(SuggestionsBuilder builder, Iterable<String> options) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		if (options == null) {
			return builder.buildFuture();
		}
		for (String option : options) {
			if (option == null || option.isBlank()) {
				continue;
			}
			if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(option);
			}
		}
		return builder.buildFuture();
	}

	private static <E extends Enum<E>> CompletableFuture<Suggestions> suggestEnumsLowerCase(
		SuggestionsBuilder builder,
		E[] values
	) {
		String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
		if (values == null) {
			return builder.buildFuture();
		}
		for (E value : values) {
			if (value == null) {
				continue;
			}
			String option = value.name().toLowerCase(Locale.ROOT);
			if (option.startsWith(remaining)) {
				builder.suggest(option);
			}
		}
		return builder.buildFuture();
	}

	private static <E extends Enum<E>> E parseEnumIgnoreCase(Class<E> enumType, String raw) {
		if (enumType == null || raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	public record CoreHandlers(
		BlacklistManager blacklist,
		Function<String, ResolvedTarget> targetResolver,
		MutePatternManager mutePatternManager,
		Consumer<UUID> onBlacklistRemoved
	) {
	}

	public record AiHandlers(
		CaptureByPlayerHandler captureByPlayerHandler,
		BiFunction<String, Integer, Integer> captureByMessageHandler,
		IntUnaryOperator captureBulkHandler,
		IntSupplier migrateTrainingHandler,
		BiFunction<String, String, Integer> modelUpdateHandler,
		Function<Boolean, Integer> updateCheckHandler,
		IntSupplier trainHandler,
		IntSupplier resetAiHandler,
		Supplier<String> lastCapturedChatSupplier
	) {
	}

	public record DebugHandlers(
		Consumer<Boolean> setAllDebugHandler,
		BiConsumer<String, Boolean> setDebugKeyHandler,
		Supplier<Map<String, Boolean>> debugStateSupplier,
		BooleanSupplier screenEnabledSupplier,
		Consumer<Boolean> setScreenEnabledHandler
	) {
	}

	@FunctionalInterface
	public interface CaptureByPlayerHandler {
		int capture(String playerName, int label, int count);
	}
}
