package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageEventParserTest {
	@Test
	void parseDetectsPartyChannel() {
		MessageEvent event = MessageEventParser.parse("Party > Player123: hello", 100L);

		assertNotNull(event);
		assertEquals(MessageContext.PARTY, event.context());
		assertEquals("party", event.channel());
	}

	@Test
	void parseDetectsTeamChannel() {
		MessageEvent event = MessageEventParser.parse("Guild > Player123: hello", 100L);

		assertNotNull(event);
		assertEquals(MessageContext.TEAM, event.context());
		assertEquals("team", event.channel());
	}

	@Test
	void parseDetectsPmChannel() {
		MessageEvent event = MessageEventParser.parse("from Player123: hello", 100L);

		assertNotNull(event);
		assertEquals(MessageContext.GENERAL, event.context());
		assertEquals("pm", event.channel());
	}

	@Test
	void parseDetectsPublicChannel() {
		MessageEvent event = MessageEventParser.parse("Player123: hello", 100L);

		assertNotNull(event);
		assertEquals(MessageContext.GENERAL, event.context());
		assertEquals("public", event.channel());
	}

	@Test
	void parseReturnsNullForSystemLines() {
		MessageEvent event = MessageEventParser.parse("You have sent a trade request to Trader123.", 100L);

		assertNull(event);
	}
}
