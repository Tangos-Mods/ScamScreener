package eu.tango.scamscreener.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageEventParserTest {
	@Test
	void parsesAndNormalizesPlayerMessage() {
		MessageEvent event = MessageEventParser.parse("Player123:  Hello THERE ", 123L);
		assertEquals("Player123", event.playerName());
		assertEquals("Hello THERE", event.rawMessage());
		assertEquals("hello there", event.normalizedMessage());
		assertEquals(123L, event.timestampMs());
	}

	@Test
	void ignoresNonPlayerLines() {
		assertNull(MessageEventParser.parse("[ScamScreener] testing", 1L));
		assertNull(MessageEventParser.parse("System: ", 1L));
	}
}
