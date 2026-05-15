package eu.tango.scamscreener.training;

import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingHubUploadWorkerTest {
    @TempDir
    Path tempDir;

    @Test
    void uploadsWithoutLoginSession() throws Exception {
        List<String> replies = new CopyOnWriteArrayList<>();
        AtomicInteger uploadCalls = new AtomicInteger();
        TrainingHubUploadWorker worker = createWorker(
            new TrainingCaseExportService("client-anon"),
            List.of(reviewedEntry("review-1", ReviewVerdict.RISK, 1_000L)),
            (trainingClientId, trainingCasesFile) -> {
                uploadCalls.incrementAndGet();
                assertEquals("client-anon", trainingClientId);
                return CompletableFuture.completedFuture(new TrainingHubClient.UploadResult("accepted", 9L, 1, 1, 0, "", "abc"));
            },
            replies
        );

        try {
            assertTrue(worker.startUpload());
            waitUntilIdle(worker);
        } finally {
            worker.shutdown();
        }

        assertEquals(1, uploadCalls.get());
        assertFalse(worker.isRunning());
        assertTrue(replies.stream().anyMatch(message -> message.contains("Training upload started")));
        assertTrue(replies.stream().anyMatch(message -> message.contains("Training upload accepted: 1 cases")));
    }

    @Test
    void stopsWithoutRetryOnUploadRejection() throws Exception {
        List<String> replies = new CopyOnWriteArrayList<>();
        AtomicInteger uploadCalls = new AtomicInteger();
        TrainingHubUploadWorker worker = createWorker(
            new TrainingCaseExportService("client-rejected"),
            List.of(reviewedEntry("review-2", ReviewVerdict.RISK, 2_000L)),
            (trainingClientId, trainingCasesFile) -> {
                uploadCalls.incrementAndGet();
                return CompletableFuture.failedFuture(new TrainingHubClient.UploadRejectedException("Invalid schema."));
            },
            replies
        );

        try {
            assertTrue(worker.startUpload());
            waitUntilIdle(worker);
        } finally {
            worker.shutdown();
        }

        assertEquals(1, uploadCalls.get());
        assertTrue(replies.stream().anyMatch(message -> message.contains("Training upload aborted: Invalid schema.")));
        assertFalse(replies.stream().anyMatch(message -> message.contains("Retrying automatically")));
    }

    @Test
    void retriesTransientFailures() throws Exception {
        List<String> replies = new CopyOnWriteArrayList<>();
        AtomicInteger uploadCalls = new AtomicInteger();
        TrainingCaseExportService exportService = new TrainingCaseExportService("client-retry");
        RuntimeConfig config = new RuntimeConfig();
        config.setTrainingUploadRetryCount(1);
        config.setTrainingUploadRetryDelaySeconds(1);
        ScheduledExecutorService executor = daemonExecutor("ScamScreener-TrainingUpload-Test-Retry");
        TrainingHubUploadWorker worker = new TrainingHubUploadWorker(
            exportService,
            () -> List.of(reviewedEntry("review-3", ReviewVerdict.SAFE, 3_000L)),
            () -> config,
            executor,
            (service, reviewEntries) -> service.exportReviewedCases(reviewEntries, tempDir.resolve("training-cases-v2-retry.jsonl")),
            (trainingClientId, trainingCasesFile) -> {
                if (uploadCalls.getAndIncrement() == 0) {
                    return CompletableFuture.failedFuture(new IllegalStateException("network timeout"));
                }

                return CompletableFuture.completedFuture(new TrainingHubClient.UploadResult("accepted", 11L, 1, 1, 0, "", "sha"));
            },
            text -> replies.add(text.getString())
        );

        try {
            assertTrue(worker.startUpload());
            waitUntilIdle(worker);
        } finally {
            worker.shutdown();
        }

        assertEquals(2, uploadCalls.get());
        assertTrue(replies.stream().anyMatch(message -> message.contains("Retrying automatically 1 more time with 1s between attempts.")));
        assertTrue(replies.stream().anyMatch(message -> message.contains("Training upload accepted: 1 cases")));
    }

    @Test
    void abortsWhenNoReviewedCasesAreExportable() throws Exception {
        List<String> replies = new CopyOnWriteArrayList<>();
        AtomicInteger uploadCalls = new AtomicInteger();
        TrainingHubUploadWorker worker = createWorker(
            new TrainingCaseExportService("client-empty"),
            List.of(reviewedEntry("review-4", ReviewVerdict.PENDING, 4_000L)),
            (trainingClientId, trainingCasesFile) -> {
                uploadCalls.incrementAndGet();
                return CompletableFuture.completedFuture(new TrainingHubClient.UploadResult("accepted", 0L, 0, 0, 0, "", ""));
            },
            replies
        );

        try {
            assertTrue(worker.startUpload());
            waitUntilIdle(worker);
        } finally {
            worker.shutdown();
        }

        assertEquals(0, uploadCalls.get());
        assertTrue(replies.stream().anyMatch(message -> message.contains("Training upload aborted: No reviewed SAFE/RISK cases are available for upload.")));
    }

    private TrainingHubUploadWorker createWorker(
        TrainingCaseExportService exportService,
        List<ReviewEntry> reviewEntries,
        TrainingHubUploadWorker.UploadAction uploadAction,
        List<String> replies
    ) {
        return new TrainingHubUploadWorker(
            exportService,
            () -> reviewEntries,
            RuntimeConfig::new,
            daemonExecutor("ScamScreener-TrainingUpload-Test"),
            (service, entries) -> service.exportReviewedCases(entries, tempDir.resolve("training-cases-v2-" + exportService.trainingClientId() + ".jsonl")),
            uploadAction,
            text -> replies.add(text.getString())
        );
    }

    private static ScheduledExecutorService daemonExecutor(String threadName) {
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    private static void waitUntilIdle(TrainingHubUploadWorker worker) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (worker.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25L);
        }

        assertFalse(worker.isRunning(), "worker should have finished");
    }

    private static ReviewEntry reviewedEntry(String id, ReviewVerdict verdict, long capturedAtMs) {
        ReviewEntry entry = new ReviewEntry(
            id,
            null,
            "Alpha",
            "[12] Alpha: add me on discord",
            25,
            "RuleStage",
            capturedAtMs,
            List.of("RULE_MATCH"),
            List.of(StageResult.score("RuleStage", 20, "External platform push: \"discord\"")),
            List.of(new ReviewCaseMessage(
                0,
                "other",
                "player",
                "Alpha add me on discord",
                true,
                ReviewCaseRole.SIGNAL,
                List.of("external_platform"),
                List.of("RuleStage - External platform push: \"discord\"")
            ))
        );
        entry.setVerdict(verdict);
        return entry;
    }
}
