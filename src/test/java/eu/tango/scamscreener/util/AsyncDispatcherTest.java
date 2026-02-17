package eu.tango.scamscreener.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncDispatcherTest {
	@AfterEach
	void cleanup() {
		AsyncDispatcher.shutdown();
	}

	@Test
	void initAndShutdownAreIdempotent() {
		AsyncDispatcher.init();
		AsyncDispatcher.init();
		AsyncDispatcher.shutdown();
		AsyncDispatcher.shutdown();
	}

	@Test
	void supplyBackgroundReturnsValue() throws Exception {
		AsyncDispatcher.init();
		Integer result = AsyncDispatcher.supplyBackground(() -> 42).get(2, TimeUnit.SECONDS);
		assertEquals(42, result);
	}

	@Test
	void runIoExecutesTask() throws Exception {
		AsyncDispatcher.init();
		CountDownLatch latch = new CountDownLatch(1);
		AsyncDispatcher.runIo(latch::countDown);
		assertTrue(latch.await(2, TimeUnit.SECONDS));
	}

	@Test
	void scheduleExecutesTask() throws Exception {
		AsyncDispatcher.init();
		CountDownLatch latch = new CountDownLatch(1);
		AsyncDispatcher.schedule(latch::countDown, 20L, TimeUnit.MILLISECONDS);
		assertTrue(latch.await(2, TimeUnit.SECONDS));
	}
}
