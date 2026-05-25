package eu.tango.scamscreener.training;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.message.ClientMessages;
import eu.tango.scamscreener.message.MessageDispatcher;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.concurrent.TimeUnit;

/**
 * Reminds the player to upload reviewed training cases after the local queue grows large enough.
 */
public final class TrainingUploadReminder {
    private static final int MIN_CASE_COUNT = 6;
    private static final long REMINDER_INTERVAL_MS = TimeUnit.MINUTES.toMillis(30);

    private static boolean initialized;
    private static long lastReminderAtMs;

    private TrainingUploadReminder() {
    }

    /**
     * Registers the periodic reminder once.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(TrainingUploadReminder::onClientTick);
    }

    /**
     * Pushes the next reminder back to a full interval after a manual or command-triggered upload start.
     */
    public static synchronized void postpone() {
        lastReminderAtMs = System.currentTimeMillis();
    }

    /**
     * Clears the current reminder timer so the next eligible state can notify immediately.
     */
    public static synchronized void resetTimer() {
        lastReminderAtMs = 0L;
    }

    private static void onClientTick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }

        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        if (!runtime.config().review().isTrainingUploadReminderEnabled()) {
            return;
        }
        if (runtime.trainingHubUploadWorker().isRunning()) {
            return;
        }

        int exportableCaseCount = TrainingCaseExportService.countExportableReviewedCases(runtime.reviewStore().entries());
        if (exportableCaseCount < MIN_CASE_COUNT) {
            resetTimer();
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (TrainingUploadReminder.class) {
            if (lastReminderAtMs > 0L && now - lastReminderAtMs < REMINDER_INTERVAL_MS) {
                return;
            }
            lastReminderAtMs = now;
        }

        MessageDispatcher.reply(ClientMessages.trainingUploadReminder(exportableCaseCount));
    }
}
