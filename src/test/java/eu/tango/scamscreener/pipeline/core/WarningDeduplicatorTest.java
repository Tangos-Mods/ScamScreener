package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningDeduplicatorTest {
	@Test
	void shouldWarnOnlyOncePerPlayerAndLevel() {
		WarningDeduplicator deduplicator = new WarningDeduplicator();
		MessageEvent event = MessageEvent.from("Player123", "hello", 100L, MessageContext.GENERAL, "public");

		assertTrue(deduplicator.shouldWarn(event, DetectionLevel.HIGH));
		assertFalse(deduplicator.shouldWarn(event, DetectionLevel.HIGH));
		assertTrue(deduplicator.shouldWarn(event, DetectionLevel.CRITICAL));
	}

	@Test
	void shouldWarnRejectsInvalidInputs() {
		WarningDeduplicator deduplicator = new WarningDeduplicator();
		MessageEvent blankPlayer = MessageEvent.from(" ", "hello", 100L, MessageContext.GENERAL, "public");

		assertFalse(deduplicator.shouldWarn(null, DetectionLevel.HIGH));
		assertFalse(deduplicator.shouldWarn(blankPlayer, DetectionLevel.HIGH));
		assertFalse(deduplicator.shouldWarn(
			MessageEvent.from("Player123", "hello", 100L, MessageContext.GENERAL, "public"),
			null
		));
	}

	@Test
	void resetClearsSeenEntries() {
		WarningDeduplicator deduplicator = new WarningDeduplicator();
		MessageEvent event = MessageEvent.from("Player123", "hello", 100L, MessageContext.GENERAL, "public");

		assertTrue(deduplicator.shouldWarn(event, DetectionLevel.MEDIUM));
		assertFalse(deduplicator.shouldWarn(event, DetectionLevel.MEDIUM));
		deduplicator.reset();
		assertTrue(deduplicator.shouldWarn(event, DetectionLevel.MEDIUM));
	}
}
