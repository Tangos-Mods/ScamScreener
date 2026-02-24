package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.util.TextUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class TrendStore {
	private static final long TREND_WINDOW_MILLIS = 45_000L;
	private static final int TREND_MIN_MESSAGES = 3;
	private static final int TREND_MIN_TRIGGERED_MESSAGES = 2;
	private static final int TREND_MIN_TOTAL_SCORE = 35;
	private static final int TREND_SCORE_BONUS = 20;
	private static final long PLAYER_HISTORY_TTL_MILLIS = 600_000L;
	private static final int CLEANUP_INTERVAL_EVALUATIONS = 64;
	private static final int MAX_PLAYERS_TRACKED = 1_024;

	private final Map<String, PlayerTrendHistory> historyByPlayer = new HashMap<>();
	private int evaluationsSinceCleanup;

	/**
	 * Tracks recent messages per player and decides whether a trend bonus applies.
	 * The returned {@link TrendEvaluation} is consumed by {@link eu.tango.scamscreener.pipeline.stage.TrendSignalStage}.
	 */
	public TrendEvaluation evaluate(MessageEvent event, List<Signal> existingSignals) {
		if (event == null || event.playerName() == null || event.playerName().isBlank()) {
			return TrendEvaluation.empty();
		}

		String key = TextUtil.anonymizedSpeakerKey(event.playerName());
		long now = event.timestampMs() > 0 ? event.timestampMs() : System.currentTimeMillis();
		boolean mapSizeIncreased = !historyByPlayer.containsKey(key);
		PlayerTrendHistory state = historyByPlayer.computeIfAbsent(key, ignored -> new PlayerTrendHistory());
		state.lastSeenMillis = now;
		Deque<TrendRecord> history = state.history;
		trimWindow(history, now);

		int messageScore = (int) Math.round(existingSignals.stream().mapToDouble(Signal::weight).sum());
		boolean hadRule = existingSignals.stream().anyMatch(signal -> signal.ruleId() != null);
		history.addLast(new TrendRecord(now, messageScore, hadRule, event.rawMessage()));
		while (history.size() > 8) {
			history.removeFirst();
		}

		maybeCleanup(now, mapSizeIncreased);

		int totalScore = 0;
		int triggeredMessages = 0;
		List<String> evaluatedMessages = new ArrayList<>();
		for (TrendRecord record : history) {
			totalScore += Math.max(0, record.riskScore());
			if (record.hadRules()) {
				triggeredMessages++;
			}
			if (record.message() != null && !record.message().isBlank()) {
				evaluatedMessages.add(record.message());
			}
		}

		boolean trendTriggered = history.size() >= TREND_MIN_MESSAGES
			&& triggeredMessages >= TREND_MIN_TRIGGERED_MESSAGES
			&& totalScore >= TREND_MIN_TOTAL_SCORE;

		if (!trendTriggered) {
			return TrendEvaluation.empty();
		}

		String detail = "Conversation trend: " + history.size() + " messages in " + (TREND_WINDOW_MILLIS / 1000)
			+ "s, triggered messages=" + triggeredMessages + ", cumulative score=" + totalScore + " (+" + TREND_SCORE_BONUS + ")";

		return new TrendEvaluation(TREND_SCORE_BONUS, detail, evaluatedMessages);
	}

	public void reset() {
		historyByPlayer.clear();
		evaluationsSinceCleanup = 0;
	}

	private void maybeCleanup(long now, boolean mapSizeIncreased) {
		evaluationsSinceCleanup++;
		if (!mapSizeIncreased && evaluationsSinceCleanup < CLEANUP_INTERVAL_EVALUATIONS) {
			return;
		}
		evaluationsSinceCleanup = 0;
		pruneExpiredPlayers(now);
		enforceMaxPlayersTracked();
	}

	private void pruneExpiredPlayers(long now) {
		Iterator<Map.Entry<String, PlayerTrendHistory>> iterator = historyByPlayer.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, PlayerTrendHistory> entry = iterator.next();
			PlayerTrendHistory state = entry.getValue();
			if (state == null) {
				iterator.remove();
				continue;
			}
			trimWindow(state.history, now);
			if (state.history.isEmpty() || now - state.lastSeenMillis > PLAYER_HISTORY_TTL_MILLIS) {
				iterator.remove();
			}
		}
	}

	private void enforceMaxPlayersTracked() {
		while (historyByPlayer.size() > MAX_PLAYERS_TRACKED) {
			String oldestKey = null;
			long oldestSeen = Long.MAX_VALUE;
			for (Map.Entry<String, PlayerTrendHistory> entry : historyByPlayer.entrySet()) {
				PlayerTrendHistory state = entry.getValue();
				long seen = state == null ? Long.MIN_VALUE : state.lastSeenMillis;
				if (oldestKey == null || seen < oldestSeen) {
					oldestKey = entry.getKey();
					oldestSeen = seen;
				}
			}
			if (oldestKey == null) {
				return;
			}
			historyByPlayer.remove(oldestKey);
		}
	}

	private static void trimWindow(Deque<TrendRecord> history, long now) {
		while (!history.isEmpty() && now - history.peekFirst().timestampMillis() > TREND_WINDOW_MILLIS) {
			history.removeFirst();
		}
	}

	/**
	 * Result of a trend evaluation. If {@link #detail()} is {@code null}, no trend triggered.
	 */
	public record TrendEvaluation(int bonusScore, String detail, List<String> evaluatedMessages) {
		public static TrendEvaluation empty() {
			return new TrendEvaluation(0, null, List.of());
		}
	}

	private static final class PlayerTrendHistory {
		private final Deque<TrendRecord> history = new ArrayDeque<>();
		private long lastSeenMillis;
	}

	private record TrendRecord(long timestampMillis, int riskScore, boolean hadRules, String message) {
	}
}
