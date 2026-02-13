package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringStageTest {
	@Test
	void funnelSignalEnablesAutoCaptureBelowHighThreshold() {
		ScoringStage stage = new ScoringStage(
			score -> DetectionLevel.MEDIUM,
			() -> "HIGH",
			ScoringStageTest::mapRiskLevel
		);
		MessageEvent event = MessageEvent.from("Trader123", "rep me then join discord", 1_000L, MessageContext.GENERAL, "public");
		Signal funnel = new Signal(
			ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN.name(),
			SignalSource.FUNNEL,
			25.0,
			"REP -> REDIRECT -> INSTRUCTION",
			ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN,
			List.of(event.rawMessage())
		);

		DetectionResult result = stage.score(event, List.of(funnel));

		assertEquals(DetectionLevel.MEDIUM, result.level());
		assertTrue(result.shouldCapture());
	}

	@Test
	void nonFunnelSignalStillRespectsAutoCaptureThreshold() {
		ScoringStage stage = new ScoringStage(
			score -> DetectionLevel.MEDIUM,
			() -> "HIGH",
			ScoringStageTest::mapRiskLevel
		);
		MessageEvent event = MessageEvent.from("Trader123", "join discord", 2_000L, MessageContext.GENERAL, "public");
		Signal nonFunnel = new Signal(
			ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH.name(),
			SignalSource.BEHAVIOR,
			25.0,
			"external platform push",
			ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH,
			List.of(event.rawMessage())
		);

		DetectionResult result = stage.score(event, List.of(nonFunnel));

		assertEquals(DetectionLevel.MEDIUM, result.level());
		assertFalse(result.shouldCapture());
	}

	@Test
	void funnelSignalDoesNotAutoCaptureWhenSettingIsOff() {
		ScoringStage stage = new ScoringStage(
			score -> DetectionLevel.MEDIUM,
			() -> "OFF",
			ScoringStageTest::mapRiskLevel
		);
		MessageEvent event = MessageEvent.from("Trader123", "rep me then join discord", 3_000L, MessageContext.GENERAL, "public");
		Signal funnel = new Signal(
			ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL.name(),
			SignalSource.AI,
			22.0,
			"funnel ai signal",
			ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL,
			List.of(event.rawMessage())
		);

		DetectionResult result = stage.score(event, List.of(funnel));

		assertFalse(result.shouldCapture());
	}

	private static ScamRules.ScamRiskLevel mapRiskLevel(DetectionLevel level) {
		if (level == null) {
			return ScamRules.ScamRiskLevel.LOW;
		}
		return switch (level) {
			case LOW -> ScamRules.ScamRiskLevel.LOW;
			case MEDIUM -> ScamRules.ScamRiskLevel.MEDIUM;
			case HIGH -> ScamRules.ScamRiskLevel.HIGH;
			case CRITICAL -> ScamRules.ScamRiskLevel.CRITICAL;
		};
	}
}
