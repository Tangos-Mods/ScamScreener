package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.ai.AiFunnelContextTracker;
import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.TextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;

public final class AiScorer {
	private static final int MIN_FUNNEL_STEP_FOR_AI = 2;

	private final LocalAiScorer localAiScorer;
	private final RuleConfig ruleConfig;
	private final IntentTagger intentTagger;
	private final AiFunnelContextTracker funnelTracker = new AiFunnelContextTracker();
	private final Map<String, Long> lastMessageTimestampByPlayer = new HashMap<>();

	/**
	 * Uses the local model to turn a {@link BehaviorAnalysis} into AI {@link Signal}s.
	 */
	public AiScorer(LocalAiScorer localAiScorer, RuleConfig ruleConfig) {
		this.localAiScorer = localAiScorer;
		this.ruleConfig = ruleConfig;
		this.intentTagger = new IntentTagger(ruleConfig);
	}

	/**
	 * Returns an empty list when AI is disabled or no AI signal triggers.
	 * See {@link eu.tango.scamscreener.rules.ScamRules#localAiEnabled()} and
	 * {@link eu.tango.scamscreener.rules.ScamRules#localAiTriggerProbability()}.
	 */
	public List<Signal> score(MessageEvent event, BehaviorAnalysis analysis, List<Signal> existingSignals) {
		if (analysis == null || event == null || !ScamRules.localAiEnabled()) {
			return List.of();
		}
		boolean localAiEnabled = ruleConfig.isEnabled(ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL);
		boolean funnelAiEnabled = ruleConfig.isEnabled(ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL);
		if (!localAiEnabled && !funnelAiEnabled) {
			return List.of();
		}

		List<Signal> safeSignals = existingSignals == null ? List.of() : existingSignals;
		IntentTagger.TaggingResult tagging = intentTagger.tag(event, safeSignals);
		String speakerKey = TextUtil.anonymizedSpeakerKey(event.playerName());
		String modelMessage = analysis.message() == null ? "" : analysis.message();
		String safeNormalized = MessageEvent.normalizeMessage(modelMessage);
		AiFunnelContextTracker.Snapshot funnel = funnelTracker.update(
			speakerKey,
			event.timestampMs(),
			tagging.tags(),
			tagging.negativeContext()
		);

		SignalHistogram hits = SignalHistogram.from(safeSignals);
		Set<IntentTag> tags = tagging.tags();
		ScamRules.BehaviorContext context = new ScamRules.BehaviorContext(
			modelMessage,
			event.channel() == null ? "unknown" : event.channel(),
			computeDeltaMillis(speakerKey, event.timestampMs()),
			analysis.pushesExternalPlatform(),
			analysis.demandsUpfrontPayment(),
			analysis.requestsSensitiveData(),
			analysis.claimsTrustedMiddlemanWithoutProof(),
			analysis.repeatedContactAttempts(),
			containsAny(safeNormalized, "free", "100 safe", "guaranteed", "garantiert", "dupe"),
			containsAny(safeNormalized, "spam", "last chance", "cheap", "buy now", "limited"),
			containsAny(safeNormalized, "borrow", "lend me", "give me", "can i have"),
			containsAny(safeNormalized, "/visit", "join my", "shop", "selling", "carry"),
			tags.contains(IntentTag.SERVICE_OFFER) || tags.contains(IntentTag.FREE_OFFER),
			tags.contains(IntentTag.REP_REQUEST),
			tags.contains(IntentTag.PLATFORM_REDIRECT),
			tags.contains(IntentTag.INSTRUCTION_INJECTION),
			tags.contains(IntentTag.PAYMENT_UPFRONT),
			tags.contains(IntentTag.COMMUNITY_ANCHOR),
			funnel.stepIndex(),
			funnel.score(),
			funnel.fullChain(),
			funnel.partialChain(),
			hits.ruleHits(),
			hits.similarityHits(),
			hits.behaviorHits(),
			hits.trendHits(),
			hits.funnelHits()
		);
		List<Signal> out = new ArrayList<>(2);

		if (localAiEnabled) {
			LocalAiScorer.AiResult result = localAiScorer.score(
				context,
				ScamRules.localAiMaxScore(),
				ScamRules.localAiTriggerProbability()
			);
			if (result.triggered() && result.score() > 0) {
				String probability = String.format(Locale.ROOT, "%.3f", result.probability());
				String threshold = String.format(Locale.ROOT, "%.3f", ScamRules.localAiTriggerProbability());
				String evidence = "Local model probability=" + probability + ", threshold=" + threshold + " (+" + result.score() + ")\n" + result.explanation();
				out.add(new Signal(
					ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL.name(),
					SignalSource.AI,
					result.score(),
					evidence,
					ScamRules.ScamRule.LOCAL_AI_RISK_SIGNAL,
					java.util.List.of()
				));
			}
		}

		if (funnelAiEnabled && shouldEvaluateFunnelAi(context)) {
			double trigger = Math.min(0.98, ScamRules.localAiTriggerProbability() + ScamRules.localAiFunnelThresholdBonus());
			LocalAiScorer.AiResult funnelResult = localAiScorer.scoreFunnelOnly(
				context,
				Math.min(ScamRules.localAiMaxScore(), ScamRules.localAiFunnelMaxScore()),
				trigger
			);
			if (funnelResult.triggered() && funnelResult.score() > 0) {
				String probability = String.format(Locale.ROOT, "%.3f", funnelResult.probability());
				String threshold = String.format(Locale.ROOT, "%.3f", trigger);
				String sequence = String.format(Locale.ROOT, "%.1f", context.funnelSequenceScore());
				String evidence = "Funnel model probability=" + probability
					+ ", threshold=" + threshold
					+ " (+" + funnelResult.score() + ")"
					+ ", step=" + context.funnelStepIndex()
					+ ", sequence=" + sequence
					+ ", full=" + context.funnelFullChain()
					+ ", partial=" + context.funnelPartialChain()
					+ "\n" + funnelResult.explanation();
				out.add(new Signal(
					ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL.name(),
					SignalSource.AI,
					funnelResult.score(),
					evidence,
					ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL,
					java.util.List.of()
				));
			}
		}

		return out;
	}

	public void reset() {
		funnelTracker.reset();
		lastMessageTimestampByPlayer.clear();
	}

	private long computeDeltaMillis(String speakerKey, long timestampMs) {
		if (speakerKey == null || speakerKey.isBlank()) {
			return 0L;
		}
		long now = timestampMs > 0 ? timestampMs : System.currentTimeMillis();
		Long previous = lastMessageTimestampByPlayer.put(speakerKey, now);
		if (previous == null || previous <= 0L || now <= previous) {
			return 0L;
		}
		return now - previous;
	}

	private static boolean containsAny(String text, String first, String... others) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String normalized = text.toLowerCase(Locale.ROOT);
		if (normalized.contains(first)) {
			return true;
		}
		for (String item : others) {
			if (normalized.contains(item)) {
				return true;
			}
		}
		return false;
	}

	private static boolean shouldEvaluateFunnelAi(ScamRules.BehaviorContext context) {
		if (context == null) {
			return false;
		}
		if (context.funnelFullChain() || context.funnelPartialChain()) {
			return true;
		}
		if (context.funnelStepIndex() >= MIN_FUNNEL_STEP_FOR_AI) {
			return true;
		}
		return context.funnelHits() > 0;
	}

	private record SignalHistogram(int ruleHits, int similarityHits, int behaviorHits, int trendHits, int funnelHits) {
		private static SignalHistogram from(List<Signal> signals) {
			int rule = 0;
			int similarity = 0;
			int behavior = 0;
			int trend = 0;
			int funnel = 0;
			if (signals != null) {
				for (Signal signal : signals) {
					if (signal == null) {
						continue;
					}
					if (signal.source() == SignalSource.RULE) {
						rule++;
					}
					if (signal.ruleId() == ScamRules.ScamRule.SIMILARITY_MATCH) {
						similarity++;
					}
					if (signal.source() == SignalSource.BEHAVIOR) {
						behavior++;
					}
					if (signal.source() == SignalSource.TREND) {
						trend++;
					}
					if (signal.source() == SignalSource.FUNNEL) {
						funnel++;
					}
				}
			}
			return new SignalHistogram(rule, similarity, behavior, trend, funnel);
		}
	}
}
