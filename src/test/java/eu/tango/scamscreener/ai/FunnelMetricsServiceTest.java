package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.pipeline.model.DetectionDecision;
import eu.tango.scamscreener.pipeline.model.DetectionEvaluation;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunnelMetricsServiceTest {
	@Test
	void recordsDetectionRateAndUserMarkedFalsePositives() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-funnel-metrics-");
		try {
			FunnelMetricsService service = new FunnelMetricsService(root.resolve("funnel-metrics.json"));

			service.recordEvaluation(evaluationWithRule("hello world", 2.0, ScamRules.ScamRule.SUSPICIOUS_LINK));
			service.recordEvaluation(evaluationWithRule("rep me on discord", 45.0, ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN));
			service.recordUserMark("rep me on discord", 0);
			service.recordUserMark("rep me on discord", 0);

			FunnelMetricsService.Snapshot snapshot = service.snapshot();
			assertEquals(2L, snapshot.evaluatedMessages());
			assertEquals(1L, snapshot.funnelDetections());
			assertEquals(1L, snapshot.userMarkedSamples());
			assertEquals(1L, snapshot.userMarkedLegit());
			assertEquals(0L, snapshot.userMarkedScam());
			assertEquals("50.00%", snapshot.detectionRatePercent());
			assertEquals("100.00%", snapshot.falsePositivePercent());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void tracksThresholdBoundaryCases() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-funnel-boundary-");
		try {
			FunnelMetricsService service = new FunnelMetricsService(root.resolve("funnel-metrics.json"));
			double threshold = 40.0;
			double score = threshold + 2.0;

			service.recordEvaluation(evaluationWithRule("discord rep join vc", score, ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN));

			FunnelMetricsService.Snapshot snapshot = service.snapshot();
			assertEquals(1L, snapshot.funnelDetections());
			assertEquals(1L, snapshot.uncertainBoundaryCases());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void persistsCountersAcrossInstances() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-funnel-persist-");
		try {
			Path file = root.resolve("funnel-metrics.json");
			FunnelMetricsService service = new FunnelMetricsService(file);
			service.recordEvaluation(evaluationWithRule("free carry vc", 50.0, ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL));
			service.recordUserMark("free carry vc", 1);

			FunnelMetricsService reloaded = new FunnelMetricsService(file);
			FunnelMetricsService.Snapshot snapshot = reloaded.snapshot();
			assertEquals(1L, snapshot.evaluatedMessages());
			assertEquals(1L, snapshot.funnelDetections());
			assertEquals(1L, snapshot.userMarkedSamples());
			assertEquals(0L, snapshot.userMarkedLegit());
			assertEquals(1L, snapshot.userMarkedScam());
			assertTrue(Files.exists(file));
		} finally {
			deleteTree(root);
		}
	}

	private static DetectionEvaluation evaluationWithRule(String message, double score, ScamRules.ScamRule rule) {
		MessageEvent event = MessageEvent.from("player", message, System.currentTimeMillis(), MessageContext.UNKNOWN, "public");
		DetectionResult result = new DetectionResult(
			score,
			DetectionLevel.HIGH,
			List.of(),
			Map.of(rule, "evidence"),
			false,
			List.of(message)
		);
		return new DetectionEvaluation(event, result, new DetectionDecision(true));
	}

	private static void deleteTree(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (var stream = Files.walk(root)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
				}
			});
		}
	}
}
