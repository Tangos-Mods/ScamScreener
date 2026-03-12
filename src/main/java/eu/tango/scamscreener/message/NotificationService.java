package eu.tango.scamscreener.message;

import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client-side notification helpers for ScamScreener warnings.
 */
public final class NotificationService {
    private static final int WARNING_TONE_REPETITIONS = 3;
    private static final long WARNING_TONE_DELAY_MS = 120L;

    private NotificationService() {
    }

    /**
     * Plays the same short warning tone pattern used by the legacy output flow.
     */
    public static void playWarningTone() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        for (int index = 0; index < WARNING_TONE_REPETITIONS; index++) {
            long delayMs = index * WARNING_TONE_DELAY_MS;
            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() ->
                client.execute(() -> {
                    try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("notification.warning_tone", "  Warning Tone")) {
                        if (client.player == null) {
                            return;
                        }

                        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8F, 1.2F);
                    }
                })
            );
        }
    }
}
