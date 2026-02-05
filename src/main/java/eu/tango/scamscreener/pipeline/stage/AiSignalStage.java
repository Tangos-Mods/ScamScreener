package eu.tango.scamscreener.pipeline.stage;

import java.util.List;
import eu.tango.scamscreener.pipeline.core.AiScorer;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.Signal;

public final class AiSignalStage {
	private final AiScorer aiScorer;

	/**
	 * Wraps {@link AiScorer} so the pipeline has a uniform "collectSignals" API.
	 */
	public AiSignalStage(AiScorer aiScorer) {
		this.aiScorer = aiScorer;
	}

	/**
	 * Returns a single AI signal, or an empty list if no trigger happened.
	 */
	public List<Signal> collectSignals(BehaviorAnalysis analysis) {
		Signal signal = aiScorer.score(analysis);
		if (signal == null) {
			return List.of();
		}
		return List.of(signal);
	}
}
