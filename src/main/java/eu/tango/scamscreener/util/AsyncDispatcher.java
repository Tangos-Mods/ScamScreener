package eu.tango.scamscreener.util;

import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class AsyncDispatcher {
	private static final int BACKGROUND_THREADS = 2;
	private static final int IO_THREADS = 2;
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 3L;

	private static final Object LOCK = new Object();

	private static ExecutorService backgroundExecutor;
	private static ExecutorService ioExecutor;
	private static ScheduledExecutorService scheduledExecutor;
	private static boolean initialized;

	private AsyncDispatcher() {
	}

	public static void init() {
		synchronized (LOCK) {
			if (initialized) {
				return;
			}
			initLocked();
		}
	}

	public static void shutdown() {
		ExecutorService backgroundToShutdown;
		ExecutorService ioToShutdown;
		ScheduledExecutorService scheduledToShutdown;

		synchronized (LOCK) {
			if (!initialized) {
				return;
			}
			backgroundToShutdown = backgroundExecutor;
			ioToShutdown = ioExecutor;
			scheduledToShutdown = scheduledExecutor;
			backgroundExecutor = null;
			ioExecutor = null;
			scheduledExecutor = null;
			initialized = false;
		}

		shutdownExecutor(backgroundToShutdown);
		shutdownExecutor(ioToShutdown);
		shutdownExecutor(scheduledToShutdown);
	}

	public static CompletableFuture<Void> runBackground(Runnable task) {
		Objects.requireNonNull(task, "task");
		return CompletableFuture.runAsync(task, backgroundExecutor());
	}

	public static <T> CompletableFuture<T> supplyBackground(Supplier<T> task) {
		Objects.requireNonNull(task, "task");
		return CompletableFuture.supplyAsync(task, backgroundExecutor());
	}

	public static CompletableFuture<Void> runIo(Runnable task) {
		Objects.requireNonNull(task, "task");
		return CompletableFuture.runAsync(task, ioExecutor());
	}

	public static <T> CompletableFuture<T> supplyIo(Supplier<T> task) {
		Objects.requireNonNull(task, "task");
		return CompletableFuture.supplyAsync(task, ioExecutor());
	}

	public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(unit, "unit");
		return scheduledExecutor().schedule(task, Math.max(0L, delay), unit);
	}

	public static void onClient(Minecraft client, Runnable task) {
		if (client == null || task == null) {
			return;
		}
		client.execute(task);
	}

	private static ExecutorService backgroundExecutor() {
		synchronized (LOCK) {
			if (!initialized) {
				initLocked();
			}
			return backgroundExecutor;
		}
	}

	private static ExecutorService ioExecutor() {
		synchronized (LOCK) {
			if (!initialized) {
				initLocked();
			}
			return ioExecutor;
		}
	}

	private static ScheduledExecutorService scheduledExecutor() {
		synchronized (LOCK) {
			if (!initialized) {
				initLocked();
			}
			return scheduledExecutor;
		}
	}

	private static void initLocked() {
		backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_THREADS, namedDaemonFactory("scamscreener-bg"));
		ioExecutor = Executors.newFixedThreadPool(IO_THREADS, namedDaemonFactory("scamscreener-io"));
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor(namedDaemonFactory("scamscreener-scheduled"));
		initialized = true;
	}

	private static ThreadFactory namedDaemonFactory(String prefix) {
		AtomicInteger counter = new AtomicInteger(1);
		return runnable -> {
			Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
	}

	private static void shutdownExecutor(ExecutorService executor) {
		if (executor == null) {
			return;
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
