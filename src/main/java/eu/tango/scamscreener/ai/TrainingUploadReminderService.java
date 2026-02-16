package eu.tango.scamscreener.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public final class TrainingUploadReminderService {
	public static final int ENTRY_THRESHOLD = 500;
	public static final long REMINDER_INTERVAL_MS = 300_000L;

	private final Supplier<Path> trainingDataPathSupplier;
	private long nextReminderAtMillis = -1L;

	public TrainingUploadReminderService(Supplier<Path> trainingDataPathSupplier) {
		this.trainingDataPathSupplier = trainingDataPathSupplier;
	}

	public ReminderDecision check(long nowMillis) {
		int entryCount = countEntries();
		if (entryCount < ENTRY_THRESHOLD) {
			nextReminderAtMillis = -1L;
			return new ReminderDecision(false, entryCount);
		}
		if (nextReminderAtMillis < 0L || nowMillis >= nextReminderAtMillis) {
			nextReminderAtMillis = nowMillis + REMINDER_INTERVAL_MS;
			return new ReminderDecision(true, entryCount);
		}
		return new ReminderDecision(false, entryCount);
	}

	public void reset() {
		nextReminderAtMillis = -1L;
	}

	private int countEntries() {
		Path path;
		try {
			path = trainingDataPathSupplier == null ? null : trainingDataPathSupplier.get();
		} catch (Exception ignored) {
			return 0;
		}
		if (path == null || !Files.exists(path)) {
			return 0;
		}

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			boolean skippedHeader = false;
			int entries = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (!skippedHeader) {
					skippedHeader = true;
					continue;
				}
				if (!line.trim().isEmpty()) {
					entries++;
				}
			}
			return entries;
		} catch (IOException ignored) {
			return 0;
		}
	}

	public record ReminderDecision(boolean shouldNotify, int entryCount) {
	}
}
