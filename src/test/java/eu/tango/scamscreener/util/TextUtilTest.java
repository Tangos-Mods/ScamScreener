package eu.tango.scamscreener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextUtilTest {
	@Test
	void normalizeForMatchLowercasesAndStripsPunctuation() {
		assertEquals("hello sky block 42", TextUtil.normalizeForMatch("  HeLLo, Sky-Block!!! 42 "));
	}

	@Test
	void normalizeCommandStripsLeadingSlashOnlyForCommands() {
		assertEquals("msg player hi", TextUtil.normalizeCommand("/msg player hi", true));
		assertEquals("/msg player hi", TextUtil.normalizeCommand("/msg player hi", false));
	}

	@Test
	void anonymizeForAiReplacesMentionsAndCommandTargets() {
		String input = "§aHey @Trader123 do /msg VictimUser now";

		assertEquals("Hey @player do /msg player now", TextUtil.anonymizeForAi(input, "Trader123"));
	}

	@Test
	void anonymizeForAiKeepsMixedNameTokensWithoutHint() {
		String input = "abc XxTrade_99";

		assertEquals("abc XxTrade_99", TextUtil.anonymizeForAi(input, null));
	}

	@Test
	void anonymizeForAiReplacesSpeakerHintWhenProvided() {
		String input = "abc XxTrade_99";

		assertEquals("abc player", TextUtil.anonymizeForAi(input, "XxTrade_99"));
	}

	@Test
	void anonymizedSpeakerKeyIsCaseInsensitiveAndStable() {
		String upper = TextUtil.anonymizedSpeakerKey("SkyTrader");
		String lower = TextUtil.anonymizedSpeakerKey("skytrader");

		assertEquals(upper, lower);
		assertTrue(upper.startsWith("speaker-"));
		assertEquals(24, upper.length());
		assertEquals("speaker-unknown", TextUtil.anonymizedSpeakerKey(" "));
	}
}
