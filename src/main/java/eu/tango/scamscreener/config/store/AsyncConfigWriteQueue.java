package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.ScamScreenerMod;
import lombok.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Serializes config file writes onto one worker thread while coalescing newer
 * payloads for the same target path.
 */
final class AsyncConfigWriteQueue {
    private static final AsyncConfigWriteQueue INSTANCE = new AsyncConfigWriteQueue();

    private final Object lock = new Object();
    private final Map<Path, PendingWrite> pendingWrites = new LinkedHashMap<>();
    private final ArrayDeque<PendingTask<?>> pendingTasks = new ArrayDeque<>();
    private final Map<Path, Long> completedSequences = new HashMap<>();
    private final Thread workerThread;
    private PendingWrite activeWrite;
    private boolean activeTask;
    private long nextOrder = 1L;
    private long nextSequence = 1L;

    private AsyncConfigWriteQueue() {
        workerThread = new Thread(this::runLoop, "ScamScreener-ConfigWriter");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    static AsyncConfigWriteQueue getInstance() {
        return INSTANCE;
    }

    void submit(@NonNull Path path, @NonNull String payload) {
        enqueue(path, payload);
    }

    void submitAndWait(@NonNull Path path, @NonNull String payload) {
        waitForSequence(path, enqueue(path, payload));
    }

    void flush(@NonNull Path path) {
        long targetSequence;
        synchronized (lock) {
            PendingWrite pendingWrite = pendingWrites.get(path);
            if (pendingWrite != null) {
                targetSequence = pendingWrite.sequence();
            } else if (activeWrite != null && activeWrite.path().equals(path)) {
                targetSequence = activeWrite.sequence();
            } else {
                return;
            }
        }

        waitForSequence(path, targetSequence);
    }

    <T> CompletableFuture<T> submitTask(@NonNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        synchronized (lock) {
            pendingTasks.addLast(new PendingTask<>(nextOrder++, task, future));
            lock.notifyAll();
        }

        return future;
    }

    static void awaitIdleForTests() {
        INSTANCE.waitUntilIdle();
    }

    private long enqueue(Path path, String payload) {
        synchronized (lock) {
            long sequence = nextSequence++;
            pendingWrites.remove(path);
            pendingWrites.put(path, new PendingWrite(path, payload, nextOrder++, sequence));
            lock.notifyAll();
            return sequence;
        }
    }

    private void waitForSequence(Path path, long targetSequence) {
        synchronized (lock) {
            while (completedSequences.getOrDefault(path, 0L) < targetSequence) {
                try {
                    lock.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    ScamScreenerMod.LOGGER.warn(
                        "Interrupted while waiting for config write to {} to finish.",
                        path,
                        exception
                    );
                    return;
                }
            }
        }
    }

    private void waitUntilIdle() {
        synchronized (lock) {
            while (activeWrite != null || activeTask || !pendingWrites.isEmpty() || !pendingTasks.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for async config writer to become idle.", exception);
                }
            }
        }
    }

    private void runLoop() {
        while (true) {
            QueueWork work = takeNextWork();
            work.run();
        }
    }

    private QueueWork takeNextWork() {
        synchronized (lock) {
            while (pendingWrites.isEmpty() && pendingTasks.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException exception) {
                    ScamScreenerMod.LOGGER.warn("Async config writer interrupted while waiting for work.", exception);
                }
            }

            PendingWrite nextWrite = pendingWrites.isEmpty() ? null : pendingWrites.values().iterator().next();
            PendingTask<?> nextTask = pendingTasks.peekFirst();
            if (nextWrite != null && (nextTask == null || nextWrite.order() < nextTask.order())) {
                pendingWrites.remove(nextWrite.path());
                activeWrite = nextWrite;
                return () -> writePayload(nextWrite);
            }

            PendingTask<?> task = pendingTasks.removeFirst();
            activeTask = true;
            return () -> runTask(task);
        }
    }

    private void writePayload(PendingWrite write) {
        try {
            Path parent = write.path().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(write.path(), write.payload(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            ScamScreenerMod.LOGGER.error("Failed to write config to {}.", write.path(), exception);
        } finally {
            synchronized (lock) {
                completedSequences.put(write.path(), write.sequence());
                activeWrite = null;
                lock.notifyAll();
            }
        }
    }

    private <T> void runTask(PendingTask<T> task) {
        try {
            task.future().complete(task.callable().call());
        } catch (Exception exception) {
            task.future().completeExceptionally(exception);
        } finally {
            synchronized (lock) {
                activeTask = false;
                lock.notifyAll();
            }
        }
    }

    private interface QueueWork {
        void run();
    }

    private record PendingWrite(Path path, String payload, long order, long sequence) {
    }

    private record PendingTask<T>(long order, Callable<T> callable, CompletableFuture<T> future) {
    }
}
