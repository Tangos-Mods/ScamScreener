package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.rules.ScamRules;

public interface RuleConfig {
	/**
	 * Returns regex patterns for single-message rule checks.
	 */
	ScamRules.PatternSet patterns();

	/**
	 * Returns regex patterns for behavior-level checks.
	 */
	ScamRules.BehaviorPatternSet behaviorPatterns();

	/**
	 * Whether a specific rule is enabled.
	 */
	boolean isEnabled(ScamRules.ScamRule rule);
}
