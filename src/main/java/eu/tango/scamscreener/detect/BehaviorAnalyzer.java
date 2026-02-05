package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BehaviorAnalyzer {
	private final RuleConfig ruleConfig;
	private final Map<String, Integer> repeatedContactByPlayer = new HashMap<>();

	public BehaviorAnalyzer(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

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
