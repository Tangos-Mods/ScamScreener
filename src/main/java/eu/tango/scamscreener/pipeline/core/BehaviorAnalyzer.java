package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.MessageEvent;

public final class BehaviorAnalyzer {
	private final RuleConfig ruleConfig;
	private final Map<String, Integer> repeatedContactByPlayer = new HashMap<>();

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
			return new BehaviorAnalysis("", "", false, false, false, false, 0);
		}

		String normalized = event.normalizedMessage();
		String playerKey = event.playerName() == null ? "" : event.playerName().trim().toLowerCase(Locale.ROOT);
		int repeated = playerKey.isBlank()
			? 0
			: repeatedContactByPlayer.merge(playerKey, 1, Integer::sum);

		ScamRules.BehaviorPatternSet patterns = ruleConfig.behaviorPatterns();
		return new BehaviorAnalysis(
			event.rawMessage(),
			normalized,
			matches(patterns.externalPlatform(), normalized),
			matches(patterns.upfrontPayment(), normalized),
			matches(patterns.accountData(), normalized),
			matches(patterns.middlemanClaim(), normalized),
			repeated
		);
	}

	/**
	 * Clears per-player repeat contact counters.
	 */
	public void reset() {
		repeatedContactByPlayer.clear();
	}

	private static boolean matches(java.util.regex.Pattern pattern, String text) {
		if (pattern == null || text == null || text.isBlank()) {
			return false;
		}
		return pattern.matcher(text).find();
	}
}
