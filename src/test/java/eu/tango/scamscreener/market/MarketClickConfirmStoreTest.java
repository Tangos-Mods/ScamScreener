package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketClickConfirmStoreTest {
	@Test
	void blocksUntilRequiredClicksReached() {
		MarketClickConfirmStore store = new MarketClickConfirmStore();
		long now = 1_000L;

		MarketClickConfirmStore.Decision first = store.registerAttempt("fingerprint", now, 3, 8_000L);
		MarketClickConfirmStore.Decision second = store.registerAttempt("fingerprint", now + 500L, 3, 8_000L);
		MarketClickConfirmStore.Decision third = store.registerAttempt("fingerprint", now + 900L, 3, 8_000L);

		assertFalse(first.allow());
		assertFalse(second.allow());
		assertTrue(third.allow());
	}

	@Test
	void timeoutResetsProgress() {
		MarketClickConfirmStore store = new MarketClickConfirmStore();
		long now = 5_000L;

		MarketClickConfirmStore.Decision first = store.registerAttempt("fingerprint", now, 3, 1_000L);
		MarketClickConfirmStore.Decision afterTimeout = store.registerAttempt("fingerprint", now + 5_000L, 3, 1_000L);

		assertFalse(first.allow());
		assertFalse(afterTimeout.allow());
		assertTrue(afterTimeout.currentCount() <= 1);
	}
}

