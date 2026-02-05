package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendStoreTest {
	@Test
	void emitsTrendSignalAfterBurst() {
		RuleConfig config = new RuleConfig() {
			@Override
			public ScamRules.PatternSet patterns() {
				return new ScamRules.PatternSet(
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x")
				);
			}

			@Override
			public ScamRules.BehaviorPatternSet behaviorPatterns() {
				return new ScamRules.BehaviorPatternSet(
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x"),
					Pattern.compile("x")
				);
			}

			@Override
			public boolean isEnabled(ScamRules.ScamRule rule) {
				return true;
			}
		};

		TrendStore trendStore = new TrendStore();
		TrendSignalStage stage = new TrendSignalStage(config, trendStore);
		List<Signal> baseSignals = List.of(new Signal(
			"LINK",
			SignalSource.RULE,
			15,
			"match",
			ScamRules.ScamRule.SUSPICIOUS_LINK,
			List.of()
		));

		MessageEvent first = MessageEvent.from("Player", "msg1", 1L, MessageContext.UNKNOWN, null);
		MessageEvent second = MessageEvent.from("Player", "msg2", 2L, MessageContext.UNKNOWN, null);
		MessageEvent third = MessageEvent.from("Player", "msg3", 3L, MessageContext.UNKNOWN, null);

		assertTrue(stage.collectSignals(first, baseSignals).isEmpty());
		assertTrue(stage.collectSignals(second, baseSignals).isEmpty());
		List<Signal> signals = stage.collectSignals(third, baseSignals);
		assertEquals(1, signals.size());
		assertEquals(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN, signals.get(0).ruleId());
	}
}
