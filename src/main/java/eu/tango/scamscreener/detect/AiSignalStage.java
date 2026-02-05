package eu.tango.scamscreener.detect;

import java.util.List;

public final class AiSignalStage {
	private final AiScorer aiScorer;

	public AiSignalStage(AiScorer aiScorer) {
		this.aiScorer = aiScorer;
	}

	public List<Signal> collectSignals(BehaviorAnalysis analysis) {
		Signal signal = aiScorer.score(analysis);
		if (signal == null) {
			return List.of();
		}
		return List.of(signal);
	}
}
