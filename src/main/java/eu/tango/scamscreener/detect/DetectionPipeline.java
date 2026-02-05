package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.detection.MutePatternManager;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class DetectionPipeline {
	private final MuteStage muteStage;
	private final RuleSignalStage ruleSignalStage;
	private final BehaviorAnalyzer behaviorAnalyzer;
	private final BehaviorSignalStage behaviorSignalStage;
	private final AiSignalStage aiSignalStage;
	private final TrendStore trendStore;
	private final TrendSignalStage trendSignalStage;
	private final ScoringStage scoringStage;
	private final DecisionStage decisionStage;
	private final OutputStage outputStage;

	public DetectionPipeline(MutePatternManager mutePatternManager, LocalAiScorer localAiScorer) {
		RuleConfig ruleConfig = new DefaultRuleConfig();
		this.muteStage = new MuteStage(mutePatternManager);
		this.ruleSignalStage = new RuleSignalStage(ruleConfig);
		this.behaviorAnalyzer = new BehaviorAnalyzer(ruleConfig);
		this.behaviorSignalStage = new BehaviorSignalStage(ruleConfig);
		this.aiSignalStage = new AiSignalStage(new AiScorer(localAiScorer, ruleConfig));
		this.trendStore = new TrendStore();
		this.trendSignalStage = new TrendSignalStage(ruleConfig, trendStore);
		this.scoringStage = new ScoringStage();
		this.decisionStage = new DecisionStage(new WarningDeduplicator());
		this.outputStage = new OutputStage();
	}

	public Optional<DetectionOutcome> process(MessageEvent event, Consumer<Component> reply, Runnable warningSound) {
		Optional<MessageEvent> maybeEvent = muteStage.filter(event);
		if (maybeEvent.isEmpty()) {
			return Optional.empty();
		}

		MessageEvent safeEvent = maybeEvent.get();
		BehaviorAnalysis analysis = behaviorAnalyzer.analyze(safeEvent);
		List<Signal> signals = new ArrayList<>();
		signals.addAll(ruleSignalStage.collectSignals(safeEvent));
		signals.addAll(behaviorSignalStage.collectSignals(analysis));
		signals.addAll(aiSignalStage.collectSignals(analysis));
		signals.addAll(trendSignalStage.collectSignals(safeEvent, signals));

		DetectionResult result = scoringStage.score(safeEvent, signals);
		DetectionDecision decision = decisionStage.decide(safeEvent, result);
		if (!decision.shouldWarn()) {
			return Optional.empty();
		}

		outputStage.output(safeEvent, result, decision, reply, warningSound);
		return Optional.of(new DetectionOutcome(safeEvent, result));
	}

	public void reset() {
		behaviorAnalyzer.reset();
		decisionStage.reset();
		trendStore.reset();
	}
}
