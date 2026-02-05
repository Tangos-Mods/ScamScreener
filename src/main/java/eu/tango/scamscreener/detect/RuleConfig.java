package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

public interface RuleConfig {
	ScamRules.PatternSet patterns();

	ScamRules.BehaviorPatternSet behaviorPatterns();

	boolean isEnabled(ScamRules.ScamRule rule);
}
