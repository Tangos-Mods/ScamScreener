package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleSignalStageTest {
	@Test
	void matchesConfiguredRulePattern() {
		RuleConfig config = new RuleConfig() {
			private final ScamRules.PatternSet patterns = new ScamRules.PatternSet(
				Pattern.compile("discord"),
				Pattern.compile("urgent"),
				Pattern.compile("pay first"),
				Pattern.compile("password"),
				Pattern.compile("too good"),
				Pattern.compile("trust")
			);
			private final Set<ScamRules.ScamRule> enabled = EnumSet.of(ScamRules.ScamRule.SUSPICIOUS_LINK);

			@Override
			public ScamRules.PatternSet patterns() {
				return patterns;
			}

			@Override
			public ScamRules.BehaviorPatternSet behaviorPatterns() {
				return new ScamRules.BehaviorPatternSet(
					Pattern.compile("discord"),
					Pattern.compile("pay first"),
					Pattern.compile("password"),
					Pattern.compile("middleman")
				);
			}

			@Override
			public boolean isEnabled(ScamRules.ScamRule rule) {
				return enabled.contains(rule);
			}
		};

		RuleSignalStage stage = new RuleSignalStage(config);
		MessageEvent event = MessageEvent.from("Player", "Join my discord", 1L, MessageContext.UNKNOWN, null);
		List<Signal> signals = stage.collectSignals(event);
		assertEquals(1, signals.size());
		assertEquals(ScamRules.ScamRule.SUSPICIOUS_LINK, signals.get(0).ruleId());
		assertTrue(signals.get(0).evidence().contains("discord"));
	}
}
