package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingUploadReminderServiceTest {
	@Test
	void headerWith499DataRowsDoesNotNotify() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-499-");
		try {
			Path csv = root.resolve("training.csv");
			writeCsv(csv, 499);
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			TrainingUploadReminderService.ReminderDecision decision = service.check(1_000L);
			assertFalse(decision.shouldNotify());
			assertEquals(499, decision.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void headerWith500DataRowsNotifiesImmediately() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-500-");
		try {
			Path csv = root.resolve("training.csv");
			writeCsv(csv, 500);
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			TrainingUploadReminderService.ReminderDecision decision = service.check(5_000L);
			assertTrue(decision.shouldNotify());
			assertEquals(500, decision.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void repeatedCheckBeforeFiveMinutesDoesNotNotifyAgain() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-before-interval-");
		try {
			Path csv = root.resolve("training.csv");
			writeCsv(csv, 500);
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			assertTrue(service.check(10_000L).shouldNotify());
			TrainingUploadReminderService.ReminderDecision second = service.check(10_000L + 299_999L);
			assertFalse(second.shouldNotify());
			assertEquals(500, second.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void checkAfterFiveMinutesNotifiesAgainWhenStillLarge() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-after-interval-");
		try {
			Path csv = root.resolve("training.csv");
			writeCsv(csv, 500);
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			assertTrue(service.check(10_000L).shouldNotify());
			TrainingUploadReminderService.ReminderDecision second = service.check(10_000L + 300_000L);
			assertTrue(second.shouldNotify());
			assertEquals(500, second.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void droppingBelowThresholdResetsAndReachingThresholdNotifiesImmediatelyAgain() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-reset-");
		try {
			Path csv = root.resolve("training.csv");
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			writeCsv(csv, 500);
			assertTrue(service.check(1_000L).shouldNotify());

			writeCsv(csv, 499);
			TrainingUploadReminderService.ReminderDecision below = service.check(2_000L);
			assertFalse(below.shouldNotify());
			assertEquals(499, below.entryCount());

			writeCsv(csv, 500);
			TrainingUploadReminderService.ReminderDecision backAbove = service.check(2_001L);
			assertTrue(backAbove.shouldNotify());
			assertEquals(500, backAbove.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void missingFileDoesNotNotifyAndReturnsZeroEntries() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-reminder-missing-");
		try {
			Path csv = root.resolve("training.csv");
			TrainingUploadReminderService service = new TrainingUploadReminderService(() -> csv);

			TrainingUploadReminderService.ReminderDecision decision = service.check(1_000L);
			assertFalse(decision.shouldNotify());
			assertEquals(0, decision.entryCount());
		} finally {
			deleteTree(root);
		}
	}

	private static void writeCsv(Path path, int entries) throws IOException {
		StringBuilder out = new StringBuilder("message,label\n");
		for (int i = 0; i < entries; i++) {
			out.append("\"sample ").append(i).append("\",").append(i % 2).append("\n");
		}
		Files.createDirectories(path.getParent());
		Files.writeString(path, out.toString(), StandardCharsets.UTF_8);
	}

	private static void deleteTree(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (var stream = Files.walk(root)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
				}
			});
		}
	}
}
