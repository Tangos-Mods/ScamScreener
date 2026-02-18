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
	void parsePlayerLineParsesDecoratedPublicChat() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("[MVP+] ✫ SkyTrader: selling carries");

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
	void parsePlayerLineParsesLevelAndArrowDecoratedPublicChat() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("[293] \u00BB [MVP+] KoudeR: ah");

		assertNotNull(parsed);
		assertEquals("KoudeR", parsed.playerName());
		assertEquals("ah", parsed.message());
	}

	@Test
	void parsePlayerLineParsesCoopChannelFormat() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("Co-op > [MVP+] Trader123: hi there");

		assertNotNull(parsed);
		assertEquals("Trader123", parsed.playerName());
		assertEquals("hi there", parsed.message());
	}

	@Test
	void parsePlayerLineParsesAllChannelFormat() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("All > [MVP+] Trader123: hi there");

		assertNotNull(parsed);
		assertEquals("Trader123", parsed.playerName());
		assertEquals("hi there", parsed.message());
	}

	@Test
	void parsePlayerLineParsesArbitraryDecoratorsBetweenLevelAndRank() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("[245] :^)_abc123 [MVP+] KoudeR: alright");

		assertNotNull(parsed);
		assertEquals("KoudeR", parsed.playerName());
		assertEquals("alright", parsed.message());
	}

	@Test
	void parsePlayerLineParsesAllChannelWithArbitraryDecoratorsBetweenLevelAndRank() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("All > [245] :^)_abc123 [MVP+] KoudeR: ya rip");

		assertNotNull(parsed);
		assertEquals("KoudeR", parsed.playerName());
		assertEquals("ya rip", parsed.message());
	}

	@Test
	void parsePlayerLineRejectsKnownSystemMessages() {
		String line = "You have sent a trade request to Trader123.";

		assertTrue(ChatLineParser.isSystemLine(line));
		assertNull(ChatLineParser.parsePlayerLine(line));
	}

	@Test
	void parsePlayerLineParsesAllChannelWithLevelAndArrowDecorators() {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine("All > [293] \u00BB [MVP+] KoudeR: ya rip");

		assertNotNull(parsed);
		assertEquals("KoudeR", parsed.playerName());
		assertEquals("ya rip", parsed.message());
	}

	@Test
	void parsePlayerLineRejectsNpcLines() {
		String line = "[NPC] Banker: Welcome back";

		assertTrue(ChatLineParser.isSystemLine(line));
		assertNull(ChatLineParser.parsePlayerLine(line));
	}
}

