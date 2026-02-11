package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.ScamRulesConfig;
import eu.tango.scamscreener.pipeline.core.FunnelStore;
import eu.tango.scamscreener.pipeline.core.IntentTagger;
import eu.tango.scamscreener.pipeline.core.RuleConfig;
import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.rules.ScamRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunnelSignalStageTest {
	@Test
	void benignServiceOfferOnlyProducesNoFunnelSignal() {
		FunnelHarness harness = new FunnelHarness("Trader123");

		List<Signal> funnelSignals = harness.process("selling carry service today", List.of());

		assertTrue(funnelSignals.isEmpty());
	}

	@Test
	void discordMentionOnlyProducesNoFunnelSignal() {
		FunnelHarness harness = new FunnelHarness("Trader123");

		List<Signal> funnelSignals = harness.process("join discord for details", List.of());

		assertTrue(funnelSignals.isEmpty());
	}

	@Test
	void repRedirectInstructionFromExistingSignalsProducesHighPartialFunnelSignal() {
		FunnelHarness harness = new FunnelHarness("Trader123");
		harness.process("rep me after trade", List.of());
		harness.process("check profile", List.of(new Signal(
			ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH.name(),
			SignalSource.BEHAVIOR,
			15.0,
			"external platform behavior",
			ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH,
			List.of()
		)));

		List<Signal> funnelSignals = harness.process("type do rep @me", List.of());
		Signal signal = onlySignal(funnelSignals);

		assertEquals(ScamRulesConfig.DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT + 6.0, signal.weight(), 0.001);
		assertTrue(signal.evidence().contains("REP -> REDIRECT -> INSTRUCTION"));
	}

	@Test
	void offerRepRedirectInstructionProducesCriticalFullFunnelSignal() {
		FunnelHarness harness = new FunnelHarness("Trader123");
		harness.process("selling carry service", List.of());
		harness.process("rep me please", List.of());
		harness.process("join discord vc", List.of());

		List<Signal> funnelSignals = harness.process("go to voice channel and type do rep @me", List.of());
		Signal signal = onlySignal(funnelSignals);

		assertEquals(ScamRulesConfig.DEFAULT_FUNNEL_FULL_SEQUENCE_WEIGHT, signal.weight(), 0.001);
		assertTrue(signal.evidence().contains("OFFER -> REP -> REDIRECT -> INSTRUCTION"));
		assertEquals(4, signal.relatedMessages().size());
	}

	@Test
	void offerThenUpfrontPaymentTriggersPartialFunnelSignal() {
		FunnelHarness harness = new FunnelHarness("Trader123");
		harness.process("i have a special offer for you", List.of());

		List<Signal> funnelSignals = harness.process("you give me 100m first then i send 1b", List.of());
		Signal signal = onlySignal(funnelSignals);

		assertEquals(ScamRulesConfig.DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT, signal.weight(), 0.001);
		assertTrue(signal.evidence().contains("OFFER -> PAYMENT"));
	}

	@Test
	void guildRecruitingIntentSuppressesOfferAndFreeTags() {
		RuleConfig ruleConfig = new EnabledRuleConfig();
		IntentTagger tagger = new IntentTagger(ruleConfig);
		MessageEvent event = MessageEvent.from(
			"Recruiter",
			"guild recruiting free carry service join discord for members",
			1_000L,
			MessageContext.GENERAL,
			"public"
		);

		IntentTagger.TaggingResult tagging = tagger.tag(event, List.of());

		assertTrue(tagging.negativeContext());
		assertFalse(tagging.tags().contains(IntentTag.SERVICE_OFFER));
		assertFalse(tagging.tags().contains(IntentTag.FREE_OFFER));
	}

	@Test
	void upfrontPaymentPhraseIsTaggedWithoutExistingSignals() {
		RuleConfig ruleConfig = new EnabledRuleConfig();
		IntentTagger tagger = new IntentTagger(ruleConfig);
		MessageEvent event = MessageEvent.from(
			"Trader",
			"you give me 100m first then i send 1b",
			2_000L,
			MessageContext.GENERAL,
			"pm"
		);

		IntentTagger.TaggingResult tagging = tagger.tag(event, List.of());

		assertTrue(tagging.tags().contains(IntentTag.PAYMENT_UPFRONT));
	}

	@Test
	void legitCarryAdsWithoutRedirectOrInstructionDoNotTriggerFunnel() {
		FunnelHarness harness = new FunnelHarness("Trader123");
		harness.process("selling f7 carry service", List.of());

		List<Signal> funnelSignals = harness.process("rep me after run if legit", List.of());

		assertTrue(funnelSignals.isEmpty());
	}

	private static Signal onlySignal(List<Signal> signals) {
		assertEquals(1, signals.size());
		return signals.get(0);
	}

	private static final class FunnelHarness {
		private final RuleConfig ruleConfig = new EnabledRuleConfig();
		private final FunnelSignalStage funnelSignalStage = new FunnelSignalStage(ruleConfig, new FunnelStore(ruleConfig));
		private final String playerName;
		private long timestamp = 1_000L;

		private FunnelHarness(String playerName) {
			this.playerName = playerName;
		}

		private List<Signal> process(String rawMessage, List<Signal> existingSignals) {
			MessageEvent event = MessageEvent.from(playerName, rawMessage, timestamp, MessageContext.GENERAL, "public");
			timestamp += 10_000L;
			return funnelSignalStage.collectSignals(event, existingSignals);
		}
	}

	private static final class EnabledRuleConfig implements RuleConfig {
		private final ScamRules.PatternSet patterns = new ScamRules.PatternSet(
			Pattern.compile(ScamRulesConfig.DEFAULT_LINK_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_URGENCY_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_TOO_GOOD_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_TRUST_BAIT_PATTERN)
		);
		private final ScamRules.BehaviorPatternSet behaviorPatterns = new ScamRules.BehaviorPatternSet(
			Pattern.compile(ScamRulesConfig.DEFAULT_EXTERNAL_PLATFORM_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_PAYMENT_FIRST_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_ACCOUNT_DATA_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_MIDDLEMAN_PATTERN)
		);
		private final ScamRules.FunnelConfig funnelConfig = new ScamRules.FunnelConfig(
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_SERVICE_OFFER_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_FREE_OFFER_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_REP_REQUEST_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_PLATFORM_REDIRECT_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_INSTRUCTION_INJECTION_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_COMMUNITY_ANCHOR_PATTERN),
			Pattern.compile(ScamRulesConfig.DEFAULT_FUNNEL_NEGATIVE_INTENT_PATTERN),
			ScamRulesConfig.DEFAULT_FUNNEL_WINDOW_SIZE,
			ScamRulesConfig.DEFAULT_FUNNEL_WINDOW_MILLIS,
			ScamRulesConfig.DEFAULT_FUNNEL_CONTEXT_TTL_MILLIS,
			ScamRulesConfig.DEFAULT_FUNNEL_FULL_SEQUENCE_WEIGHT,
			ScamRulesConfig.DEFAULT_FUNNEL_PARTIAL_SEQUENCE_WEIGHT
		);

		@Override
		public ScamRules.PatternSet patterns() {
			return patterns;
		}

		@Override
		public ScamRules.BehaviorPatternSet behaviorPatterns() {
			return behaviorPatterns;
		}

		@Override
		public boolean isEnabled(ScamRules.ScamRule rule) {
			return true;
		}

		@Override
		public ScamRules.FunnelConfig funnelConfig() {
			return funnelConfig;
		}
	}
}
