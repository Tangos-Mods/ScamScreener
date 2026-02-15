package eu.tango.scamscreener.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.ScamScreenerPaths;
import eu.tango.scamscreener.pipeline.model.DetectionEvaluation;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FunnelMetricsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FunnelMetricsService.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long FEEDBACK_MATCH_TTL_MS = 30L * 60L * 1000L;
	private static final long SAVE_INTERVAL_MS = 10_000L;
	private static final long SAVE_AFTER_UPDATES = 25L;
	private static final double UNCERTAIN_MARGIN = 5.0;
	private static final int LEGIT_LABEL = 0;
	private static final int SCAM_LABEL = 1;

	private final Path metricsPath;
	private final MetricsState state;
	private final Map<String, ArrayDeque<Long>> pendingByMessage = new HashMap<>();
	private long unsavedChanges;
	private long lastSaveTimestampMs;

	public FunnelMetricsService() {
		this(resolveDefaultPath());
	}

	FunnelMetricsService(Path metricsPath) {
		this.metricsPath = metricsPath == null ? resolveDefaultPath() : metricsPath;
		this.state = load(this.metricsPath);
		this.lastSaveTimestampMs = System.currentTimeMillis();
	}

	public synchronized void recordEvaluation(DetectionEvaluation evaluation) {
		if (evaluation == null || evaluation.result() == null) {
			return;
		}
		state.evaluatedMessages = safeIncrement(state.evaluatedMessages);
		DetectionResult result = evaluation.result();
		if (hasFunnelDetection(result)) {
			state.funnelDetections = safeIncrement(state.funnelDetections);
			double threshold = currentAlertThreshold();
			if (threshold > 0.0 && Math.abs(result.totalScore() - threshold) <= UNCERTAIN_MARGIN) {
				state.uncertainBoundaryCases = safeIncrement(state.uncertainBoundaryCases);
			}
			long now = System.currentTimeMillis();
			for (String message : relatedMessages(evaluation)) {
				String key = fingerprint(message);
				if (key.isBlank()) {
					continue;
				}
				pendingByMessage.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(now);
			}
			cleanupPending(now);
		}
		markDirtyAndMaybeSave(false);
	}

	public synchronized void recordUserMark(String message, int label) {
		if (label != LEGIT_LABEL && label != SCAM_LABEL) {
			return;
		}
		String key = fingerprint(message);
		if (key.isBlank()) {
			return;
		}
		long now = System.currentTimeMillis();
		cleanupPending(now);
		ArrayDeque<Long> queue = pendingByMessage.get(key);
		if (queue == null || queue.isEmpty()) {
			return;
		}
		queue.removeFirst();
		if (queue.isEmpty()) {
			pendingByMessage.remove(key);
		}

		state.userMarkedSamples = safeIncrement(state.userMarkedSamples);
		if (label == LEGIT_LABEL) {
			state.userMarkedLegit = safeIncrement(state.userMarkedLegit);
		} else {
			state.userMarkedScam = safeIncrement(state.userMarkedScam);
		}
		markDirtyAndMaybeSave(true);
	}

	public synchronized Snapshot snapshot() {
		cleanupPending(System.currentTimeMillis());
		long evaluated = Math.max(0L, state.evaluatedMessages);
		long funnel = Math.max(0L, state.funnelDetections);
		long uncertain = Math.max(0L, state.uncertainBoundaryCases);
		long marked = Math.max(0L, state.userMarkedSamples);
		long markedLegit = Math.max(0L, state.userMarkedLegit);
		long markedScam = Math.max(0L, state.userMarkedScam);
		double detectionRate = evaluated <= 0L ? 0.0 : (double) funnel / (double) evaluated;
		double falsePositiveRate = marked <= 0L ? 0.0 : (double) markedLegit / (double) marked;
		return new Snapshot(
			evaluated,
			funnel,
			uncertain,
			marked,
			markedLegit,
			markedScam,
			detectionRate,
			falsePositiveRate,
			currentAlertThreshold(),
			UNCERTAIN_MARGIN
		);
	}

	public synchronized void reset() {
		state.evaluatedMessages = 0L;
		state.funnelDetections = 0L;
		state.uncertainBoundaryCases = 0L;
		state.userMarkedSamples = 0L;
		state.userMarkedLegit = 0L;
		state.userMarkedScam = 0L;
		pendingByMessage.clear();
		markDirtyAndMaybeSave(true);
	}

	private void markDirtyAndMaybeSave(boolean force) {
		unsavedChanges = safeIncrement(unsavedChanges);
		long now = System.currentTimeMillis();
		boolean intervalElapsed = now - lastSaveTimestampMs >= SAVE_INTERVAL_MS;
		if (!force && unsavedChanges < SAVE_AFTER_UPDATES && !intervalElapsed) {
			return;
		}
		save(this.metricsPath, state);
		unsavedChanges = 0L;
		lastSaveTimestampMs = now;
	}

	private static long safeIncrement(long value) {
		if (value >= Long.MAX_VALUE) {
			return Long.MAX_VALUE;
		}
		return value + 1L;
	}

	private static List<String> relatedMessages(DetectionEvaluation evaluation) {
		DetectionResult result = evaluation.result();
		if (result == null) {
			return List.of();
		}
		if (result.evaluatedMessages() != null && !result.evaluatedMessages().isEmpty()) {
			return result.evaluatedMessages();
		}
		if (evaluation.event() != null && evaluation.event().rawMessage() != null && !evaluation.event().rawMessage().isBlank()) {
			return List.of(evaluation.event().rawMessage());
		}
		return List.of();
	}

	private static String fingerprint(String message) {
		String normalized = TextUtil.normalizeForMatch(message);
		return normalized == null ? "" : normalized;
	}

	private static boolean hasFunnelDetection(DetectionResult result) {
		if (result == null || result.triggeredRules() == null || result.triggeredRules().isEmpty()) {
			return false;
		}
		return result.triggeredRules().containsKey(ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN)
			|| result.triggeredRules().containsKey(ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL);
	}

	private void cleanupPending(long nowMs) {
		pendingByMessage.entrySet().removeIf(entry -> {
			ArrayDeque<Long> queue = entry.getValue();
			if (queue == null || queue.isEmpty()) {
				return true;
			}
			while (!queue.isEmpty()) {
				Long timestamp = queue.peekFirst();
				if (timestamp == null || nowMs - timestamp > FEEDBACK_MATCH_TTL_MS) {
					queue.removeFirst();
					continue;
				}
				break;
			}
			return queue.isEmpty();
		});
	}

	private static double currentAlertThreshold() {
		try {
			ScamRules.ScamRiskLevel level = ScamRules.minimumAlertRiskLevel();
			if (level == null) {
				return 0.0;
			}
			return switch (level) {
				case LOW -> 0.0;
				case MEDIUM -> ScamRules.levelMediumThreshold();
				case HIGH -> ScamRules.levelHighThreshold();
				case CRITICAL -> ScamRules.levelCriticalThreshold();
			};
		} catch (Throwable ignored) {
			// Unit tests can run without FabricLoader config wiring.
			return 40.0;
		}
	}

	private static MetricsState load(Path path) {
		if (path == null || !Files.isRegularFile(path)) {
			return new MetricsState();
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			MetricsState parsed = GSON.fromJson(reader, MetricsState.class);
			return normalize(parsed);
		} catch (Exception e) {
			LOGGER.warn("Failed to load funnel metrics from {}", path, e);
			return new MetricsState();
		}
	}

	private static MetricsState normalize(MetricsState state) {
		MetricsState safe = state == null ? new MetricsState() : state;
		safe.evaluatedMessages = Math.max(0L, safe.evaluatedMessages);
		safe.funnelDetections = Math.max(0L, safe.funnelDetections);
		safe.uncertainBoundaryCases = Math.max(0L, safe.uncertainBoundaryCases);
		safe.userMarkedSamples = Math.max(0L, safe.userMarkedSamples);
		safe.userMarkedLegit = Math.max(0L, safe.userMarkedLegit);
		safe.userMarkedScam = Math.max(0L, safe.userMarkedScam);
		return safe;
	}

	private static Path resolveDefaultPath() {
		try {
			return ScamScreenerPaths.inModConfigDir("funnel-metrics.json");
		} catch (Exception ignored) {
			return Path.of("config", "scamscreener", "funnel-metrics.json");
		}
	}

	private static void save(Path path, MetricsState state) {
		if (path == null || state == null) {
			return;
		}
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(state, writer);
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to save funnel metrics to {}", path, e);
		}
	}

	private static final class MetricsState {
		long evaluatedMessages;
		long funnelDetections;
		long uncertainBoundaryCases;
		long userMarkedSamples;
		long userMarkedLegit;
		long userMarkedScam;
	}

	public record Snapshot(
		long evaluatedMessages,
		long funnelDetections,
		long uncertainBoundaryCases,
		long userMarkedSamples,
		long userMarkedLegit,
		long userMarkedScam,
		double funnelDetectionRate,
		double falsePositiveRate,
		double alertThreshold,
		double uncertainMargin
	) {
		public String detectionRatePercent() {
			return String.format(Locale.ROOT, "%.2f%%", Math.max(0.0, funnelDetectionRate) * 100.0);
		}

		public String falsePositivePercent() {
			return String.format(Locale.ROOT, "%.2f%%", Math.max(0.0, falsePositiveRate) * 100.0);
		}
	}
}
