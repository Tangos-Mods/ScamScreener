package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.Locale;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class BehaviorAnalyzer {
	private final RuleConfig ruleConfig;
	private String lastPlayerKey;
	private int consecutiveCount;
	private final java.util.List<String> consecutiveMessages = new java.util.ArrayList<>();

	/**
	 * Extracts behavior flags from each chat line (e.g. external platform push).
	 * Behavior flags are later turned into signals by {@link eu.tango.scamscreener.pipeline.stage.BehaviorSignalStage}.
	 */
	public BehaviorAnalyzer(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	/**
	 * Builds a {@link BehaviorAnalysis} snapshot for the given event.
	 * This does not score; it only classifies behaviors and counts repeat contact attempts.
	 */
	public BehaviorAnalysis analyze(MessageEvent event) {
		if (event == null) {
			return new BehaviorAnalysis("", "", false, false, false, false, 0, java.util.List.of());
		}

		String normalized = event.normalizedMessage();
		String playerKey = event.playerName() == null ? "" : event.playerName().trim().toLowerCase(Locale.ROOT);
		if (playerKey.isBlank()) {
			resetStreak();
			return new BehaviorAnalysis(
				event.rawMessage(),
				normalized,
				false,
				false,
				false,
				false,
				0,
				java.util.List.of()
			);
		}

		if (!playerKey.equals(lastPlayerKey)) {
			resetStreak();
		}

		consecutiveCount++;
		if (event.rawMessage() != null && !event.rawMessage().isBlank()) {
			consecutiveMessages.add(event.rawMessage());
		}
		lastPlayerKey = playerKey;

		ScamRules.BehaviorPatternSet patterns = ruleConfig.behaviorPatterns();
		return new BehaviorAnalysis(
			event.rawMessage(),
			normalized,
			matches(patterns.externalPlatform(), normalized),
			matches(patterns.upfrontPayment(), normalized),
			matches(patterns.accountData(), normalized),
			matches(patterns.middlemanClaim(), normalized),
			consecutiveCount,
			java.util.List.copyOf(consecutiveMessages)
		);
	}

	/**
	 * Clears per-player repeat contact counters.
	 */
	public void reset() {
		resetStreak();
	}

	private void resetStreak() {
		lastPlayerKey = null;
		consecutiveCount = 0;
		consecutiveMessages.clear();
	}

	private static boolean matches(java.util.regex.Pattern pattern, String text) {
		if (pattern == null || text == null || text.isBlank()) {
			return false;
		}
		return pattern.matcher(text).find();
	}
}
