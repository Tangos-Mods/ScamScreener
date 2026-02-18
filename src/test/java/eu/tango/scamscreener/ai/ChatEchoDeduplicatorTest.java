package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEchoDeduplicatorTest {
	@Test
	void consumeIncomingEchoMatchesRememberedOutgoing() {
		ChatEchoDeduplicator deduplicator = new ChatEchoDeduplicator(30_000L);

		deduplicator.rememberOutgoing("KoudeR", "ah", "public", 1_000L);

		assertTrue(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 1_200L));
		assertFalse(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 1_300L));
	}

	@Test
	void consumeIncomingEchoDoesNotMatchDifferentSpeaker() {
		ChatEchoDeduplicator deduplicator = new ChatEchoDeduplicator(30_000L);

		deduplicator.rememberOutgoing("KoudeR", "ah", "public", 1_000L);

		assertFalse(deduplicator.consumeIncomingEcho("Trader123", "ah", "public", 1_200L));
	}

	@Test
	void consumeIncomingEchoDoesNotMatchExpiredOutgoing() {
		ChatEchoDeduplicator deduplicator = new ChatEchoDeduplicator(5_000L);

		deduplicator.rememberOutgoing("KoudeR", "ah", "public", 1_000L);

		assertFalse(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 7_000L));
	}

	@Test
	void consumeIncomingEchoTracksMultipleEqualMessages() {
		ChatEchoDeduplicator deduplicator = new ChatEchoDeduplicator(30_000L);

		deduplicator.rememberOutgoing("KoudeR", "ah", "public", 1_000L);
		deduplicator.rememberOutgoing("KoudeR", "ah", "public", 2_000L);

		assertTrue(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 2_100L));
		assertTrue(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 2_200L));
		assertFalse(deduplicator.consumeIncomingEcho("KoudeR", "ah", "public", 2_300L));
	}
}
