package eu.tango.scamscreener.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoredChatMessagesTest {
	@Test
	void isSystemLineRecognizesNpcPrefixAndKnownSystemPatterns() {
		assertTrue(IgnoredChatMessages.isSystemLine("[NPC] Banker: Welcome back"));
		assertTrue(IgnoredChatMessages.isSystemLine("[SECURITY] This account is compromised."));
		assertTrue(IgnoredChatMessages.isSystemLine("[HYPIXEL] Scheduled maintenance soon."));
		assertTrue(IgnoredChatMessages.isSystemLine("You have sent a trade request to Trader123."));
		assertFalse(IgnoredChatMessages.isSystemLine("Player123: hello there"));
	}

	@Test
	void isMuteExemptLineRecognizesOwnWarningMessages() {
		assertTrue(IgnoredChatMessages.isMuteExemptLine("[ScamScreener] test"));
		assertTrue(IgnoredChatMessages.isMuteExemptLine("===================================="));
		assertTrue(IgnoredChatMessages.isMuteExemptLine("RISKY MESSAGE"));
		assertFalse(IgnoredChatMessages.isMuteExemptLine("Player123: normal line"));
	}
}
