package eu.tango.scamscreener.training;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.review.ReviewEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs Training Hub exports and uploads on one background worker with retry support.
 */
public final class TrainingHubUploadWorker {
    private final TrainingCaseExportService exportService;
    private final Supplier<List<ReviewEntry>> reviewEntriesSupplier;
    private final Supplier<ScamScreenerClientSession> sessionSupplier;
    private final Runnable clearSessionAction;
    private final Supplier<RuntimeConfig> configSupplier;
    private final ScheduledExecutorService executor;
    private volatile UploadJob activeJob;

    public TrainingHubUploadWorker(
        TrainingCaseExportService exportService,
        Supplier<List<ReviewEntry>> reviewEntriesSupplier,
        Supplier<ScamScreenerClientSession> sessionSupplier,
        Runnable clearSessionAction,
        Supplier<RuntimeConfig> configSupplier
    ) {
        this.exportService = exportService;
        this.reviewEntriesSupplier = reviewEntriesSupplier;
        this.sessionSupplier = sessionSupplier;
        this.clearSessionAction = clearSessionAction;
        this.configSupplier = configSupplier;
        this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread workerThread = new Thread(task, "ScamScreener-TrainingUpload");
            workerThread.setDaemon(true);
            return workerThread;
        });
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
            TrainingCaseExportService.TrainingCaseExportResult exportResult = exportService.exportReviewedCases(reviewEntriesSupplier.get());
            if (exportResult == null || exportResult.exportedCaseCount() <= 0 || exportResult.trainingCasesFile() == null) {
                abort(job, "No reviewed SAFE/RISK cases are available for upload.", false);
                return;
            }

            MessageDispatcher.reply(ClientMessages.trainingUploadStarted(exportResult.exportedCaseCount()));
            attemptUpload(job, exportResult.trainingCasesFile(), 1);
        } catch (RuntimeException exception) {
            ScamScreenerMod.LOGGER.warn("Training upload export failed.", exception);
            abort(job, rootCauseMessage(exception), false);
        }
    }

    private void attemptUpload(UploadJob job, Path trainingCasesFile, int attemptNumber) {
        if (!isActive(job)) {
            return;
        }

        ScamScreenerClientSession session = sessionSupplier.get();
        if (session == null) {
            abort(job, "Session expired. Please log in again.", true);
            return;
        }

        try {
            ScamScreenerClientSession.UploadResult uploadResult = session.uploadTrainingDataAsync(trainingCasesFile).join();
            MessageDispatcher.reply(ClientMessages.trainingUploadCompleted(uploadResult));
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

        if (rootCause instanceof ScamScreenerClientSession.SessionExpiredException) {
            abort(job, rootCauseMessage(rootCause), true);
            return;
        }
        if (rootCause instanceof ScamScreenerClientSession.UploadRejectedException) {
            abort(job, rootCauseMessage(rootCause), false);
            return;
        }

        int retriesRemaining = job.maxRetries() - (attemptNumber - 1);
        if (retriesRemaining <= 0) {
            abort(job, rootCauseMessage(rootCause), false);
            return;
        }

        MessageDispatcher.reply(ClientMessages.trainingUploadRetryScheduled(
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

    private void abort(UploadJob job, String message, boolean clearSession) {
        if (clearSession) {
            clearSessionAction.run();
        }

        MessageDispatcher.reply(ClientMessages.trainingUploadAborted(message));
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

    private record UploadJob(
        int maxRetries,
        int retryDelaySeconds
    ) {
        private int totalAttempts() {
            return maxRetries + 1;
        }
    }
}
