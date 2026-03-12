package eu.tango.scamscreener.config.store;

import lombok.NonNull;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Public facade for the shared single-threaded async file worker.
 */
public final class AsyncFileWorkQueue {
    private AsyncFileWorkQueue() {
    }

    /**
     * Queues one file payload write without waiting for completion.
     *
     * @param path the target path
     * @param payload the full file content to write
     */
    public static void submit(@NonNull Path path, @NonNull String payload) {
        AsyncConfigWriteQueue.getInstance().submit(path, payload);
    }

    /**
     * Queues one file payload write and waits until it has been written.
     *
     * @param path the target path
     * @param payload the full file content to write
     */
    public static void submitAndWait(@NonNull Path path, @NonNull String payload) {
        AsyncConfigWriteQueue.getInstance().submitAndWait(path, payload);
    }

    /**
     * Blocks until all earlier writes for the given path have completed.
     *
     * @param path the target path to flush
     */
    public static void flush(@NonNull Path path) {
        AsyncConfigWriteQueue.getInstance().flush(path);
    }

    /**
     * Runs one callable on the shared async file worker.
     *
     * @param task the work item to execute
     * @return a future completed with the task result
     * @param <T> the task result type
     */
    public static <T> CompletableFuture<T> submitTask(@NonNull Callable<T> task) {
        return AsyncConfigWriteQueue.getInstance().submitTask(task);
    }

    static void awaitIdleForTests() {
        AsyncConfigWriteQueue.awaitIdleForTests();
    }
}
