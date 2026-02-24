package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendStoreTest {
	@Test
	void prunesExpiredPlayerStateWhenMapGrows() throws Exception {
		TrendStore store = new TrendStore();
		long base = 1_000_000L;

		store.evaluate(event("old-player", base), List.of());
		store.evaluate(event("active-player", base + 700_000L), List.of());

		assertTrue(historyByPlayer(store).size() <= 1);
	}

	@Test
	void prunesExpiredPlayerStateOnPeriodicCleanup() throws Exception {
		TrendStore store = new TrendStore();
		long base = 2_000_000L;

		store.evaluate(event("stale-player", base), List.of());
		store.evaluate(event("active-player", base + 1), List.of());
		for (int i = 0; i < 64; i++) {
			store.evaluate(event("active-player", base + 700_000L + i), List.of());
		}

		assertTrue(historyByPlayer(store).size() <= 1);
	}

	@Test
	void enforcesGlobalPlayerLimit() throws Exception {
		TrendStore store = new TrendStore();
		long base = 3_000_000L;
		for (int i = 0; i < 1_400; i++) {
			store.evaluate(event("player-" + i, base + i), List.of());
		}

		assertTrue(historyByPlayer(store).size() <= 1_024);
	}

	private static MessageEvent event(String player, long timestamp) {
		return MessageEvent.from(player, "message", timestamp, MessageContext.UNKNOWN, "public");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ?> historyByPlayer(TrendStore store) throws Exception {
		Field field = TrendStore.class.getDeclaredField("historyByPlayer");
		field.setAccessible(true);
		return (Map<String, ?>) field.get(store);
	}
}
