package eu.tango.scamscreener.detect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TrendStore {
	private static final long TREND_WINDOW_MILLIS = 45_000L;
	private static final int TREND_MIN_MESSAGES = 3;
	private static final int TREND_MIN_TRIGGERED_MESSAGES = 2;
	private static final int TREND_MIN_TOTAL_SCORE = 35;
	private static final int TREND_SCORE_BONUS = 20;

	private final Map<String, Deque<TrendRecord>> historyByPlayer = new HashMap<>();

	public TrendEvaluation evaluate(MessageEvent event, List<Signal> existingSignals) {
		if (event == null || event.playerName() == null || event.playerName().isBlank()) {
			return TrendEvaluation.empty();
		}

		String key = event.playerName().toLowerCase(Locale.ROOT);
		long now = event.timestampMs() > 0 ? event.timestampMs() : System.currentTimeMillis();
		Deque<TrendRecord> history = historyByPlayer.computeIfAbsent(key, ignored -> new ArrayDeque<>());

		while (!history.isEmpty() && now - history.peekFirst().timestampMillis() > TREND_WINDOW_MILLIS) {
			history.removeFirst();
		}

		int messageScore = (int) Math.round(existingSignals.stream().mapToDouble(Signal::weight).sum());
		boolean hadRule = existingSignals.stream().anyMatch(signal -> signal.ruleId() != null);
		history.addLast(new TrendRecord(now, messageScore, hadRule, event.rawMessage()));
		while (history.size() > 8) {
			history.removeFirst();
		}

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
	}

	public record TrendEvaluation(int bonusScore, String detail, List<String> evaluatedMessages) {
		public static TrendEvaluation empty() {
			return new TrendEvaluation(0, null, List.of());
		}
	}

	private record TrendRecord(long timestampMillis, int riskScore, boolean hadRules, String message) {
	}
}
