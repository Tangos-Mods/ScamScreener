package eu.tango.scamscreener.chat.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OutgoingChatCommandParserTest {
	@Test
	void parseParsesPmMessageCommand() {
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse("/msg Trader123 hi there");

		assertNotNull(parsed);
		assertEquals("pm", parsed.channel());
		assertEquals("hi there", parsed.message());
	}

	@Test
	void parseParsesPartyChatCommand() {
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse("pc selling carries");

		assertNotNull(parsed);
		assertEquals("party", parsed.channel());
		assertEquals("selling carries", parsed.message());
	}

	@Test
	void parseParsesGuildChatCommand() {
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse("guild chat meet at hub");

		assertNotNull(parsed);
		assertEquals("team", parsed.channel());
		assertEquals("meet at hub", parsed.message());
	}

	@Test
	void parseParsesCoopChatCommand() {
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse("/cc meet on island");

		assertNotNull(parsed);
		assertEquals("team", parsed.channel());
		assertEquals("meet on island", parsed.message());
	}

	@Test
	void parseParsesReplyCommand() {
		OutgoingChatCommandParser.ParsedOutgoingChat parsed = OutgoingChatCommandParser.parse("/r sounds good");

		assertNotNull(parsed);
		assertEquals("pm", parsed.channel());
		assertEquals("sounds good", parsed.message());
	}

	@Test
	void parseReturnsNullForNonChatCommands() {
		assertNull(OutgoingChatCommandParser.parse("/coopadd Trader123"));
		assertNull(OutgoingChatCommandParser.parse("/msg Trader123"));
	}
}
