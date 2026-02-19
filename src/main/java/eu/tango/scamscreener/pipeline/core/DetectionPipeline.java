package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.DetectionDecision;
import eu.tango.scamscreener.pipeline.model.DetectionEvaluation;
import eu.tango.scamscreener.pipeline.model.DetectionOutcome;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.stage.AiSignalStage;
import eu.tango.scamscreener.pipeline.stage.BehaviorSignalStage;
import eu.tango.scamscreener.pipeline.stage.DecisionStage;
import eu.tango.scamscreener.pipeline.stage.MuteStage;
import eu.tango.scamscreener.pipeline.stage.OutputStage;
import eu.tango.scamscreener.pipeline.stage.RuleSignalStage;
import eu.tango.scamscreener.pipeline.stage.ScoringStage;
import eu.tango.scamscreener.pipeline.stage.LevenshteinSignalStage;
import eu.tango.scamscreener.pipeline.stage.FunnelSignalStage;
import eu.tango.scamscreener.pipeline.stage.TrendSignalStage;
import eu.tango.scamscreener.pipeline.stage.WhitelistStage;

public final class DetectionPipeline {
	private final MuteStage muteStage;
	private final WhitelistStage whitelistStage;
	private final RuleSignalStage ruleSignalStage;
	private final LevenshteinSignalStage levenshteinSignalStage;
	private final BehaviorAnalyzer behaviorAnalyzer;
	private final BehaviorSignalStage behaviorSignalStage;
	private final AiSignalStage aiSignalStage;
	private final TrendStore trendStore;
	private final TrendSignalStage trendSignalStage;
	private final FunnelStore funnelStore;
	private final FunnelSignalStage funnelSignalStage;
	private final ScoringStage scoringStage;
	private final DecisionStage decisionStage;
	private final OutputStage outputStage;

	/**
	 * Creates the full detection pipeline with all stages wired up.
	 * The pipeline is executed from {@link #process(MessageEvent, java.util.function.Consumer, Runnable)}.
	 */
	public DetectionPipeline(
		MutePatternManager mutePatternManager,
		WhitelistManager whitelistManager,
		Function<String, UUID> uuidResolver,
		LocalAiScorer localAiScorer
	) {
		RuleConfig ruleConfig = new DefaultRuleConfig();
		this.muteStage = new MuteStage(mutePatternManager);
		this.whitelistStage = new WhitelistStage(whitelistManager, uuidResolver);
		this.ruleSignalStage = new RuleSignalStage(ruleConfig);
		this.levenshteinSignalStage = new LevenshteinSignalStage(ruleConfig);
		this.behaviorAnalyzer = new BehaviorAnalyzer(ruleConfig);
		this.behaviorSignalStage = new BehaviorSignalStage(ruleConfig);
		this.aiSignalStage = new AiSignalStage(new AiScorer(localAiScorer, ruleConfig));
		this.trendStore = new TrendStore();
		this.trendSignalStage = new TrendSignalStage(ruleConfig, trendStore);
		this.funnelStore = new FunnelStore(ruleConfig);
		this.funnelSignalStage = new FunnelSignalStage(ruleConfig, funnelStore);
		this.scoringStage = new ScoringStage();
		this.decisionStage = new DecisionStage(new WarningDeduplicator());
		this.outputStage = new OutputStage();
	}

	/**
	 * Runs the pipeline for a single chat event. Stages are executed in this order:
	 * {@link MuteStage} -> {@link WhitelistStage} -> {@link BehaviorAnalyzer} -> {@link RuleSignalStage}
	 * -> {@link LevenshteinSignalStage} -> {@link BehaviorSignalStage} -> {@link TrendSignalStage}
	 * -> {@link FunnelSignalStage} -> {@link AiSignalStage} -> {@link ScoringStage}
	 * -> {@link DecisionStage} -> {@link OutputStage}.
	 */
	public Optional<DetectionOutcome> process(MessageEvent event, Consumer<Component> reply, Runnable warningSound) {
		return process(event, reply, warningSound, null);
	}

	/**
	 * Same as {@link #process(MessageEvent, Consumer, Runnable)} but also exposes the
	 * internal scoring/decision output for local telemetry aggregation.
	 */
	public Optional<DetectionOutcome> process(
		MessageEvent event,
		Consumer<Component> reply,
		Runnable warningSound,
		Consumer<DetectionEvaluation> evaluationConsumer
	) {
		Optional<MessageEvent> maybeEvent = muteStage.filter(event);
		if (maybeEvent.isEmpty()) {
			return Optional.empty();
		}

		Optional<MessageEvent> maybeWhitelistedEvent = whitelistStage.filter(maybeEvent.get());
		if (maybeWhitelistedEvent.isEmpty()) {
			return Optional.empty();
		}

		MessageEvent safeEvent = maybeWhitelistedEvent.get();
		BehaviorAnalysis analysis = behaviorAnalyzer.analyze(safeEvent);
		List<Signal> signals = new ArrayList<>();
		signals.addAll(ruleSignalStage.collectSignals(safeEvent));
		signals.addAll(levenshteinSignalStage.collectSignals(safeEvent));
		signals.addAll(behaviorSignalStage.collectSignals(analysis));
		signals.addAll(trendSignalStage.collectSignals(safeEvent, signals));
		signals.addAll(funnelSignalStage.collectSignals(safeEvent, signals));
		signals.addAll(aiSignalStage.collectSignals(safeEvent, analysis, signals));

		DetectionResult result = scoringStage.score(safeEvent, signals);
		DetectionDecision decision = decisionStage.decide(safeEvent, result);
		if (evaluationConsumer != null) {
			evaluationConsumer.accept(new DetectionEvaluation(safeEvent, result, decision));
		}
		if (decision.shouldWarn()) {
			outputStage.output(safeEvent, result, decision, reply, warningSound);
		}
		if (!decision.shouldWarn() && !result.shouldCapture()) {
			return Optional.empty();
		}
		return Optional.of(new DetectionOutcome(safeEvent, result));
	}

	/**
	 * Clears any stateful stage data (trend/funnel history, dedupe, repeated-contact counts).
	 */
	public void reset() {
		behaviorAnalyzer.reset();
		aiSignalStage.reset();
		decisionStage.reset();
		trendStore.reset();
		funnelStore.reset();
	}
}
