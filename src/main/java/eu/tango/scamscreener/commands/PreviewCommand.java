package eu.tango.scamscreener.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class PreviewCommand {
	private PreviewCommand() {
	}

	static LiteralArgumentBuilder<FabricClientCommandSource> build(
		Consumer<Component> reply,
		Supplier<String> lastCapturedChatSupplier
	) {
		return ClientCommandManager.literal("preview")
			.executes(context -> runDryRun(reply, lastCapturedChatSupplier));
	}

	private static int runDryRun(Consumer<Component> reply, Supplier<String> lastCapturedChatSupplier) {
		UUID demoUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		ScamRules.ScamAssessment demoAssessment = new ScamRules.ScamAssessment(
			72,
			ScamRules.ScamRiskLevel.CRITICAL,
			EnumSet.of(ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM, ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL),
			Map.of(
				ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM, "Behavior flag claimsTrustedMiddlemanWithoutProof=true (+20)",
				ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL, "Local AI probability=0.812, threshold=0.560 (+22)"
			),
			"this is legit middleman trust me and pay first",
			List.of("this is legit middleman trust me and pay first")
		);

		reply.accept(Component.literal("[ScamScreener] Preview dry run started."));
		reply.accept(Messages.addedToBlacklist("DemoPlayer", demoUuid));
		reply.accept(Messages.addedToBlacklistWithScore("DemoPlayer", demoUuid, 88));
		reply.accept(Messages.addedToBlacklistWithMetadata("DemoPlayer", demoUuid));
		reply.accept(Messages.alreadyBlacklisted("DemoPlayer", demoUuid));
		reply.accept(Messages.removedFromBlacklist("DemoPlayer", demoUuid));
		reply.accept(Messages.notOnBlacklist("DemoPlayer", demoUuid));
		reply.accept(Messages.blacklistEmpty());
		reply.accept(Messages.blacklistHeader());
		reply.accept(Messages.unresolvedTarget("DemoPlayer"));
		reply.accept(Messages.noChatToCapture());
		reply.accept(Messages.trainingSampleSaved("config/scamscreener/scam-screener-training-data.csv", 1));
		reply.accept(Messages.trainingSamplesSaved("config/scamscreener/scam-screener-training-data.csv", 0, 3));
		reply.accept(Messages.trainingSaveFailed("demo single-save failure"));
		reply.accept(Messages.trainingSamplesSaveFailed("demo batch-save failure"));
		reply.accept(Messages.trainingCompleted(42, 21, "scam-screener-training-data.csv.old.3"));
		reply.accept(Messages.trainingFailed("demo training failure"));
		reply.accept(Messages.mojangLookupStarted("DemoPlayer"));
		reply.accept(Messages.mojangLookupCompleted("DemoPlayer", "DemoPlayer"));
		reply.accept(Messages.blacklistWarning("DemoPlayer", "preview trigger", (BlacklistManager.ScamEntry) null));
		reply.accept(buildLiveBehaviorPreview(lastCapturedChatSupplier, demoAssessment));
		reply.accept(Component.literal("[ScamScreener] Preview dry run finished."));
		return 1;
	}

	private static Component buildLiveBehaviorPreview(
		Supplier<String> lastCapturedChatSupplier,
		ScamRules.ScamAssessment fallbackAssessment
	) {
		return Messages.behaviorRiskWarning("DemoPlayer", fallbackAssessment);
	}
}
