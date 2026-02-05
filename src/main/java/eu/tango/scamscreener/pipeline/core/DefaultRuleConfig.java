package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.rules.ScamRules;

public final class DefaultRuleConfig implements RuleConfig {
	/**
	 * Delegates rule settings to {@link ScamRules}.
	 */
	@Override
	public ScamRules.PatternSet patterns() {
		return ScamRules.patternSet();
	}

	@Override
	public ScamRules.BehaviorPatternSet behaviorPatterns() {
		return ScamRules.behaviorPatternSet();
	}

	@Override
	public boolean isEnabled(ScamRules.ScamRule rule) {
		return ScamRules.isRuleEnabled(rule);
	}
}
