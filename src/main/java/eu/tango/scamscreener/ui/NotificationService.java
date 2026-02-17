package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.util.AsyncDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.TimeUnit;

public final class NotificationService {
	private NotificationService() {
	}

	public static void playWarningTone() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			long delayMs = i * 120L;
			AsyncDispatcher.schedule(() -> AsyncDispatcher.onClient(client, () -> {
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
