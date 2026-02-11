package eu.tango.scamscreener.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyBypassStoreTest {
	private SafetyBypassStore store;

	@BeforeEach
	void setUp() {
		store = new SafetyBypassStore(SafetyBypassStore.Kind.EMAIL, Pattern.compile("secret", Pattern.CASE_INSENSITIVE));
	}

	@Test
	void blockIfMatchCreatesPendingAndTakePendingConsumesIt() {
		SafetyBypassStore.BlockResult blocked = store.blockIfMatch("send me secret now", true);

		assertNotNull(blocked);
		SafetyBypassStore.Pending pending = store.takePending(blocked.id());
		assertNotNull(pending);
		assertEquals(SafetyBypassStore.Kind.EMAIL, pending.kind());
		assertTrue(pending.command());
		assertEquals("send me secret now", pending.message());
		assertNull(store.takePending(blocked.id()));
	}

	@Test
	void allowOnceIsSingleUseAndScopedByCommandFlag() {
		store.allowOnce("hello", false);
		assertTrue(store.consumeAllowOnce("hello", false));
		assertFalse(store.consumeAllowOnce("hello", false));

		store.allowOnce("coopadd Player123", true);
		assertFalse(store.consumeAllowOnce("coopadd Player123", false));
		assertTrue(store.consumeAllowOnce("coopadd Player123", true));
		assertFalse(store.consumeAllowOnce("coopadd Player123", true));
	}

	@Test
	void allowOnceQueueDropsOldestEntries() {
		for (int i = 1; i <= 6; i++) {
			store.allowOnce("msg" + i, false);
		}

		assertFalse(store.consumeAllowOnce("msg1", false));
		for (int i = 2; i <= 6; i++) {
			assertTrue(store.consumeAllowOnce("msg" + i, false));
		}
	}
}
