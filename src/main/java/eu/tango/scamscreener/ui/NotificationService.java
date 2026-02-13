package eu.tango.scamscreener.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NotificationService {
	private static final ScheduledExecutorService WARNING_SOUND_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "scamscreener-warning-sound");
		thread.setDaemon(true);
		return thread;
	});

	private NotificationService() {
	}

	public static void playWarningTone() {
		Minecraft client = Minecraft.getInstance();
		for (int i = 0; i < 3; i++) {
			long delayMs = i * 120L;
			WARNING_SOUND_EXECUTOR.schedule(() -> client.execute(() -> {
				if (client.player != null) {
					//? if <1.21.11 {
					client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.2F);
					//?} else {
					/*client.player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.8F, 1.2F);
					*///?}
				}
			}), delayMs, TimeUnit.MILLISECONDS);
		}
	}
}
