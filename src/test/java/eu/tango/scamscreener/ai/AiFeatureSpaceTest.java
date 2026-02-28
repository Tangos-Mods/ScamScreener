package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiFeatureSpaceTest {
	@Test
	void kwAccountStillMatchesLiteralEntries() {
		Map<String, Double> features = AiFeatureSpace.extractDenseFeatures(context("please share your password"));

		assertEquals(1.0, features.get("kw_account"));
	}

	@Test
	void kwAccountRequiresGiveOrGimmeForCode() {
		Map<String, Double> positive = AiFeatureSpace.extractDenseFeatures(context("gimme the code"));
		Map<String, Double> negative = AiFeatureSpace.extractDenseFeatures(context("what is the code"));

		assertEquals(1.0, positive.get("kw_account"));
		assertEquals(0.0, negative.get("kw_account"));
	}

	private static ScamRules.BehaviorContext context(String message) {
		return new ScamRules.BehaviorContext(
			message,
			"unknown",
			0L,
			false,
			false,
			false,
			false,
			0,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			0,
			0.0,
			false,
			false,
			0,
			0,
			0,
			0,
			0
		);
	}
}
