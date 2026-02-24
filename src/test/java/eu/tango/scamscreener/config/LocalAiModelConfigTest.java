package eu.tango.scamscreener.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAiModelConfigTest {
	@Test
	void normalizeMaxTokenWeightsFallsBackToDefaultWhenOutOfRange() {
		assertEquals(LocalAiModelConfig.DEFAULT_MAX_TOKEN_WEIGHTS, LocalAiModelConfig.normalizeMaxTokenWeights(1));
		assertEquals(LocalAiModelConfig.DEFAULT_MAX_TOKEN_WEIGHTS, LocalAiModelConfig.normalizeMaxTokenWeights(999_999));
	}

	@Test
	void pruneTokenWeightsKeepsLargestAbsoluteWeights() {
		Map<String, Double> weights = new LinkedHashMap<>();
		for (int i = 0; i < 520; i++) {
			weights.put("token-" + i, (double) i / 1000.0);
		}
		weights.put("bigPositive", 2.5);
		weights.put("bigNegative", -3.0);

		Map<String, Double> pruned = LocalAiModelConfig.pruneTokenWeights(weights, 500);

		assertEquals(500, pruned.size());
		assertTrue(pruned.containsKey("bigPositive"));
		assertTrue(pruned.containsKey("bigNegative"));
	}
}
