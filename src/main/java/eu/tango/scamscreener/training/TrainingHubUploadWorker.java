package eu.tango.scamscreener.training;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.review.ReviewEntry;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs Training Hub exports and uploads on one background worker with retry support.
 */
public final class TrainingHubUploadWorker {
    private final TrainingCaseExportService exportService;
    private final Supplier<List<ReviewEntry>> reviewEntriesSupplier;
    private final Supplier<RuntimeConfig> configSupplier;
    private final ScheduledExecutorService executor;
    private final ExportAction exportAction;
    private final UploadAction uploadAction;
    private final Consumer<Component> replyAction;
    private volatile UploadJob activeJob;

    public TrainingHubUploadWorker(
        TrainingCaseExportService exportService,
        Supplier<List<ReviewEntry>> reviewEntriesSupplier,
        Supplier<RuntimeConfig> configSupplier
    ) {
        this(
            exportService,
            reviewEntriesSupplier,
            configSupplier,
            createExecutor(),
            (service, reviewEntries) -> service.exportReviewedCases(reviewEntries),
            TrainingHubClient::uploadTrainingDataAsync,
            MessageDispatcher::reply
        );
    }

    TrainingHubUploadWorker(
        TrainingCaseExportService exportService,
        Supplier<List<ReviewEntry>> reviewEntriesSupplier,
        Supplier<RuntimeConfig> configSupplier,
        ScheduledExecutorService executor,
        ExportAction exportAction,
        UploadAction uploadAction,
        Consumer<Component> replyAction
    ) {
        this.exportService = exportService;
        this.reviewEntriesSupplier = reviewEntriesSupplier;
        this.configSupplier = configSupplier;
        this.executor = executor;
        this.exportAction = exportAction;
        this.uploadAction = uploadAction;
        this.replyAction = replyAction;
    }

    /**
     * Returns whether a background upload job is already running.
     *
     * @return {@code true} when an upload worker is active
     */
    public synchronized boolean isRunning() {
        return activeJob != null;
    }

    /**
     * Starts one background Training Hub upload job when none is active.
     *
     * @return {@code true} when a new job was started
     */
    public synchronized boolean startUpload() {
        if (activeJob != null) {
            return false;
        }

        RuntimeConfig config = currentConfig();
        UploadJob job = new UploadJob(
            config.trainingUploadRetryCount(),
            config.trainingUploadRetryDelaySeconds()
        );
        activeJob = job;
        executor.execute(() -> exportAndUpload(job));
        return true;
    }

    private void exportAndUpload(UploadJob job) {
        try {
            TrainingCaseExportService.TrainingCaseExportResult exportResult = exportAction.export(exportService, reviewEntriesSupplier.get());
            if (exportResult == null || exportResult.exportedCaseCount() <= 0 || exportResult.trainingCasesFile() == null) {
                abort(job, "No reviewed SAFE/RISK cases are available for upload.");
                return;
            }

            replyAction.accept(ClientMessages.trainingUploadStarted(exportResult.exportedCaseCount()));
            attemptUpload(job, exportResult.trainingCasesFile(), 1);
        } catch (RuntimeException exception) {
            ScamScreenerMod.LOGGER.warn("Training upload export failed.", exception);
            abort(job, rootCauseMessage(exception));
        }
    }

    private void attemptUpload(UploadJob job, Path trainingCasesFile, int attemptNumber) {
        if (!isActive(job)) {
            return;
        }

        String trainingClientId = exportService.trainingClientId();
        if (trainingClientId == null || trainingClientId.isBlank()) {
            abort(job, "Training client ID is unavailable.");
            return;
        }

        try {
            TrainingHubClient.UploadResult uploadResult = uploadAction.upload(trainingClientId, trainingCasesFile).join();
            replyAction.accept(ClientMessages.trainingUploadCompleted(uploadResult));
            finish(job);
        } catch (CompletionException exception) {
            handleUploadFailure(job, trainingCasesFile, attemptNumber, rootCause(exception));
        } catch (RuntimeException exception) {
            handleUploadFailure(job, trainingCasesFile, attemptNumber, rootCause(exception));
        }
    }

    private void handleUploadFailure(UploadJob job, Path trainingCasesFile, int attemptNumber, Throwable failure) {
        if (!isActive(job)) {
            return;
        }

        Throwable rootCause = failure == null ? new IllegalStateException("unknown error") : failure;
        ScamScreenerMod.LOGGER.warn(
            "Training upload attempt {}/{} failed.",
            attemptNumber,
            job.totalAttempts(),
            rootCause
        );

        if (rootCause instanceof TrainingHubClient.UploadRejectedException) {
            abort(job, rootCauseMessage(rootCause));
            return;
        }

        int retriesRemaining = job.maxRetries() - (attemptNumber - 1);
        if (retriesRemaining <= 0) {
            abort(job, rootCauseMessage(rootCause));
            return;
        }

        replyAction.accept(ClientMessages.trainingUploadRetryScheduled(
            rootCauseMessage(rootCause),
            retriesRemaining,
            job.retryDelaySeconds()
        ));
        executor.schedule(
            () -> attemptUpload(job, trainingCasesFile, attemptNumber + 1),
            job.retryDelaySeconds(),
            TimeUnit.SECONDS
        );
    }

    private void abort(UploadJob job, String message) {
        replyAction.accept(ClientMessages.trainingUploadAborted(message));
        finish(job);
    }

    private synchronized void finish(UploadJob job) {
        if (activeJob == job) {
            activeJob = null;
        }
    }

    private synchronized boolean isActive(UploadJob job) {
        return activeJob == job;
    }

    private RuntimeConfig currentConfig() {
        RuntimeConfig config = configSupplier.get();
        return config == null ? new RuntimeConfig() : config;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause instanceof CompletionException && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause;
    }

    private static String rootCauseMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }

    void shutdown() {
        executor.shutdownNow();
    }

    private static ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread workerThread = new Thread(task, "ScamScreener-TrainingUpload");
            workerThread.setDaemon(true);
            return workerThread;
        });
    }

    private record UploadJob(
        int maxRetries,
        int retryDelaySeconds
    ) {
        private int totalAttempts() {
            return maxRetries + 1;
        }
    }

    @FunctionalInterface
    interface UploadAction {
        java.util.concurrent.CompletableFuture<TrainingHubClient.UploadResult> upload(String trainingClientId, Path trainingCasesFile);
    }

    @FunctionalInterface
    interface ExportAction {
        TrainingCaseExportService.TrainingCaseExportResult export(TrainingCaseExportService exportService, Iterable<ReviewEntry> reviewEntries);
    }
}
