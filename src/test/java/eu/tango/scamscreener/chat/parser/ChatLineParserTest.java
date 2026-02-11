package eu.tango.scamscreener.chat.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLineParserTest {
	@Test
	void parsePlayerLineParsesDirectChatWithRankPrefix() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("[MVP+] SkyTrader: selling carries");

		assertNotNull(parsed);
		assertEquals("SkyTrader", parsed.playerName());
		assertEquals("selling carries", parsed.message());
	}

	@Test
	void parsePlayerLineParsesWhisperFormat() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("from [VIP] Trader123: hi there");

		assertNotNull(parsed);
		assertEquals("Trader123", parsed.playerName());
		assertEquals("hi there", parsed.message());
	}

	@Test
	void parsePlayerLineRejectsKnownSystemMessages() {
		String line = "You have sent a trade request to Trader123.";

		assertTrue(ChatLineParser.isSystemLine(line));
		assertNull(ChatLineParser.parsePlayerLine(line));
	}

	@Test
	void parsePlayerLineRejectsNpcLines() {
		String line = "[NPC] Banker: Welcome back";

		assertTrue(ChatLineParser.isSystemLine(line));
		assertNull(ChatLineParser.parsePlayerLine(line));
	}
}
