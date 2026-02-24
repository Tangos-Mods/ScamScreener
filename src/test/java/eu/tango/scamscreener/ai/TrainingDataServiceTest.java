package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDataServiceTest {
	@Test
	void constructorCapsCapturedChatCacheSize() {
		TrainingDataService service = new TrainingDataService(10_000);
		assertEquals(500, service.maxCapturedChatLines());
	}

	@Test
	void recentAndPendingCachesAreBoundedByConfiguredLimit() {
		TrainingDataService service = new TrainingDataService(50);
		long base = 1_000_000L;
		for (int i = 0; i < 80; i++) {
			MessageEvent event = MessageEvent.from("player", "msg-" + i, base + i, MessageContext.UNKNOWN, "public");
			service.recordChatEvent(event, 0);
		}

		assertEquals(50, service.recentCaptured(100).size());
		assertEquals(50, service.recentPendingCaptured(100).size());
	}

	@Test
	void playerStateCleanupRemovesExpiredEntries() throws Exception {
		TrainingDataService service = new TrainingDataService(50);
		Method computeDelta = TrainingDataService.class.getDeclaredMethod("computeDelta", String.class, long.class);
		Method updateRepeated = TrainingDataService.class.getDeclaredMethod("updateRepeatedContact", String.class, long.class);
		Method cleanupState = TrainingDataService.class.getDeclaredMethod("cleanupPlayerStateIfNeeded", long.class);
		computeDelta.setAccessible(true);
		updateRepeated.setAccessible(true);
		cleanupState.setAccessible(true);

		computeDelta.invoke(service, "speaker-old", 1_000_000L);
		updateRepeated.invoke(service, "speaker-old", 1_000_000L);
		Field capturesSinceCleanup = TrainingDataService.class.getDeclaredField("capturesSinceCleanup");
		capturesSinceCleanup.setAccessible(true);
		capturesSinceCleanup.setInt(service, 127);

		cleanupState.invoke(service, 1_700_000L);

		assertTrue(mapField(service, "lastTimestampByPlayer").isEmpty());
		assertTrue(mapField(service, "repeatedContactByPlayer").isEmpty());
		assertTrue(mapField(service, "lastSeenByPlayer").isEmpty());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, ?> mapField(TrainingDataService service, String name) throws Exception {
		Field field = TrainingDataService.class.getDeclaredField(name);
		field.setAccessible(true);
		return (Map<String, ?>) field.get(service);
	}
}
