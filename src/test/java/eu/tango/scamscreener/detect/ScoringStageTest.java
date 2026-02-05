package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringStageTest {
	@Test
	void combinesSignalsIntoStableScoreAndLevel() {
		ScoringStage stage = new ScoringStage();
		List<Signal> signals = List.of(
			new Signal("URGENT", SignalSource.RULE, 15, "urgency", ScamRules.ScamRule.PRESSURE_AND_URGENCY, List.of()),
			new Signal("PAY", SignalSource.BEHAVIOR, 25, "payment", ScamRules.ScamRule.UPFRONT_PAYMENT, List.of())
		);
		DetectionResult result = stage.score(MessageEvent.from("Player", "pay first", 1L, MessageContext.UNKNOWN, null), signals);
		assertEquals(40.0, result.totalScore());
		assertEquals(DetectionLevel.HIGH, result.level());
		assertEquals(2, result.triggeredRules().size());
	}
}
